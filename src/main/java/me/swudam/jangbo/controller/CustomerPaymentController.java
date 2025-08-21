package me.swudam.jangbo.controller;

import lombok.RequiredArgsConstructor;
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

    // 1. 결제 정보 조회
    // GET - /api/payments/{orderId}
    @GetMapping("/orderId")
    public ResponseEntity<?> getPaymentInfo(@PathVariable Long orderId) {
        try{
            PaymentResponseDto response = paymentService.getPaymentInfo(orderId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(Map.of(
                    "found", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 2. 결제 승인
    // POST - /api/payments/{orderId}/approve
    @PostMapping("/{orderId}/approve")
    public ResponseEntity<?> approvePayment(@PathVariable Long orderId) {
        try{
            PaymentResponseDto response = paymentService.approvePayment(orderId);
            return ResponseEntity.ok(Map.of(
                    "updated", true,
                    "message", "결제가 승인됐습니다.",
                    "payment", response
            ));
        } catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(Map.of(
                    "updated", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 3. 결제 거부
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
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "updated", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 4. 결제 취소
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
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "updated", false,
                    "message", ex.getMessage()
            ));
        }
    }
}

