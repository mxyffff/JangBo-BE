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
     * [2] 결제 승인 처리
     * - 주문 ID로 Order를 조회 (없으면 예외 발생)
     * - 해당 Order에 Payment 객체가 없으면 새로 생성 후 저장
     *   > 금액 = 주문 총 금액 + 배달(픽업) 수수료
     *   > 결제 방법 = 계좌이체 (하드코딩)
     *   > 상태 = APPROVED
     * - 이미 Payment가 존재하면 상태만 APPROVED로 변경
     * - Order 상태도 'ACCEPTED(주문 수락됨)' 으로 변경
     * - 최종적으로 PaymentResponseDto 반환
     */
    @Transactional
    public PaymentResponseDto approvePayment(Long orderId){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        Payment payment = order.getPayment();
        if (payment == null){
            // Payment가 아직 없으면 새로 생성
            payment = Payment.builder()
                    .order(order)
                    .amount(BigDecimal.valueOf(order.getTotalPrice() + order.getDeliveryFee()))
                    .method("ACCOUNT_TRANSFER")
                    .status(PaymentStatus.APPROVED)
                    .build();
            paymentRepository.save(payment);
            order.setPayment(payment);
        } else {
            // 기존 결제 정보가 있으면 상태만 갱신
            payment.setStatus(PaymentStatus.APPROVED);
        }

        // Order 상태를 결제 완료로 변경
        order.setStatus(OrderStatus.ACCEPTED);

        return toDto(payment);
    }

    /*
     * [3] 결제 거부 처리
     * - orderId로 Payment 조회 (없으면 예외 발생)
     * - Payment 상태를 DECLINED로 변경
     * - Order 상태는 변경하지 않음
     */
    @Transactional
    public PaymentResponseDto declinePayment(Long orderId){
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보가 존재하지 않습니다."));
        payment.setStatus(PaymentStatus.DECLINED);

        return toDto(payment);
    }

    /*
     * [4] 결제 취소 처리
     * - orderId로 Payment 조회 (없으면 예외 발생)
     * - Payment 상태를 CANCELED로 변경
     * - Order 상태도 다시 "REQUESTED(주문 요청됨)"으로 되돌린다.
     */
    @Transactional
    public PaymentResponseDto cancelPayment(Long orderId){
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보가 존재하지 않습니다."));
        payment.setStatus(PaymentStatus.CANCELED);

        // Order 상태는 다시 REQUESTED로 되돌림
        // 결제 취소 시 주문 상태도 초기화
        Order order = payment.getOrder();
        order.setStatus(OrderStatus.REQUESTED);

        return toDto(payment);
    }

    /*
     * [5] 상인 전용 - 특정 주문 건의 결제 내역 조회
     * - orderId로 Order 조회
     * - 해당 Order의 Store의 Merchant ID와 요청한 merchantId 비교
     * - 다르면 AccessDeniedException 발생
     * - 결제 정보(Payment)가 없으면 예외 발생
     * - 있으면 PaymentResponseDto 반환
     */
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentForMerchant(Long orderId, Long merchantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        // 본인 가게 주문인지 확인
        if (!order.getStore().getMerchant().getId().equals(merchantId)) {
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
