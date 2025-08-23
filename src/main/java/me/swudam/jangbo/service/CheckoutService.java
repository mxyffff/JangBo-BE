package me.swudam.jangbo.service;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.order.OrderRequestDto;
import me.swudam.jangbo.dto.order.OrderResponseDto;
import me.swudam.jangbo.dto.cart.RemoveItemsRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

// 주문 생성 + 장바구니에서 해당 항목 제거
@Service
@RequiredArgsConstructor
public class CheckoutService {
    private final CartService cartService;
    private final OrderService orderService;

    // 장바구니 선택 항목(선택 없을 시 전체)을 주문으로 전환하고 성공 시 그 항목들을 장바구니에서 제거함
    @Transactional
    public List<OrderResponseDto> checkoutFromCart(Long customerId, Collection<Long> selectedItemIds) {
        // 1. 장바구니 -> OrderRequestDto 반환
        OrderRequestDto orderRequestDto = cartService.buildOrderRequestFromSelection(customerId, selectedItemIds);

        // 2. 같은 선택 기준 수수료 산 (CartService 로직 재사용)
        int pickupFee = cartService.getSelectionSummary(customerId, selectedItemIds).getPickupFee();

        // 3. 주문 생성 (상점 별로 분할 생성, 수수료는 한 번만 적용)
        List<OrderResponseDto> orders = orderService.createOrders(customerId, orderRequestDto, pickupFee);

        // 4. 성공 후 장바구니에서 해당 항목 제거(선택 없으면 전체 비우기)
        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            cartService.clearCart(customerId);
        } else {
            RemoveItemsRequestDto rm = new RemoveItemsRequestDto();
            rm.setItemIds(selectedItemIds);
            cartService.removeSelected(customerId, rm);
        }
        return orders;
    }
}