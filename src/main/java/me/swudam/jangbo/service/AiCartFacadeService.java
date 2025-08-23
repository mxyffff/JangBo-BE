package me.swudam.jangbo.service;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.ai.AiBulkAddToCartRequestDto;
import me.swudam.jangbo.dto.ai.AiBulkAddToCartResponseDto;
import me.swudam.jangbo.dto.cart.AddToCartRequestDto;
import me.swudam.jangbo.dto.cart.CartSummaryResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiCartFacadeService {

    private final CartService cartService;

    @Transactional
    public AiBulkAddToCartResponseDto addAll(Long customerId, AiBulkAddToCartRequestDto requestDto) {
        // 1. 요청 방어
        if (requestDto == null || requestDto.getItems() == null || requestDto.getItems().isEmpty()) {
            throw new  IllegalArgumentException("담을 상품 목록이 없습니다.");
        }

        // 2. 항목별로 장바구니 담기 (이미 담긴 상품이면 CartService 내부 규칙에 따라 수량 증가)
        List<AiBulkAddToCartResponseDto.AddedItem> added = new ArrayList<>();

        for (AiBulkAddToCartRequestDto.Item it : requestDto.getItems()) {
            // 수량 방어적 보정 (null 또는 1 미만 -> 1)
            int qty = (it.getQuantity() == null || it.getQuantity() < 1) ? 1 : it.getQuantity();

            // CartService 표준 DTO 사용
            AddToCartRequestDto add = new AddToCartRequestDto();
            add.setProductId(it.getProductId());
            add.setQuantity(qty);

            var res = cartService.addToCart(customerId, add);

            // 응답 축약(해당 CartItem의 최종 수량을 반환)
            added.add(AiBulkAddToCartResponseDto.AddedItem.builder()
                    .productId(it.getProductId())
                    .itemId(res.getItemId())
                    .quantity(res.getQuantity())
                    .build());
        }

        // 3. 최신 장바구니 요약
        CartSummaryResponseDto summaryResponseDto = cartService.getCartDetail(customerId);

        // 4) 최종 응답
        return AiBulkAddToCartResponseDto.builder()
                .addedCount(added.size())
                .added(added)
                .cartSummaryResponseDto(summaryResponseDto)
                .message("추천 목록을 장바구니에 담았습니다.")
                .build();
    }
}
