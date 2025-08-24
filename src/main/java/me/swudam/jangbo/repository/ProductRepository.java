package me.swudam.jangbo.repository;

import me.swudam.jangbo.entity.OrderStatus;
import me.swudam.jangbo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    /* 단건 조회 */
    // 상인 소유권을 함께 검증하는 단건 조회 (상인 전용 API에서 사용)
    Optional<Product> findByIdAndMerchantId(Long productId, Long merchantId);
    // 공개 단건 조회 (고객 API 전용) -> JPA 기본 메서드이므로 삭제
    // Optional<Product> findById(Long productId);

    /* 특정 상인 목록 조회 (정렬) */
    // 최신순 정렬
    List<Product> findAllByMerchantIdOrderByCreatedAtDesc(Long merchantId);
    // 저가순 정렬
    List<Product> findAllByMerchantIdOrderByPriceAsc(Long merchantId);
    // 인기순 정렬 (주문 테이블 생성 후 작성)
    // 신선순 정렬(유통기한 많이 남은 순)
    List<Product> findAllByMerchantIdOrderByExpiryDateDesc(Long merchantId);

    /* 특정 상인 상품: 인기순(완료 주문 건수 내림차순) */
    @Query("""
        select p
        from Product p
        left join OrderProduct op on op.product = p
        left join op.order o
        where p.merchant.id = :merchantId
        group by p.id
        order by sum(case when o.status = :completed then 1 else 0 end) desc,
                 p.createdAt desc
    """)
    List<Product> findPopularByMerchant(
            @Param("merchantId") Long merchantId,
            @Param("completed") OrderStatus completed
    );

    /* 전역 목록 조회 (정렬) - 고객 전용 */
    // 최신순 정렬
    List<Product> findAllByOrderByCreatedAtDesc();
    // 저가순
    List<Product> findAllByOrderByPriceAsc();
    // 신선순
    List<Product> findAllByOrderByExpiryDateDesc();

    /* 전역 상품: 인기순(완료 주문 건수 내림차순) */
    @Query("""
        select p
        from Product p
        left join OrderProduct op on op.product = p
        left join op.order o
        group by p.id
        order by sum(case when o.status = :completed then 1 else 0 end) desc,
                 p.createdAt desc
    """)
    List<Product> findAllOrderByPopularity(@Param("completed") OrderStatus completed);

    /* 검색: AI 장보 기능에서 2차 처리를 위한 세부 조건 검색 */
    // 검색: 특정 상인 + 이름 검색 + 최신순
    List<Product> findByMerchantIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(Long merchantId, String keyword);
    // 검색: 이름 검색 + 신선순(유통기한 많이 남은 순)
    List<Product> findByNameContainingIgnoreCaseOrderByExpiryDateDesc(String keyword);
    // 이름 검색 + 저가순
    List<Product> findByNameContainingIgnoreCaseOrderByPriceAsc(String keyword);

    /* 전역 검색: 인기순(완료 주문 건수 내림차순) */
    @Query("""
        select p
        from Product p
        left join OrderProduct op on op.product = p
        left join op.order o
        where lower(p.name) like lower(concat('%', :keyword, '%'))
        group by p.id
        order by sum(case when o.status = :completed then 1 else 0 end) desc,
                 p.createdAt desc
    """)
    List<Product> findByNameOrderByPopularity(
            @Param("keyword") String keyword,
            @Param("completed") OrderStatus completed
    );


    // 존재 여부: 중복 검증이나 사전 체크용
    boolean existsByIdAndMerchantId(Long productId, Long merchantId);
}
