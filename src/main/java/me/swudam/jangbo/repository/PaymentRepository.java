package me.swudam.jangbo.repository;

import me.swudam.jangbo.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Order와 1:1 매핑된 Payment 조회
    Optional<Payment> findByOrderId(Long orderId);
}
