package me.swudam.jangbo.repository;

import me.swudam.jangbo.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// [온보딩] 상인
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    // 특정 이메일로 회원 조회 (로그인, 상세 조회 등의 용도)
    Merchant findByEmail(String email);
    // 이메일 중복 여부 확인
    boolean existsByEmail(String email);

    // 대소문자 무시하고 이메일 조회
    Optional<Merchant> findByEmailIgnoreCase(String email);
}
