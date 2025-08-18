package me.swudam.jangbo.dto;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.entity.Order;
import me.swudam.jangbo.entity.OrderStatus;
import me.swudam.jangbo.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MerchantOrderService{

    private final OrderRepository orderRepository;

    // ------------------------
    // 1. 주문 수락 + 준비시간 설정
    // ------------------------
    @Transactional
    public void acceptOrder(Long merchantId, Long orderId, Integer preparationTime) {
        // 1) 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

        // 2) 상인 소속 확인: 다른 상점 주문이면 처리 불가
        if (!order.getStore().getMerchant().getId().equals(merchantId)) {
            throw new IllegalStateException("본인 상점의 주문만 처리할 수 있습니다.");
        }

        // 3) 상태 체크: 이미 처리된 주문이면 수락 불가
        if(order.getStatus() != OrderStatus.REQUESTED) {
            throw new IllegalStateException("이미 처리된 주문입니다.");
        }

        // 4) 주문 상태 변경 및 준비시간 설정
        order.setStatus(OrderStatus.ACCEPTED);
        order.setPreparationTime(preparationTime);
        order.setAcceptedAt(LocalDateTime.now()); // 주문 수락 시점
    }

    // ------------------------
    // 2. 주문 준비 완료
    // ------------------------
    @Transactional
    public void markOrderReady(Long orderId) {
        // 1) 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

        // 2) 상태 체크: ACCEPTED 또는 PREPARING 상태만 준비 완료 가능
        if(order.getStatus() != OrderStatus.ACCEPTED && order.getStatus() != OrderStatus.PREPARING) {
            throw new IllegalStateException("준비 중인 주문만 완료 처리 가능합니다.");
        }

        // 3) 상태 변경
        order.setStatus(OrderStatus.READY);
    }

    // ------------------------
    // 3. 상인 주문 취소 + 취소 사유
    // ------------------------
    @Transactional
    public void cancelOrderByMerchant(Long orderId, String reason) {
        // 1) 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

        // 2) 상태 체크: 이미 처리된 주문은 취소 불가
        if(order.getStatus() != OrderStatus.REQUESTED) {
            throw new IllegalStateException("이미 처리된 주문은 취소 불가합니다.");
        }

        // 3) 상태 변경 + 취소 사유 기록
        order.setStatus(OrderStatus.CANCELED);
        order.setCancelReason(reason);
    }

    // 기존 고객용 주문 조회/취소/생성 메서드는 그대로 유지
}