package me.swudam.jangbo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.ai.AiIngredientRequestDto;
import me.swudam.jangbo.dto.ai.AiIngredientResponseDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;   // 예시(assistant) 메시지 타입
import org.springframework.ai.chat.messages.Message;            // 공통 메시지 인터페이스
import org.springframework.ai.chat.messages.SystemMessage;      // 시스템 지시문
import org.springframework.ai.chat.messages.UserMessage;        // 사용자 질문
import org.springframework.ai.chat.prompt.Prompt;               // 메시지 묶음
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 1차 입력값 처리
// 사용자 질문 → "필요한 식재료만" 간결 답변 + JSON 배열(ingredients) 반환
@Service
@RequiredArgsConstructor
public class AiIngredientService {

    private final ChatClient chatClient; // AiConfig에서 주입한 ChatClient (OpenAI 호출용)
    private final ObjectMapper om = new ObjectMapper(); // JSON 직렬화/역직렬화용

    public AiIngredientResponseDto analyze(AiIngredientRequestDto requestDto) {
        // 1. 사용자 질문 - (가독성용) 사용자 질문을 별도 변수에 보관
        final String userQuestion = requestDto.getQuestion();

        // 1) 간단 로컬 규칙으로 엉뚱한 질문 거르기
        if (!looksLikeRecipeQuestion(userQuestion)) {
            // 2) 여전히 애매하면 LLM으로 한 번 더 확인
            if (!confirmIngredientIntentWithLLM(userQuestion)) {
                // -> 식재료 질문 아니라면: 정중한 안내 + 빈 배열
                return AiIngredientResponseDto.builder()
                        .answer("이 기능은 요리에 필요한 식재료를 간단히 추천하는 용도예요. 예: \"간장계란밥을 만들 건데 어떤 걸 사야 할까요?\"처럼 물어봐 주세요.")
                        .ingredients(List.of())
                        .build();
            }
        }
        // 2. 본 처리 (필요 식재료 추출)
        // 시스템 규칙: 모델의 역할/어조/출력 형식 고정
        String system = """
                너는 '필요한 식재료 추출기'야.
                규칙:
                - 사용자가 요리/메뉴를 말하면, 그 요리를 만드는 데 '필수'인 식재료만 간결히 도출해(장식 X).
                - '양념/소스'도 필요하면 포함(예: 간장, 소금, 설탕, 참기름 등).
                - 설명은 길게 하지 말고, '정중하고 간결한' 한국어 한 문장인 'answer' 작성.
                - 오직 아래 JSON 스키마 형식으로만 출력:
                    {
                        "answer": "<한 문장 한국어 답변>",
                        "ingredients": ["식재료1","식재료2", ...]
                    }
                - JSON 외 다른 텍스트(코드블록, 주석)는 절대 출력하지 마.
                """;

        // 3. 모델에게 few-shot 예시(정답 JSON) 제공 -> 모델이 포맷을 잘 따르게 유도
        Map<String, Object> example1 = Map.of(
                "answer", "간장계란밥을 만들 때 필요한 식재료로는 계란, 쌀, 간장, 참기름 등이 있어요.",
                "ingredients", List.of("계란", "쌀", "간장", "참기름")
        );
        Map<String, Object> example2 = Map.of(
                "answer", "김치찌개에는 김치, 돼지고기(또는 참치), 두부, 대파, 다진마늘, 고춧가루, 간장, 설탕 등이 필요해요.",
                "ingredients", List.of("김치", "돼지고기", "두부", "대파", "다진마늘", "고춧가루", "간장", "설탕")
        );

        // 4. 메시지 시퀀스 (시스템 -> 예시(assistant) -> 사용자)
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(system)); // 규칙/포맷 고정
        messages.add(new AssistantMessage(writeJson(example1))); // 예시 1 (assistant 역할)
        messages.add(new AssistantMessage(writeJson(example2))); // 에시 2
        messages.add(new UserMessage(userQuestion)); // 실제 사용자 질문

        // 5. 모델 호출 (Prompt로 메시지 묶음 전달)
        String content = chatClient
                .prompt(new Prompt(messages)) // 메시지 묶음 전달
                .call() // 실제 호출 수행 (ResponseSpec 반환)
                .content(); // 모델의 응답 본문 문자열만 추출

