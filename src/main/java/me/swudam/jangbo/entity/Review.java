package me.swudam.jangbo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "product_reviews",
        // 한 주문의 같은 상품에 대해 같은 고객이 한 번만 리뷰 가능
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_once_per_order_product",
                columnNames = {"customer_id", "order_id", "product_id"}
        ),
        indexes = {
                @Index(name = "idx_review_product", columnList = "product_id"),
                @Index(name = "idx_review_customer", columnList = "customer_id"),
                @Index(name = "idx_review_order", columnList = "order_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    // 리뷰 작성자 (고객)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // 어떤 주문에 대한 리뷰인지 (구매 증빙/권한 검증용)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 어떤 "상품"에 대한 리뷰인지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 별점: 1~5
    @Column(nullable = false)
    private int rating;

    /* 비즈니스 규칙 */
    public void validate() {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("별점은 1~5 사이여야 합니다.");
        }
    }

    /* 별점 수정용 도메인 메서드 */
    // 1 ~ 5 범위만 허용
    // 기존 값과 같으면 아무것도 하지 않음
    public void changeRating(int newRating) {
        // 범위 체크 (엔티티를 잘못된 상태로 만들지 않기 위해 먼저 검사)
        if (newRating < 1 || newRating > 5) {
            throw new IllegalArgumentException("별점은 1~5 사이여야 합니다.");
        }
        this.rating = newRating;
    }
}
