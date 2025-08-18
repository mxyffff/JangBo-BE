package me.swudam.jangbo.dto;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.service.MerchantService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchants/orders")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final MerchantService merchantService; // 상인 관련 서비스
    private final MerchantOrderService merchantOrderService;       // 주문 관련 서비스

    /* ---------------- 상인 인증 관련 ---------------- */

    // 헬퍼: 이메일 가져오기
    private String getAuthenticatedMerchantEmail(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = null;
        if (auth != null && auth.isAuthenticated() && !isAnonymous(auth)) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails ud) email = ud.getUsername();
            else if (principal instanceof String s) email = s;
        }
        if (email == null) email = (String) session.getAttribute("merchantEmail");
        if (email == null) {
            Boolean justRegistered = (Boolean) session.getAttribute("justRegisteredMerchant");
            if (Boolean.TRUE.equals(justRegistered)) {
                email = (String) session.getAttribute("justRegisteredMerchantEmail");
            }
        }
        return email;
    }

    private boolean isAnonymous(Authentication auth) {
        String name = auth.getName();
        return name == null || "anonymousUser".equals(name);
    }

    private Long getMerchantId(HttpSession session) {
        String email = getAuthenticatedMerchantEmail(session);
        if (email == null) throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        return merchantOrderService.getMerchantIdByEmail(email);
    }

    /* ---------------- 주문 관련 API ---------------- */

    // 1. 상인 기준 주문 목록 조회
    @GetMapping
    public List<OrderResponseDto> getOrders(HttpSession session) {
        Long merchantId = getMerchantId(session);
        return merchantOrderService.getOrdersByMerchant(merchantId);
    }

    // 2. 주문 수락 + 준비시간 설정
    @PatchMapping("/{orderId}/accept")
    public void acceptOrder(@PathVariable Long orderId,
                            @RequestParam Integer preparationTime,
                            HttpSession session) {
        Long merchantId = getMerchantId(session);
        merchantOrderService.acceptOrder(merchantId, orderId, preparationTime);
    }

    // 3. 준비 완료
    @PatchMapping("/{orderId}/ready")
    public void markReady(@PathVariable Long orderId, HttpSession session) {
        Long merchantId = getMerchantId(session);
        merchantOrderService.markOrderReady(merchantId, orderId);
    }

    // 4. 주문 취소 + 사유 입력
    @PatchMapping("/{orderId}/cancel")
    public void cancelOrder(@PathVariable Long orderId,
                            @RequestParam String reason,
                            HttpSession session) {
        Long merchantId = getMerchantId(session);
        merchantOrderService.cancelOrderByMerchant(merchantId, orderId, reason);
    }
}
