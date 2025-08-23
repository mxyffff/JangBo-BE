package me.swudam.jangbo.dto.ai;

import lombok.Builder;
import lombok.Value;

// “특정 식재료”와 “선정된 대표 상품” 1:1 매핑
@Value
@Builder
public class IngredientPickDto {
    String ingredient; // 예: "계란"
    ProductSummaryDto product; // 예: 특정 상점의 '1등급 계란 10구'
}
