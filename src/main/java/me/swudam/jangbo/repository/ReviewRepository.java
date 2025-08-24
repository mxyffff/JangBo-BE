package me.swudam.jangbo.repository;

import me.swudam.jangbo.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/* 리뷰 저장소 */
// 한 주문(order) 안의 개별 상품(product) 단위로 리뷰 1회 작성 가능 (고객별·주문별·상품별 유니크)
// 수정/삭제를 위해 본인 소유(reviewId + customerId) 조회 메서드 제공
// 마이페이지 “주문(최신순) 하위 상품 리스트”에서 어떤 상품이 이미 리뷰됐는지 빠르게 판단할 수 있도록 (orderId, productId) 튜플 조회 제공
// 상점 평균 별점/카운트 조회(스토어 메인 목록 노출용) JPQL 제공
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /* 단건 존재/중복 확인 & 단건 조회 (리뷰 수정·삭제 등에 사용) */
    // 고객 + 주문 + 상품으로 리뷰 존재 여부 (유니크 제약과 짝)
    boolean existsByCustomer_IdAndOrder_IdAndProduct_Id(Long customerId, Long orderId, Long productId);

    // 고객 + 주문 + 상품으로 리뷰 단건 조회 (있으면 Optional 반환)
    Optional<Review> findByCustomer_IdAndOrder_IdAndProduct_Id(Long customerId, Long orderId, Long productId);

    // 본인 리뷰만 안전하게 수정/삭제할 수 있도록 reviewId + customerId 동시 검증
    Optional<Review> findByIdAndCustomer_Id(Long reviewId, Long customerId);

    /* 고객 마이페이지용 조회 */
    // 고객이 남긴 모든 리뷰를 최신순으로
    List<Review> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    // 특정 주문에 대해 고객이 남긴 리뷰(여러 상품일 수 있음)
    List<Review> findByCustomer_IdAndOrder_IdOrderByCreatedAtDesc(Long customerId, Long orderId);

    /* 마이페이지 "주문별 상품 리스트" 화면에서 이미 리뷰된 조합(orderId, productId)을 한 번에 가져오기 위한 튜플 조회 */
    // 서비스단에서 Set<Pair(orderId, productId)> 형태로 변환해두면 O(1)로 "리뷰 여부" 표시가 가능
    @Query("""
           select r.order.id as orderId, r.product.id as productId
           from Review r
           where r.customer.id = :customerId
             and r.order.id in :orderIds
           """)
    List<OrderProductTuple> findReviewedTuplesByCustomerAndOrderIds(
            @Param("customerId") Long customerId,
            @Param("orderIds") Collection<Long> orderIds
    );

    /* 위 JPQL 결과를 담을 프로젝션 인터페이스 */
    interface OrderProductTuple {
        Long getOrderId();
        Long getProductId();
    }

    /* 상품/상점 별점 통계 (실시간 계산용) */
    // 메인 리스트에서 상점 카드에 노출할 평균 별점

    // [상품 단위] 평균 별점
    @Query("select coalesce(avg(r.rating), 0) from Review r where r.product.id = :productId")
    double getAverageRatingByProductId(@Param("productId") Long productId);

    // [상품 단위] 리뷰 개수
    long countByProduct_Id(Long productId);

    // [상점 단위] 평균 별점 = 상점이 보유한 모든 상품의 리뷰 평균
    @Query("""
           select coalesce(avg(r.rating), 0)
           from Review r
           where r.product.store.id = :storeId
           """)
    double getAverageRatingByStoreId(@Param("storeId") Long storeId);

    // [상점 단위] 리뷰 총 개수
    long countByProduct_Store_Id(Long store);

    // 여러 상점의 평균 별점을 **한 번에** 가져오고 싶을 때(메인 목록 최적화 용도)
    // - 반환은 [storeId, avg] 튜플 리스트. 서비스에서 Map<Long, Double>로 바꿔 쓰기 좋습니다.
    @Query("""
           select r.product.store.id as storeId, avg(r.rating) as avgRating
           from Review r
           where r.product.store.id in :storeIds
           group by r.product.store.id
           """)
    List<StoreAvgTuple> getAverageRatingByStoreIds(@Param("storeIds") Collection<Long> storeIds);

    interface StoreAvgTuple {
        Long getStoreId();
        Double getAvgRating();
    }

    /* 특정 상품/주문 묶음의 리뷰들 */
    // 주문 내 특정 상품들만 모아 보기
    List<Review> findByOrder_IdAndProduct_IdIn(Long orderId, Set<Long> productIds);

    // 상품 단위로 모아 보기
    List<Review> findByProduct_IdOrderByCreatedAtDesc(Long productId);
}
