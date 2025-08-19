package me.swudam.jangbo.controller;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.OrderRequestDto;
import me.swudam.jangbo.dto.OrderResponseDto;
import me.swudam.jangbo.security.CustomerUserDetails;
import me.swudam.jangbo.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 고객 주문
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class CustomerOrderController{

    private final OrderService orderService;

    // 1. 주문 생성
    // POST - /api/orders
    @PostMapping
    public OrderResponseDto createOrder(@RequestBody OrderRequestDto dto,
                                        @AuthenticationPrincipal CustomerUserDetails user) {
        Long customerId = user.getId();
        return orderService.createOrder(customerId, dto);
    }

    // 2. 고객 주문 취소
    // PATCH - /api/orders/{orderId}/cancel
    @PatchMapping("/{orderId}/cancel")
    public void cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
    }

    // 3. 주문 목록 조회
    // GET - /api/orders
    @GetMapping
    public List<OrderResponseDto> getOrders(@AuthenticationPrincipal CustomerUserDetails user) {
        Long customerId = user.getId();
        return orderService.getOrdersByCustomer(customerId);
    }

    // 4. 주문 상세 조회
    // GET - /api/orders/{orderId}
    @GetMapping("/{orderId}")
    public OrderResponseDto getOrder(@PathVariable Long orderId) {
        return orderService.getOrderById(orderId);
    }
}
