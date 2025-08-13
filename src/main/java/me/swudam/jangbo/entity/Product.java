package me.swudam.jangbo.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "products",
        indexes = { @Index(name = "idx_product_merchant", columnList = "merchant_id") }
)
public class Product extends BaseTimeEntity{
    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    // 소유 상인 (N:1)
}
