package me.swudam.jangbo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;


// 장바구니의 개별 품목
// - 하나의 Cart에 여러 CartItem이 속함 (N:1)
// - 같은 Cart 내 같은 Product만 한 줄만 존재하도록 UNIQUE 제약
// - 가격은 실시간(Product.price)으로 적용 -> 스냅샷 컬럼 사용 안함
@Entity
@Table(name = "cart_items",
        uniqueConstraints = {
                // 동일 카트에서 같은 상품은 1행만 존재 -> 수량만 증가
                @UniqueConstraint(name = "uk_cart_item_cart_product", columnNames = {"cart_id", "product_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CartItem extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long id;

    // 소속 장바구니 (N:1)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    @Setter(AccessLevel.PACKAGE)
    private Cart cart;

    // 상품 소유 상점
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // 담긴 상품 (N:1)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 수량 (최소 1)
    @Min(1)
    @Column(nullable = false)
    private Integer quantity;

    // 수량 변경 유틸 (하한 1 보장)
    public void changeQuantity(int newQty) {
        if (newQty < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        this.quantity = newQty;
    }

    // 현재 상품가 기준 라인 합계
    public int lineTotal() {
        return product.getPrice() * this.quantity;
    }
}
