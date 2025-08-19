package me.swudam.jangbo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// 주문 당시 가격과 수량 기록 관리
@Entity
@Table(name = "order_products")
@Getter
@Setter
public class OrderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // 주문 당시 수량
    private int quantity;

    // 주문 당시 가격 (스냅샷)
    private int price;

    // 주문 상품 총 금액 계산
    public int getTotalPrice() {
        return price * quantity;
    }
}
