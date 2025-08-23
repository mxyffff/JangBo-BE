package me.swudam.jangbo.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.PaymentResponseDto;
import me.swudam.jangbo.service.MerchantService;
import me.swudam.jangbo.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/merchants/payments")
@RequiredArgsConstructor
public class MerchantPaymentController {

    private final PaymentService paymentService;
    private final MerchantService merchantService;

    // 세션에서 상인 ID 추출
    private Long getMerchantIdFromSession(HttpSession session) {
        String email = (String) session.getAttribute("merchantEmail");
        if (email == null) throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        return merchantService.getMerchantByEmail(email).getId();
    }

    // 특정 주문 건 결제 내역 조회
    // GET - /api/merchants/payments/{orderId}
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getPaymentForMerchant(@PathVariable Long orderId, HttpSession session) {
        Long merchantId = getMerchantIdFromSession(session);
        try {
            PaymentResponseDto response = paymentService.getPaymentForMerchant(orderId, merchantId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "payment", response,
                    "message", "결제 내역 조회 성공"
            ));
        } catch (SecurityException ex) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "해당 주문에 접근할 권한이 없습니다."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 상인은 결제 승인/취소 권한 없음
    // 따라서, approve/decline/cancel 관련 엔드포인트는 제공하지 않음
    // 결제 상태 변경은 오직 CustomerPaymentController에서 처리
}
