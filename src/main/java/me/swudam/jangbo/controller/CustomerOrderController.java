package me.swudam.jangbo.controller;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.OrderRequestDto;
import me.swudam.jangbo.dto.OrderResponseDto;
import me.swudam.jangbo.service.CustomerOrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class CustomerOrderController{

    private final CustomerOrderService customerOrderService;

    // 주문 생성
    @PostMapping
    public OrderResponseDto createOrder(@RequestBody OrderRequestDto dto,
                                        @AuthenticationPrincipal Long customerId) {
        return customerOrderService.createOrder(customerId, dto);
    }

    // 고객 주문 취소
    @PatchMapping("/{orderId}/cancel")
    public void cancelOrder(@PathVariable Long orderId) {
        customerOrderService.cancelOrder(orderId);
    }

    // 주문 목록 조회
    @GetMapping
    public List<OrderResponseDto> getOrders(@AuthenticationPrincipal Long customerId) {
        return customerOrderService.getOrdersByCustomer(customerId);
    }

    // 주문 상세 조회
    @GetMapping("/{orderId}")
    public OrderResponseDto getOrder(@PathVariable Long orderId) {
        return customerOrderService.getOrderById(orderId);
    }
}
