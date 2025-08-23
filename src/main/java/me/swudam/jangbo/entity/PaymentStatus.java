package me.swudam.jangbo.entity;

public enum PaymentStatus {
    PENDING, // 결제 대기
    APPROVED, // 결제 승인 완료
    DECLINED, // 결제 거부
    CANCELED // 결제 취소
}
