package me.swudam.jangbo.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// AI의 1차 답변 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiIngredientResponseDto {
    // 예: "간장계란밥을 만들 때 필요한 식재료로는 계란, 쌀, 간장, 참기름 등이 있어요."
    private String answer;

    // 예: [ "계란", "쌀", "간장" ] - 백엔드 2차 처리에서 검색 키워드로 사용
    private List<String> ingredients;
}
