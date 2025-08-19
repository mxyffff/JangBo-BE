package me.swudam.jangbo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 고객과 상점, 주문 상품들 연결
// 주문 상태, 총 금액
@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Integer deliveryFee = 0;
    private Integer totalPrice = 0;

    // 상인이 선택한 준비시간 (분 단위)
    private Integer preparationTime;

    // 수락 시간
    @Column
    private LocalDateTime acceptedAt;

    // 상인이 주문 취소 시 입력하는 사유
    private String cancelReason;

    // 픽업대 슬롯
    @Column(name = "pickup_slot")
    private Integer pickupSlot; // 1~10까지 부여되는 픽업대 번호

    /* 연관관계 편의 메서드 */
    public void addOrderProduct(OrderProduct op) {
        orderProducts.add(op);
        op.setOrder(this);
    }

    public void calculateTotalPrice() {
        this.totalPrice = orderProducts.stream()
                .mapToInt(OrderProduct::getTotalPrice)
                .sum();
    }

    // 주문 상품 영속성
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProduct> orderProducts = new ArrayList<>();
}