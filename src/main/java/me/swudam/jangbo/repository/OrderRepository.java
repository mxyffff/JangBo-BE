package me.swudam.jangbo.repository;

import me.swudam.jangbo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// 주문 레포지토리
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 고객별 주문 조회
    List<Order> findByCustomerId(Long customerId);

    // 상인별(상점 기준) 주문 조회
    List<Order> findByStore_Merchant_Id(Long merchantId);

    // 픽업대 번호 조회
    @Query("SELECT o.pickupSlot FROM Order o WHERE o.store.id = :storeId AND o.pickupSlot IS NOT NULL AND o.status NOT IN ('CANCELED', 'COMPLETED')")
    List<Integer> findPickupSlotsByStoreId(@Param("storeId") Long storeId);

    // 특정 상점별 픽업대 조회
    Optional<Order> findByPickupSlotAndStoreId(Integer pickupSlot, Long storeId);
}