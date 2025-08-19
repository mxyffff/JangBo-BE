package me.swudam.jangbo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.cart.*;
import me.swudam.jangbo.entity.Customer;
import me.swudam.jangbo.repository.CustomerRepository;
import me.swudam.jangbo.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CustomerRepository customerRepository;

    /* 조회 */
    // 장바구니 전체 조회
    // 프론트 기본 장바구니 화면 진입 시 사용
    @GetMapping
    public ResponseEntity<CartSummaryResponseDto> getCart() {
        Long customerId = getCurrentCustomerIDorThrow();
        return ResponseEntity.ok(cartService.getCartDetail(customerId));
    }

    // 선택 항목 요약(합계/수수료/총액)
    @PostMapping("/selection/summary")
    public ResponseEntity<CartSummaryResponseDto> getSelectionSummary(
            @RequestBody @Valid CartSelectionRequestDto requestDto
    ) {
        Long customerId = getCurrentCustomerIDorThrow();
        return ResponseEntity.ok(
                cartService.getSelectionSummary(customerId, requestDto.getSelectedItemIds())
        );
    }

    /* 변경 (담기/수정/삭제) */
    // 장바구니 담기
    // - body: { "productId": 10, "quantity": 2 }
    // - response: { cartId, itemId, quantity, message }
    @PostMapping("/items")
    public ResponseEntity<AddToCartResponseDto> addToCart(
            @RequestBody @Valid AddToCartRequestDto requestDto
    ) {
        Long customerId = getCurrentCustomerIDorThrow();
        return ResponseEntity.ok(cartService.addToCart(customerId, requestDto));
    }

    // 장바구니 수량 지정 변경 (증감이 아닌 절대값으로 변경)
    // - path: /items/{itemId}
    // - body: { "itemId": <같은 값>, "quantity": 3 } -> 프론트 단에서 path와 body의 itemId 일치 유지 필요
    @PostMapping("/items/{itemId}")
    public ResponseEntity<UpdateQuantityResponseDto> updateQuantity(
            @PathVariable("itemId") Long itemId,
            @RequestBody @Valid UpdateQuantityRequestDto requestDto
    ) {
        // path의 itemId와 body의 itemId 불일치 방지
        if (requestDto.getItemId() == null || !Objects.equals(requestDto.getItemId(), itemId)) {
            throw new IllegalArgumentException("요청 경로의 itemId와 본문 itemId가 일치해야 합니다.");
        }
        Long customerId = getCurrentCustomerIDorThrow();
        return ResponseEntity.ok(cartService.updateQuantity(customerId, requestDto));
    }

    // 장바구니 수량 증감 변경
    // +1
    @PatchMapping("/items/{itemId}/increase")
    public ResponseEntity<?> inc(@PathVariable Long itemId) {
        Long customerId = getCurrentCustomerIDorThrow(); // 인증에서 구해오기
        return ResponseEntity.ok(cartService.changeQuantityByDelta(customerId, itemId, +1));
    }

    // -1
    @PatchMapping("/items/{itemId}/decrease")
    public ResponseEntity<?> dec(@PathVariable Long itemId) {
        Long customerId = getCurrentCustomerIDorThrow();
        return ResponseEntity.ok(cartService.changeQuantityByDelta(customerId, itemId, -1));
    }

    // 장바구니 개별 항목 삭제
    // - path: /items/{itemId}
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<DeleteItemsResponseDto> removeOne(
            @PathVariable("itemId") Long itemId
    ) {
        Long customerId = getCurrentCustomerIDorThrow();
        return ResponseEntity.ok(cartService.removeOne(customerId, itemId));
    }

    // 장바구니 선택 항목 일괄 삭제
    // - body: { "itemIds": [1,2,3] }
    @DeleteMapping("/items")
    public ResponseEntity<DeleteItemsResponseDto> removeSelected(
            @RequestBody @Valid RemoveItemsRequestDto request
    ) {
        Long customerId = getCurrentCustomerIDorThrow();
        return ResponseEntity.ok(cartService.removeSelected(customerId, request));
    }

    // 장바구니 비우기 (전체 삭제)
    @DeleteMapping
    public ResponseEntity<DeleteItemsResponseDto> clearCart() {
        Long customerId = getCurrentCustomerIDorThrow();
        return ResponseEntity.ok(cartService.clearCart(customerId));
    }

    /* 내부 유틸 메서드 */
    // 현재 로그인 고객 ID 획득
    // SecurityContext → email(username) → DB 조회 → customerId 반환
    // - 로그인 안 되어 있으면 401 유도
    // - email 미일치/탈퇴 등으로 고객 없으면 404 유도(IllegalArgumentException → GlobalHandler 변환)
    private Long getCurrentCustomerIDorThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || isAnonymous(auth)) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }

        String email;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            email = ud.getUsername();
        } else if (principal instanceof String s) {
            email = s;
        } else {
            throw new AuthenticationCredentialsNotFoundException("인증 정보를 확인할 수 없습니다.");
        }

        Customer customer = customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("고객 정보를 찾을 수 없습니다."));
        return customer.getId();
    }

    private boolean isAnonymous(Authentication auth) {
        String name = auth.getName();
        return name == null || "anonymousUser".equals(name);
    }
}
