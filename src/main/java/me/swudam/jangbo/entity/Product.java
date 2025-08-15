package me.swudam.jangbo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product extends BaseTimeEntity {
    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    // 소유 상인 (N:1)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    // 상품명
    @Column(nullable = false, length = 100)
    private String name;

    // 원산지
    @Column(nullable = false, length = 100)
    private String origin;

    // 유통기한 - 'YYYY-MM-DD' 포맷
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    // 재고: 품절(true)일 때만 0 허용
    @Min(0)
    @Column(nullable = false)
    @Comment("재고 수량(품절일 때만 0 허용)")
    private Integer stock;

    // 가격 - 단위: 원
    @Min(value = 1, message = "가격은 1원 이상이어야 합니다.")
    @Column(nullable = false)
    @Comment("가격(원화, 정수)")
    private Integer price;

    // 이미지 URL
    // 실제 파일 업로드는 별도 모듈에서 처리
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // 품절 여부
    @Column(nullable = false)
    @Comment("품절 여부")
    private Boolean soldOut;

    /* 비즈니스 메서드 */
    // 규칙 강제:
    // - 재고 입력란은 항상 1 이상
    // - 재고가 0이 되는 유일한 경로는 "품절 버튼"(markSoldOut)

    // 품절 처리: 재고 강제로 0 세팅
    public void markSoldOut() {
        this.soldOut = true;
        this.stock = 0;
    }

    // 상품 수정(품절 해제 및 재고/기본정보 동시 수정 가능)
    // - 품절 상태에서 재고 >= 1 입력시 품절 해제
    // - 재고 < 1은 거부 (품절 버튼 외에는 0 허용 X)
    public void updateProduct(String name, String origin, LocalDate expiryDate, Integer price, int newStock, String imageUrl) {
        if (newStock < 1) {
            throw new IllegalArgumentException("재고는 1 이상이어야 합니다.");
        }

        // 품절 상태면 해제
        if (Boolean.TRUE.equals(this.soldOut)) {
            this.soldOut = false;
        }

        // 재고 및 기본 정보 수정
        this.stock = newStock;
        this.name = name;
        this.origin = origin;
        this.expiryDate = expiryDate;
        this.price = price;
        this.imageUrl = imageUrl;
    }
}