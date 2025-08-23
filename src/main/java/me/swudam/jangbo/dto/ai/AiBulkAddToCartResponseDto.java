package me.swudam.jangbo.dto.ai;

import lombok.Builder;
import lombok.Value;
import me.swudam.jangbo.dto.cart.CartSummaryResponseDto;

import java.util.List;

// 일괄 담기 응답 DTO
// - addedCount: 처리된 항목 수
// - added: 방금 처리된 각 항목의 요약 (productId, cart itemId, 최종 quantity)
// - cartSummary: 담기 이후 장바구니 요약
// - message: 프론트 표시용
@Value
@Builder
public class AiBulkAddToCartResponseDto {

    int addedCount;

    List<AddedItem> added;

    CartSummaryResponseDto cartSummaryResponseDto;

    String message;

    @Value
    @Builder
    public static class AddedItem {
        Long productId;  // 담은 상품 id
        Long itemId;     // 생성/증가된 CartItem id
        int quantity;    // 해당 CartItem의 "최종" 수량
    }
}
