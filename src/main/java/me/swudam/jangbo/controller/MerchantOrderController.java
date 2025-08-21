package me.swudam.jangbo.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.OrderResponseDto;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.service.MerchantService;
import me.swudam.jangbo.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchants/orders")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final MerchantService merchantService; // 상인 관련 서비스
    private final OrderService orderService; // 주문 관련 서비스

    /* 상인 인증 API */
    // Helper: 세션/인증 정보에서 이메일 가져오기
    private String getAuthenticatedMerchantEmail(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication(); // Spring Security 인증 정보
        String email = null;
        if (auth != null && auth.isAuthenticated() && !isAnonymous(auth)) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails ud) email = ud.getUsername(); // UserDetails에서 이메일
            else if (principal instanceof String s) email = s; // String으로 저장된 이메일
        }
        if (email == null) email = (String) session.getAttribute("merchantEmail"); // 세션에서 이메일 확인
        if (email == null) {
            Boolean justRegistered = (Boolean) session.getAttribute("justRegisteredMerchant"); // 가입 직후 처리
            if (Boolean.TRUE.equals(justRegistered)) {
                email = (String) session.getAttribute("justRegisteredMerchantEmail"); // 방금 가입한 상인 이메일
            }
        }
        return email; // 이메일 반환
    }

    // 인증 정보가 익명인지
    private boolean isAnonymous(Authentication auth) {
        String name = auth.getName();
        return name == null || "anonymousUser".equals(name);
    }

    // 이메일로 상인 ID 조회
    private Long getMerchantId(HttpSession session) {
        String email = getAuthenticatedMerchantEmail(session);
        if (email == null) throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        Merchant merchant = merchantService.getMerchantByEmail(email);
        if (merchant == null) {
            throw new AuthenticationCredentialsNotFoundException("상인 정보를 찾을 수 없습니다.");
        }
        return merchant.getId();
    }

    /* 주문 관련 API */
    // 1. 상인 기준 주문 목록 조회
    // GET - /api/merchants/orders
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrders(HttpSession session) {
        Long merchantId = getMerchantId(session); // 상인 ID 가져오기
        return ResponseEntity.ok(orderService.getOrdersByMerchant(merchantId));
    }

    // 2. 주문 수락 + 준비시간 설정
    // PATCH /api/merchants/orders/{orderId}/accept?preparationTime=15
    @PatchMapping("/{orderId}/accept")
    public ResponseEntity<?> acceptOrder(@PathVariable Long orderId,
                                         @RequestParam Integer preparationTime,
                                         HttpSession session) {
        if (preparationTime == null || preparationTime <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "updated", false,
                    "message", "준비 시간은 1분 이상이어야 합니다."
            ));
        }
        Long merchantId = getMerchantId(session);
        try {
            orderService.acceptOrder(merchantId, orderId, preparationTime);
            return ResponseEntity.ok(Map.of(
                    "updated", true,
                    "message", "주문이 수락되었습니다. 준비 시간: " + preparationTime + "분"
            ));
        } catch (IllegalStateException | IllegalArgumentException ex) { // 예외 처리 BadRequest
            return ResponseEntity.badRequest().body(Map.of(
                    "updated", false,
                    "message", ex.getMessage()
            ));
        }
    }

    // 3. 준비 완료
    // PATCH /api/merchants/orders/{orderId}/ready
    @PatchMapping("/{orderId}/ready")
    public ResponseEntity<?> markReady(@PathVariable Long orderId, HttpSession session) {
        Long merchantId = getMerchantId(session);
        try {
            orderService.markOrderReady(merchantId, orderId);
            return ResponseEntity.ok(Map.of(
                    "updated", true,
                    "message", "주문 준비가 완료되었습니다."
            ));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "updated", false,
                    "message", ex.getMessage()
            ));
        }
    }

    // 4. 주문 취소 + 사유 입력
    // PATCH /api/merchants/orders/{orderId}/cancel?reason=사유입력
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId,
                                         @RequestParam String reason,
                                         HttpSession session) {
        Long merchantId = getMerchantId(session);
        try {
            orderService.cancelOrderByMerchant(merchantId, orderId, reason);
            return ResponseEntity.ok(Map.of(
                    "updated", true,
                    "message", "주문이 취소되었습니다. 사유: " + reason
            ));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "updated", false,
                    "message", ex.getMessage()
            ));
        }
    }
}