package me.swudam.jangbo.service;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.PaymentResponseDto;
import me.swudam.jangbo.entity.Order;
import me.swudam.jangbo.entity.OrderStatus;
import me.swudam.jangbo.entity.Payment;
import me.swudam.jangbo.entity.PaymentStatus;
import me.swudam.jangbo.repository.OrderRepository;
import me.swudam.jangbo.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /*
     * [1] 특정 주문에 대한 결제 정보 조회
     * - orderId로 Payment를 찾음
     * - 없으면 예외 발생 (IllegalArgumentException)
     * - 있으면 PaymentResponseDto로 변환하여 반환
     */
    public PaymentResponseDto getPaymentInfo(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보가 존재하지 않습니다."));

        return toDto(payment);
    }

    /*
     * [2] 결제 요청 생성
     * - Order 존재 확인
     * - Payment가 없으면 새로 생성(PENDING)
     * - 이미 존재하면 상태 그대로 반환
     */
    @Transactional
    public PaymentResponseDto requestPayment(Long orderId) { // ★신규 메서드
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        Payment payment = order.getPayment();
        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .amount(BigDecimal.valueOf(order.getTotalPrice() + order.getDeliveryFee()))
                    .method("ACCOUNT_TRANSFER")
                    .status(PaymentStatus.PENDING) // PENDING으로 생성
                    .build();
            paymentRepository.save(payment);
            order.setPayment(payment);
        }

        return toDto(payment);
    }

    /*
     * [3] 결제 승인 처리
     * - 기존 PENDING 결제를 APPROVED로 변경
     * - Order 상태 ACCEPTED로 변경
     */
    @Transactional
    public PaymentResponseDto approvePayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if (order.getStatus() == OrderStatus.ACCEPTED ||
                order.getStatus() == OrderStatus.PREPARING ||
                order.getStatus() == OrderStatus.READY ||
                order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("이미 결제되었거나 준비 중인 주문입니다.");
        }

        Payment payment = order.getPayment();
        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("결제 요청이 존재하지 않거나 이미 처리된 결제입니다.");
        }

        payment.setStatus(PaymentStatus.APPROVED);
        order.setStatus(OrderStatus.ACCEPTED);

        return toDto(payment);
    }


    /*
     * [4] 결제 거부 처리
     * - PENDING 결제를 DECLINED로 변경
     * - Order 상태는 REQUESTED 유지
     */
    @Transactional
    public PaymentResponseDto declinePayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보가 존재하지 않습니다."));
        Order order = payment.getOrder();

        if(order.getStatus() == OrderStatus.ACCEPTED ||
                order.getStatus() == OrderStatus.PREPARING ||
                order.getStatus() == OrderStatus.READY ||
                order.getStatus() == OrderStatus.COMPLETED){
            throw new IllegalStateException("이미 진행 중인 주문은 결제를 거부할 수 없습니다.");
        }

        if(payment.getStatus() != PaymentStatus.PENDING){
            throw new IllegalStateException("처리 가능한 결제가 아닙니다.");
        }

        payment.setStatus(PaymentStatus.DECLINED);
        order.setStatus(OrderStatus.REQUESTED); // 결제 실패 시 주문 초기 상태 유지

        return toDto(payment);
    }

    /*
     * [5] 결제 취소 처리
     * - PENDING 혹은 APPROVED 결제를 CANCELED로 변경
     * - Order 상태 REQUESTED로 되돌림
     */
    @Transactional
    public PaymentResponseDto cancelPayment(Long orderId){
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보가 존재하지 않습니다."));
        Order order = payment.getOrder();

        if(order.getStatus() == OrderStatus.READY || order.getStatus() == OrderStatus.COMPLETED){
            throw new IllegalStateException("이미 준비 완료된 주문은 결제를 취소할 수 없습니다.");
        }

        if(payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.APPROVED){
            throw new IllegalStateException("취소 가능한 결제가 아닙니다.");
        }

        payment.setStatus(PaymentStatus.CANCELED);
        order.setStatus(OrderStatus.REQUESTED);

        return toDto(payment);
    }

    /*
     * [6] 상인 전용 - 특정 주문 건의 결제 내역 조회
     */
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentForMerchant(Long orderId, Long merchantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if(!order.getStore().getMerchant().getId().equals(merchantId)){
            throw new SecurityException("해당 주문에 접근할 권한이 없습니다.");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역이 존재하지 않습니다."));

        return toDto(payment);
    }

    /*
     * [Helper] Entity → DTO 변환
     * - Controller 응답에 사용할 수 있도록 PaymentResponseDto로 변환
     */
    private PaymentResponseDto toDto(Payment payment) {
        return new PaymentResponseDto(
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus()
        );
    }
}
