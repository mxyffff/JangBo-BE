package me.swudam.jangbo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.order.OrderResponseDto;
import me.swudam.jangbo.dto.cart.CartSelectionRequestDto;
import me.swudam.jangbo.entity.Customer;
import me.swudam.jangbo.entity.Order;
import me.swudam.jangbo.repository.CustomerRepository;
import me.swudam.jangbo.security.CustomerUserDetails;
import me.swudam.jangbo.service.CheckoutService;
import me.swudam.jangbo.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 고객 주문
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;
    private final CheckoutService checkoutService;
    private final CustomerRepository customerRepository;

    // 1. 주문 생성 (장바구니 -> 주문 생성)
    // POST - /api/orders
    // body: { "selectedItemIds": [2, 3, 5] } => 없거나 빈 배열일 경우 "전체 항목"
    @PostMapping
    public ResponseEntity<List<OrderResponseDto>> createOrders(
            @RequestBody(required = false) @Valid CartSelectionRequestDto requestDto
    ) {
        Long customerId = getCurrentCustomerIdOrThrow();
        List<Long> selected = (requestDto == null) ? null : requestDto.getSelectedItemIds();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkoutService.checkoutFromCart(customerId,selected));
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

    // 5. 주문 픽업 완료 처리 (고객 전용)
    // PATCH - /api/orders/{orderId}/pickup
    @PatchMapping("/{orderId}/pickup")
    public ResponseEntity<Map<String, Object>> completePickup(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomerUserDetails user
    ) { try {
            // 로그인한 고객 ID 확인
            Long customerId = user.getId();

            // 주문 존재 여부 + 고객 소유 여부 체크
            Order order = orderService.getOrderByIdEntity(orderId);
            // 본인 주문인지 확인
            if (!order.getCustomer().getId().equals(customerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "updated", false,
                        "message", "본인 주문만 픽업 완료할 수 있습니다."
                ));
            }
            // 픽업 상태 확인 후 완료 처리
            orderService.completePickup(orderId);

            return ResponseEntity.ok(Map.of(
                    "updated", true,
                    "message", "픽업이 완료되었습니다."
            ));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "updated", false,
                    "message", ex.getMessage()
            ));
        }
    }

    // 내부 유틸 메서드
    private Long getCurrentCustomerIdOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || isAnonymous(auth)) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }
        String email = (auth.getPrincipal() instanceof UserDetails ud) ? ud.getUsername() : String.valueOf(auth.getPrincipal());
        Customer customer = customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("고객 정보를 찾을 수 없습니다."));
        return customer.getId();
    }

    private boolean isAnonymous(Authentication auth) {
        String name = auth.getName();
        return name == null || "anonymousUser".equals(name);
    }
}