package me.swudam.jangbo.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.swudam.jangbo.dto.MerchantFormDto;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

// [온보딩] 상인
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

    public static Merchant createMerchant(MerchantFormDto merchantFormDto, PasswordEncoder passwordEncoder) {
        Merchant merchant = new Merchant();

        merchant.setUsername(merchantFormDto.getUsername()); // 유저 이름
        merchant.setEmail(merchantFormDto.getEmail()); // 이메일

        String pwd = passwordEncoder.encode(merchantFormDto.getPassword()); // 비밀번호
        merchant.setPassword(pwd);

        return merchant;
    }
}
