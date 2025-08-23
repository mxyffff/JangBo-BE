package me.swudam.jangbo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Order와 1:1 연관관계
    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private BigDecimal amount; // 결제 금액 (주문 금액 + 픽업팁)
    private String method; // 결제 방법 (계좌이체)

    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // 결제 상태 ENUM
}
