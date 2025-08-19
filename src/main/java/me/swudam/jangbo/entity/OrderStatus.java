package me.swudam.jangbo.entity;

// 주문 상태
public enum OrderStatus {
    REQUESTED, // 고객이 주문 요청, 상인 확인 전
    ACCEPTED, // 상인이 주문 수락, 준비시간 설정
    PREPARING, // 상인이 준비 중
    READY, // 픽업 대기
    COMPLETED, // 픽업 완료
    CANCELED; // 주문 취소

    public boolean canBeCanceledByCustomer() {
        return this == REQUESTED;
    }

    public boolean canBeCanceledByMerchant() {
        return this == REQUESTED;
    }
}