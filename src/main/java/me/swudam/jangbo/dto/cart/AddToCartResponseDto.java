package me.swudam.jangbo.dto.cart;

import lombok.Builder;
import lombok.Getter;

// [응답 DTO] 담기 작업 결과를 간단히 알릴 때 사용
@Getter @Builder
public class AddToCartResponseDto {
    private final Long cartId;
    private final Long itemId;
    private final int quantity;   // 최종 수량(기존 + 추가 결과)
    private final String message; // 예: "장바구니에 담았습니다."
}
