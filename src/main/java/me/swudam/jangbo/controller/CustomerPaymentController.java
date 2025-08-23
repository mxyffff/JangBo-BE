package me.swudam.jangbo.controller;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.CheckoutResponseDto;
import me.swudam.jangbo.dto.PaymentResponseDto;
import me.swudam.jangbo.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class CustomerPaymentController {

    private final PaymentService paymentService;

    // 1. 결제 요청 생성 (신규)
    // POST - /api/payments/{orderId}/request
    @PostMapping("/{orderId}/request")
    public ResponseEntity<?> requestPayment(@PathVariable Long orderId) {
        try {
            PaymentResponseDto response = paymentService.requestPayment(orderId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "결제 요청이 생성되었습니다.",
                    "payment", response
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (IllegalStateException e) {  // Redis 락 중복 요청 예외
            return ResponseEntity.status(409).body(Map.of(  // 409 Conflict
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 2. 결제 정보 조회
    // GET - /api/payments/{orderId}
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getPaymentInfo(@PathVariable Long orderId) {
        try {
            PaymentResponseDto response = paymentService.getPaymentInfo(orderId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "found", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 3. 결제 승인
    // POST - /api/payments/{orderId}/approve
    @PostMapping("/{orderId}/approve")
    public ResponseEntity<?> approvePayment(@PathVariable Long orderId) {
        try {
            PaymentResponseDto response = paymentService.approvePayment(orderId);
            return ResponseEntity.ok(Map.of(
                    "updated", true,
                    "message", "결제가 승인됐습니다.",
                    "payment", response
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "updated", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 4. 결제 거부
    // POST - /api/payments/{orderId}/decline
    @PostMapping("/{orderId}/decline")
    public ResponseEntity<?> declinePayment(@PathVariable Long orderId) {
        try {
            PaymentResponseDto response = paymentService.declinePayment(orderId);
            return ResponseEntity.ok(Map.of(
                    "updated", true,
                    "message", "결제가 거부되었습니다.",
                    "payment", response
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "updated", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 5. 결제 취소
    // POST - /api/payments/{orderId}/cancel
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelPayment(@PathVariable Long orderId) {
        try {
            PaymentResponseDto response = paymentService.cancelPayment(orderId);
            return ResponseEntity.ok(Map.of(
                    "updated", true,
                    "message", "결제가 취소되었습니다.",
                    "payment", response
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "updated", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 7. 주문/결제 정보 확인
    // GET - /api/payments/{orderId}/checkout
    @GetMapping("/{orderId}/checkout")
    public ResponseEntity<?> getCheckoutInfo(@PathVariable Long orderId) {
        try {
            CheckoutResponseDto response = paymentService.getCheckoutInfo(orderId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "checkout", response
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}