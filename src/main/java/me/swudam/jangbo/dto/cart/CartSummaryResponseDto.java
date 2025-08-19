package me.swudam.jangbo.dto.cart;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// [응답 DTO] 장바구니 전체(혹은 선택된 항목) 요약
@Getter @Builder
public class CartSummaryResponseDto {
    private final List<CartItemResponseDto> items;

    private final int selectedItemCount; // 선택 항목 수
    private final int selectedStoreCount; // 선택 항목에 포함된 서로 다른 상점 수

    private final int subtotal; // 상품 합계
    private final int pickupFee; // 픽업 수수료
    private final int total; // 결제 총액
}
