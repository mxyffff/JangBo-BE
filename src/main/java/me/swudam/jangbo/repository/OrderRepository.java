package me.swudam.jangbo.repository;

import me.swudam.jangbo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 주문 레포지토리
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 고객별 주문 조회
    List<Order> findByCustomerId(Long customerId);

    // 상인별(상점 기준) 주문 조회
    List<Order> findByStore_Merchant_Id(Long merchantId);
}