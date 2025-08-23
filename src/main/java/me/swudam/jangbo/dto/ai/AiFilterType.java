package me.swudam.jangbo.dto.ai;

// 2차 처리에 사용하는 고정 필터 3종
public enum AiFilterType {
    CHEAPEST, // 가장 저렴한 식재료
    LONGEST_EXPIRY, // 유통기한이 가장 많이 남은 식재료
    MAX_COVERAGE_ONE_STORE // 한 상점에서 최대한 많이
}
