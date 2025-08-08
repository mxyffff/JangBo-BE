package me.swudam.jangbo.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.swudam.jangbo.dto.MerchantFormDto;
import org.springframework.security.crypto.password.PasswordEncoder;

// 상인-2 - [온보딩] 상인 상점 등록
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

    @Column(unique = true)
    private String username; // 유저 이름

    @Column(unique = true)
    private String email; // 이메일

    private String password; // 비밀번호

    @Column(unique = true)
    private String phoneNumber; // 전화번호

    private String storeName; // 상점 이름
    private String storeAddress; // 상점 위치(도로명주소)

    @Column(unique = true)
    private String businessNumber; // 사업자등록번호

    @Enumerated(EnumType.STRING)
    private Category category; // 카테고리 필드 추가

    public static Merchant createMerchant(MerchantFormDto merchantFormDto, PasswordEncoder passwordEncoder) {
        Merchant merchant = new Merchant();

        merchant.setUsername(merchantFormDto.getUsername()); // 유저 이름
        merchant.setEmail(merchantFormDto.getEmail()); // 이메일

        String pwd = passwordEncoder.encode(merchantFormDto.getPassword()); // 비밀번호
        merchant.setPassword(pwd);

        merchant.setPhoneNumber(merchantFormDto.getPhoneNumber()); // 전화번호
        merchant.setStoreName(merchantFormDto.getStoreName()); // 상점 이름
        merchant.setStoreAddress(merchantFormDto.getStoreAddress()); // 상점 위치
        merchant.setBusinessNumber(merchantFormDto.getBusinessNumber()); // 사업자등록번호

        merchant.setCategory(merchantFormDto.getCategory()); // 카테고리

        return merchant;
    }
}
