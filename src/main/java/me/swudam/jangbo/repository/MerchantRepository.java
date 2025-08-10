package me.swudam.jangbo.repository;

import me.swudam.jangbo.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

// [온보딩] 상인
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    // 이메일 중복 검증
    Merchant findByEmail(String email);
    String email(String email);

    // 사업자등록번호 중복 검증
    Merchant findByBusinessNumber(String businessNumber);
    String businessNumber(String businessNumber);
}
