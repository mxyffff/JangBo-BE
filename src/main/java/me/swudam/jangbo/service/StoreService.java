package me.swudam.jangbo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.StoreFormDto;
import me.swudam.jangbo.entity.DayOff;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.entity.Store;
import me.swudam.jangbo.repository.MerchantRepository;
import me.swudam.jangbo.repository.StoreRepository;
import me.swudam.jangbo.support.NotFoundException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final MerchantRepository merchantRepository;

    // AI 호출용 (Spring AI) — AiConfig 에서 주입됨
    private final ChatClient chatClient;

    private final ObjectMapper om = new ObjectMapper();


    @Value("${uploadPath}")
    private String uploadPath;

    /* 공통 검증/정리 */
    // 휴무 요일 제약
    public void validateDayOff(StoreFormDto storeFormDto) {
        if (storeFormDto.getDayOff().contains(DayOff.ALWAYS_OPEN) && storeFormDto.getDayOff().size() > 1) {
            throw new IllegalArgumentException("연중무휴는 다른 요일과 함께 선택할 수 없습니다.");
        }
    }

    // 한줄 소개 문자열 정리
    private String normalizeTagline(String s) {
        if (s == null) return ""; // DTO에서 null 금지지만 2중 방어
        String t = s.trim().replaceAll("\\s+", " ");
        if (t.length() > StoreFormDto.TAGLINE_MAX) {
            t = t.substring(0, StoreFormDto.TAGLINE_MAX);
        }
        return t;
    }

    /* C: 상점 저장 */
    // 이미지 포함하여 Store 저장 로직
    public Long saveStore(StoreFormDto storeFormDto, Merchant merchant, MultipartFile storeImage) {
        // 기본값/공백 정리
        storeFormDto.normalize();
        // 휴무 요일 규칙 검증
        validateDayOff(storeFormDto);

        // 상점 등록 시 Merchant 조회 + 예외
        Merchant managedMerchant = merchantRepository.findById(merchant.getId())
                .orElseThrow(() -> new NotFoundException("상인을 찾을 수 없습니다."));

        // 상점 1개 제한
        if (storeRepository.existsByMerchantId(managedMerchant.getId())) {
            throw new IllegalArgumentException("상점은 상인당 1개만 등록할 수 있습니다.");
        }

        // Store 생성
        Store store = Store.createStore(storeFormDto, managedMerchant);
        // 한 줄 소개 세팅 (null 금지, 길이 제한)
        store.setTagline(normalizeTagline(storeFormDto.getTagline()));


        // 이미지 저장
        if (storeImage != null && !storeImage.isEmpty()) {
            try {
                UUID uuid = UUID.randomUUID();
                String fileName = uuid + "-" + storeImage.getOriginalFilename();
                File storeImgFile = new File(uploadPath, fileName);
                storeImage.transferTo(storeImgFile);
                store.setStoreImg(fileName);
                store.setStoreImgPath(uploadPath + "/" + fileName);
            } catch (Exception e) {
                throw new RuntimeException("이미지 저장 중 오류가 발생했습니다.");
            }
        }

        storeRepository.save(store);
        return store.getId();
    }

    /* R: 전체 / 단건 조회는 기존 그대로 */
    // 전체 상점 조회
    @Transactional(readOnly = true)
    public List<StoreFormDto> getStore() {
        List<Store> stores = storeRepository.findAll();
        List<StoreFormDto> dtos = new ArrayList<>();
        stores.forEach(s -> dtos.add(StoreFormDto.of(s)));
        return dtos;
    }

    // ID로 특정 상점 찾기
    @Transactional(readOnly = true)
    public StoreFormDto getStoreById(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("상점을 찾을 수 없습니다."));
        return StoreFormDto.of(store);
    }

    /* U: 상점 수정 */
    // 상점 수정
    public void updateStore(Long storeId, StoreFormDto storeFormDto, MultipartFile storeImage) {
        // 기본 값/공백 정리
        storeFormDto.normalize();
        // 휴무 요일 규칙 검증
        validateDayOff(storeFormDto);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("상점을 찾을 수 없습니다."));

        if (storeFormDto.getStoreName() != null) store.setStoreName(storeFormDto.getStoreName());
        if (storeFormDto.getStoreAddress() != null) store.setStoreAddress(storeFormDto.getStoreAddress());
        if (storeFormDto.getOpenTime() != null) store.setOpenTime(storeFormDto.getOpenTime());
        if (storeFormDto.getCloseTime() != null) store.setCloseTime(storeFormDto.getCloseTime());
        if (storeFormDto.getDayOff() != null && !storeFormDto.getDayOff().isEmpty())
            store.setDayOff(new HashSet<>(storeFormDto.getDayOff()));
        if (storeFormDto.getStorePhoneNumber() != null) store.setStorePhoneNumber(storeFormDto.getStorePhoneNumber());
        if (storeFormDto.getCategory() != null) store.setCategory(storeFormDto.getCategory());

        // 한 줄 소개 수정 (DTO는 null 금지지만, PATCH에서는 값이 올 수 있으니 체크 후 반영)
        if (storeFormDto.getTagline() != null) {
            store.setTagline(normalizeTagline(storeFormDto.getTagline()));
        }

        // 이미지 수정
        if (storeImage != null && !storeImage.isEmpty()) {
            try {
                if (store.getStoreImgPath() != null) {
                    File old = new File(store.getStoreImgPath());
                    if (old.exists()) {
                        boolean deleted = old.delete();
                        if (!deleted) {
                            System.err.println("기존 이미지 삭제 실패: " + old.getAbsolutePath());
                        }
                    }
                }
                // 새 파일 저장
                UUID uuid = UUID.randomUUID();
                String fileName = uuid + "-" + storeImage.getOriginalFilename();
                File storeImgFile = new File(uploadPath, fileName);
                storeImage.transferTo(storeImgFile);
                store.setStoreImg(fileName);
                store.setStoreImgPath(uploadPath + "/" + fileName);
            } catch (Exception e) {
                throw new RuntimeException("이미지 저장 중 오류가 발생했습니다.");
            }
        }

        storeRepository.save(store);
    }

    /* D: 상점 삭제 */
    // 상점 삭제
    public void deleteStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("상점을 찾을 수 없습니다."));
        storeRepository.delete(store);
    }

    /* 상점 리스트 정렬 - 최신순, 인기순 */
    // 상점 최신순 정렬
    @Transactional(readOnly = true)
    public List<StoreFormDto> getStoreSortedByRecent(){
        // 1. Repository에서 createdAt 기준 내림차순으로 상점 조회
        List<Store> stores = storeRepository.findAllByOrderByCreatedAtDesc();

        // 2. 엔티티 → DTO 변환
        List<StoreFormDto> storeFormDtos = new ArrayList<>();
        stores.forEach(s -> {
            StoreFormDto dto = StoreFormDto.of(s);
            // Service에서 trim 적용
            if (dto.getStoreName() != null) dto.setStoreName(dto.getStoreName().trim());
            if (dto.getStoreAddress() != null) dto.setStoreAddress(dto.getStoreAddress().trim());
            if (dto.getStorePhoneNumber() != null) dto.setStorePhoneNumber(dto.getStorePhoneNumber().trim());
            storeFormDtos.add(dto);
        });

        // 3. DTO 리스트 반환
        return storeFormDtos;
    }

    /* AI: 한 줄 소개 추천 */
    // 입력: 상점명, 카테고리(선택), 키워드들(선택)
    // 출력: 길이 80자 이하의 한국어 문장 3개 내외
    // 응답을 JSON으로 강제하고, 파싱 실패 시 보수적으로 폴백
    @Transactional(readOnly = true)
    public List<String> suggestTaglines(String storeName, String categoryName, List<String> keywords) {
        // 1. 입력 정리
        String name = safe(storeName);
        String cat = safe(categoryName);
        List<String> kws = Optional.ofNullable(keywords).orElse(List.of())
                .stream().map(this::safe).filter(s -> !s.isEmpty()).toList();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("상점명이 비어있습니다.");
        }

        // 2. 프롬프트 구성
        String system = """
                 너는 상점의 '한 줄 소개'를 만드는 도우미야.
                                규칙:
                                - 한국어로 최대 80자 이내의 간결한 문장을 3개 추천해.
                                - 광고 문구처럼 과장되거나 과도한 이모지는 쓰지 마.
                                - 상점명과 카테고리, 키워드를 자연스럽게 녹여도 되지만, 너무 기계적으로 반복하지 마.
                                - 오직 아래 JSON 포맷으로만 출력해:
                                  {"candidates": ["문장1","문장2","문장3"]}
                                - JSON 외의 다른 텍스트(설명/코드블록)는 절대 출력하지 마.
                """;

        String user = "상점명: " + name +
                (cat.isEmpty() ? "" : ("\n카테고리: " + cat)) +
                (kws.isEmpty() ? "" : ("\n키워드: " + String.join(", ", kws)));

        // 3. 모델 호출
        String content = chatClient
                .prompt()
                .messages(new SystemMessage(system), new UserMessage(user))
                .options(OpenAiChatOptions.builder()
                        .model("gpt-4o-mini")
                        .temperature(0.5)
                        .maxTokens(200)
                        .build())
                .call()
                .content();

        // 4. JSON 파싱 + 후처리(중복 제거/길이 제한)
        try {
            content = stripFences(content);
            JsonNode root = om.readTree(content);
            var arr = root.path("candidates");

            List<String> out = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    String line = normalizeTagline(n.asText(""));
                    if (!line.isEmpty()) out.add(line);
                }
            }
            // 중복 제거 + 최대 3개
            return out.stream()
                    .distinct()
                    .limit(3)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // 파싱 실패 시: 간단한 폴백 문구 1~2개 리턴
            return List.of(
                    normalizeTagline(name + " - 신선함과 정성을 전합니다."),
                    normalizeTagline("매일 찾아오는 맛있는 " + ((cat.isEmpty()) ? "상품" : cat))
            );
        }
    }

    /* 내부 유틸 */
    private String safe(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ");
    }

    /** ```json … ``` 같은 코드펜스 제거 */
    private String stripFences(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.startsWith("```")) {
            int first = t.indexOf('\n');
            if (first > 0) t = t.substring(first + 1);
            int lastFence = t.lastIndexOf("```");
            if (lastFence >= 0) t = t.substring(0, lastFence);
            return t.trim();
        }
        return t;
    }
}