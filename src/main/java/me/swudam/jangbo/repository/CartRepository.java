package me.swudam.jangbo.repository;

import jakarta.persistence.LockModeType;
import me.swudam.jangbo.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

// 장바구니 루트 엔티티 저장소
// - 고객별로 장바구니는 1개라는 가정 → customerId 로 단건 조회하는 메서드 제공
// - 합계 계산/응답 DTO 생성 시 N+1을 피하려고 fetch 전략이 필요한 지점이 많으므로 items(+ product, store) 를 함께 로딩하는 메서드 별도로 제공(@EntityGraph 또는 fetch join)
// - 동시성(수량 증감, 담기) 상황을 고려한 비관적 락 메서드도 제공
public interface CartRepository extends JpaRepository<Cart, Long> {

    // 고객 ID로 Cart 단건 조회
    Optional<Cart> findByCustomer_Id(Long customerId);

    // 고객 id로 Cart 조회 + items / items.product / items.store까지 즉시 로딩
    // - 합계 계산/선택 상품 응답 생성시 N+1 방지
    // - @EntityGraph는 JPA 표준이라 유지보수에 유리
    @EntityGraph(attributePaths = {
            "items",
            "items.product",
            "items.store"
    })
    Optional<Cart> findWithItemsByCustomer_Id(Long customer_id);

    // 수량 변경/담기 같은 갱신 작업에 사용할 수 있는 비관적 락 쿼리
    // - 같은 고객의 Cart를 동시에 수정하는 경쟁을 직관적으로 제어
    // - 트랜잭션 경계 안에서 사용해야 함(@Transactional)
    // - 필요할 때만 사용 (잠금은 비용이 큼)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.customer.id = :customerId")
    Optional<Cart> findByCustomerIdForUpdate(Long customerId);
}
