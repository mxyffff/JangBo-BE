package me.swudam.jangbo.dto.ai;

import lombok.Builder;
import lombok.Value;

import java.util.List;

// 2차 처리 응답 전체
// - filter: 적용된 필터
// - picks: 식재료별로 선정된 대표 상품
@Value
@Builder
public class AiRecommendationResponseDto {
    AiFilterType filter;
    List<IngredientPickDto> picks;

    // MAX_COVERAGE_ONE_STORE 전용 부가 정보
    Long selectedStoreId;
    String selectedStoreName;
    Integer coveredCount; // 해당 상점에서 충족한 식재료 수
    Integer sumPrice; // 그 상점에서 담을 때 총 가격
}
