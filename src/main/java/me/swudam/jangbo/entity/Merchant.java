package me.swudam.jangbo.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
@Entity
@Table(name="merchant")
@Getter
@Setter
public class Merchant {
    // PK
    @Id
    @Column (name = "merchant_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본키 id

    private String username; // 유저 이름

    @Column(unique = true)
    private String email; // 이메일

    private String password; // 비밀번호

    // 상인 영속성
    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Store> stores = new ArrayList<>();
    // 상품 영속성
    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();
}
