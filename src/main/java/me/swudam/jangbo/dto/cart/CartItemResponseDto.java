package me.swudam.jangbo.dto.cart;

import lombok.Builder;
import lombok.Getter;

// [요청 DTO] 장바구니 내 개별 항목 한 줄
@Getter @Builder
public class CartItemResponseDto {

    private final Long itemId;

    private final Long productId;
    private final String productName;

    private final Long storeId;
    private final String storeName;

    private final int unitPrice; // 개당 현재가 (원)
    private final int quantity; // 담긴 수량
    private final int lineTotal; // unitPrice * quantity (원)
    private final String imageUrl;
}
