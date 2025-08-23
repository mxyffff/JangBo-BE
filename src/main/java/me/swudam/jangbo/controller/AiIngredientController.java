package me.swudam.jangbo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.ai.AiIngredientRequestDto;
import me.swudam.jangbo.dto.ai.AiIngredientResponseDto;
import me.swudam.jangbo.service.AiIngredientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 1차 처리(4.1~4.4): 자연어 질문 → "필요한 식재료만" 간결 답변 + JSON 배열 반환
@RestController
@RequestMapping("/api/ai/ingredients")
@RequiredArgsConstructor
public class AiIngredientController {

    private final AiIngredientService aiIngredientService;

    // 1차 처리 실행 API
    // HTTP: POST /api/ai/ingredients/analyze
    @PostMapping("/analyze")
    public ResponseEntity<AiIngredientResponseDto> analyze(
            @RequestBody @Valid AiIngredientRequestDto requestDto // JSON 본문을 DTO로 바인딩 + Bean Validation 수행
    ) {
        // 질문이 비어있으면 400 Bad Request 반환
        // - DTO에 @NotBlank 등을 이미 달았지만 사용자 경험을 위해 한 번 더 체크
        if (requestDto == null || !StringUtils.hasText(requestDto.getQuestion())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST) // 400 상태코드
                    .body(AiIngredientResponseDto.builder()
                            .answer("질문이 비어 있습니다. 예: \"간장계란밥을 만들건데 어떤걸 사야할까?\"")
                            .ingredients(List.of())
                            .build());
        }

        // 1차 처리 서비스 호출
        AiIngredientResponseDto result = aiIngredientService.analyze(requestDto);

        // 200 OK와 함께 결과 반환
        return ResponseEntity.ok(result);
    }
}