        // 6. 응답(JSON) 파싱 -> DTO 반환
        try {
            // 혹시라도 ```json ... ```로 감싸져 오면 대비
            content = stripFences(content);

            JsonNode node = om.readTree(content);
            String answer = node.path("answer").asText();
            List<String> ingredients =
                    om.convertValue(node.path("ingredients"),
                            om.getTypeFactory().constructCollectionType(List.class, String.class));

            // 예외 케이스 방지: 모델이 실수로 빈 배열을 돌려준다면 안내 메시지로 폴백
            if (ingredients == null || ingredients.isEmpty()) {
                return AiIngredientResponseDto.builder()
                        .answer("요리에 필요한 식재료를 찾지 못했어요. 예: \"비빔국수 만들려면 뭐가 필요해?\"처럼 질문해 주세요.")
                        .ingredients(List.of())
                        .build();
            }

            return AiIngredientResponseDto.builder()
                    .answer(answer)
                    .ingredients(ingredients == null ? List.of() : ingredients)
                    .build();

        } catch (Exception e) {
            // 파싱 실패시 폴백
            return AiIngredientResponseDto.builder()
                    .answer("필요한 식재료를 추출하지 못했어요. \"김치볶음밥 만들려면?\"처럼 다시 질문해 주세요.")
                    .ingredients(List.of())
                    .build();
        }

    }

    /* 내부 유틸 메서드 */
    // 가드레일 메서드 1
    // '요리/레시피' 관련 단서가 있는지 대략 판별
    private boolean looksLikeRecipeQuestion(String q) {
        if (q == null) return false;
        String t = q.strip();
        if (t.length() < 2) return false;

        // 1. 먼저 부정/감상 위주 토큰이면서 '식재료 의도'가 전혀 없으면 차단
        String[] sentimentOnly = {"싫어", "싫어해", "좋아", "좋아해", "맛있", "별로", "최고"};
        boolean hasSentiment = containsAny(t, sentimentOnly);

        // 2. 조리/행동 토큰 (만들, 끓, 볶, 굽 등)
        String[] cookTokens = {
                "만들", "만들려면", "만들라면", "만드려면", "만들건데", "끓이", "볶", "찌", "굽", "튀기",
                "비비", "데치", "삶", "썰", "파스타", "스프", "국", "찌개", "탕", "덮밥"
        };
        boolean hasCook = containsAny(t, cookTokens);

        // 3) 식재료/구매 의도 토큰 (무엇이 필요/뭘 사야/재료 등)
        String[] needTokens = {
                "필요", "재료", "뭐가 필요", "무엇이 필요", "뭘 사", "사야", "사려면", "살려면", "살라면",
                "구매", "사오", "준비", "어떤 걸 사", "어떤 재료", "리스트", "목록"
        };
        boolean hasNeed = containsAny(t, needTokens);

        // 4) 의문/질문성 단서(권장) — “?” 또는 한국어 의문 패턴
        String[] questionHints = { "?", "어떻게", "뭐", "무엇", "어떤", "알려줘", "가르쳐", "해줘" };
        boolean looksQuestion = containsAny(t, questionHints);

        // 5) 결정 로직
        //   - 조리행동 + 필요/구매 의도가 모두 있고(핵심),
        //   - 단순 감상문만이 아니어야 함
        if (hasCook && hasNeed) return true;

        if (hasSentiment && !hasNeed) return false; // “싫어해/좋아해” 류는 차단

        // 질문형으로 보이지만 명시적 필요/구매 의도가 없다면 보수적으로 false
        return false;
    }

    private boolean containsAny(String text, String[] keywords) {
        String lower = text.toLowerCase();
        for (String k : keywords) {
            if (lower.contains(k.toLowerCase())) return true;
        }
        return false;
    }

    // 가드레일 메서드 2
    // - LLM 의도 확인 (출력: {"isIngredient": true/false})
    private boolean confirmIngredientIntentWithLLM(String q) {
        try {
            String system = """
                    너의 임무는 '사용자 문장이 요리에 필요한 식재료(또는 구매/준비 목록)를 묻는가'만 판별하는 것이다.
                    조건:
                    - 다음 두 조건이 모두 만족되어야 true:
                    (1) 요리/조리/메뉴와 관련된 요청이며,
                    (2) 필요한 재료, 무엇을 사야 하는지, 준비 목록 등 '구매/준비 의도'가 명확함.
                    - 위 조건 중 하나라도 불명확하면 false.
                    - 오직 JSON 한 줄로만 출력: {"isIngredient": true/false}
                    - 설명 문장, 코드펜스, 여분의 텍스트 금지.
                    예시:
                    [긍정] "비빔국수 만들려면 뭐 사야 해?" -> {"isIngredient": true}
                    [긍정] "오믈렛 재료 알려줘" -> {"isIngredient": true}
                    [부정] "난 된장찌개 싫어해" -> {"isIngredient": false}
                    [부정] "비빔밥이 맛있어" -> {"isIngredient": false}
                    [부정] "김치찌개가 뭐야?" -> {"isIngredient": false}
                    """;

            List<Message> messages = List.of(
                    new SystemMessage(system),
                    new UserMessage(q)
            );

            // 소형 모델 + 저온도 + 짧은 토큰
            var options = OpenAiChatOptions.builder()
                    .model("gpt-4o-mini") // 사용 중인 소형 채팅 모델
                    .temperature(0.0)
                    .maxTokens(12)
                    .build();

            String content = chatClient
                    .prompt()
                    .messages(messages)
                    .options(options)
                    .call()
                    .content();

            content = stripFences(content);
            return new ObjectMapper().readTree(content).path("isIngredient").asBoolean(false);
        } catch (Exception ignore) {
            // 분류 실패 시 보수적으로 false 처리
            return false;
        }
    }

    // Map -> Json 문자열 (few-shot 예시용)
    private String writeJson(Map<String, Object> map) {
        try {
            return om.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"answer\":\"예시\",\"ingredients\":[]}";
        }
    }

    // json 코드펜스가 있을 때 제거하는 보조 메서드
    private String stripFences(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.startsWith("```")) {
            // 첫줄의 ``` 제거
            int first = t.indexOf('\n');
            if (first > 0) t = t.substring(first + 1);
            // 끝쪽 ``` 제거
            int lastFence = t.lastIndexOf("```");
            if (lastFence >= 0) t = t.substring(0, lastFence);
            return t.trim();
        }
        return t;
    }
}
