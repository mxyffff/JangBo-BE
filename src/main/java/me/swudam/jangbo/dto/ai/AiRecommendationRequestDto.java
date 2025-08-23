package me.swudam.jangbo.dto.ai;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 클라이언트가 보내는 2차 처리 요청
// - ingredients: 1차 처리에서 추출된 식재료 문자열 배열
// - filter: 고정 필터 1개 (CHEAPEST, LONGEST_EXPIRY, MAX_COVERAGE_ONE_STORE)
@Getter @Setter
public class AiRecommendationRequestDto {

    @NotEmpty(message = "ingredients는 비어 있을 수 없습니다.")
    private List<String> ingredients;

    @NotNull(message = "filter는 필수입니다.")
    private AiFilterType filter;
}
