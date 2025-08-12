package me.swudam.jangbo.repository;

import me.swudam.jangbo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Optional -> null일 수도 있는 값을 감싸고 .orElseThrow()로 예외 던지기 가능
    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByUsername(String username);

    Optional<Customer> findByPhoneNumber(String phoneNumber);

    // 중복 체크 existsBy -> boolean을 즉시 반환하므로 가볍고 빠름
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // 하이픈 없이 숫자만 저장&전달 필요함
    boolean existsByPhoneNumber(String phoneNumber);

    // 로그인용: 이메일(대소문자 무시)로 고객 조회
    Optional<Customer> findByEmailIgnoreCase(String email);
}
