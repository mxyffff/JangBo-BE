package me.swudam.jangbo.service;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.review.ReviewCreateRequestDto;
import me.swudam.jangbo.dto.review.ReviewResponseDto;
import me.swudam.jangbo.dto.review.ReviewUpdateRequestDto;
import me.swudam.jangbo.entity.*;
import me.swudam.jangbo.repository.CustomerRepository;
import me.swudam.jangbo.repository.OrderRepository;
import me.swudam.jangbo.repository.ProductRepository;
import me.swudam.jangbo.repository.ReviewRepository;
import me.swudam.jangbo.support.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    // 생성
    public ReviewResponseDto create(Long customerId, ReviewCreateRequestDto requestDto) {
        // 1. 주문 존재 + 내 주문인지 확인
        Order order = orderRepository.findById(requestDto.getOrderId())
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다."));
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("본인 주문에만 리뷰를 남길 수 있습니다.");
        }
        // 2. 픽업 완료 이후에만 리뷰 가능
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("픽업 완료된 주문만 리뷰할 수 있습니다.");
        }

        // 3. 상품 존재 + 주문에 포함된 상품인지 확인
        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다."));
        boolean inOrder = order.getOrderProducts().stream()
                .anyMatch(op -> op.getProduct().getId().equals(product.getId()));
        if (!inOrder) {
            throw new IllegalArgumentException("해당 주문에 포함되지 않은 상품입니다.");
        }

        // 4. 중복 체크 (같은 주문/상품에 이미 작성했는지)
        boolean duplicated = reviewRepository
                .existsByCustomer_IdAndOrder_IdAndProduct_Id(customerId, order.getId(), product.getId());
        if (duplicated) {
            throw new IllegalStateException("이미 리뷰를 작성하셨습니다.");
        }

        // 5. 고객 로드
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("고객을 찾을 수 없습니다."));

        // 6. 엔티티 만들고 저장
        Review review = Review.builder()
                .customer(customer)
                .order(order)
                .product(product)
                .rating(requestDto.getRating())
                .build();
        review.validate(); // 1~5 체크
        Review saved = reviewRepository.save(review);

        return toDto(saved);
    }

    // 수정
    public ReviewResponseDto update(Long customerId, Long reviewId, ReviewUpdateRequestDto requestDto) {
        // 본인 리뷰만 조회
        Review review = reviewRepository.findByIdAndCustomer_Id(reviewId, customerId)
                .orElseThrow(() -> new NotFoundException("리뷰를 찾을 수 없거나 권한이 없습니다."));

        // 별점만 수정
        review.changeRating(requestDto.getRating());

        reviewRepository.saveAndFlush(review);

        return toDto(review);
    }

    // 삭제
    public void delete(Long customerId, Long reviewId){
        Review review = reviewRepository.findByIdAndCustomer_Id(reviewId, customerId)
                .orElseThrow(() -> new NotFoundException("리뷰를 찾을 수 없거나 권한이 없습니다."));
        reviewRepository.delete(review);
    }

    // 내 리뷰 (최신순)
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getMyReviewsByRecent(Long customerId) {
        return reviewRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toDto)
                .toList();
    }

    // 리뷰 남기기 화면용
    // - 여러 주문 id를 한 번에 보내면 이미 리뷰한 (orderId, productId) 세트를 돌려줌
    // - 프론트는 이 세트로 별 아이콘/버튼 상태를 바로 표시하면 됨
    @Transactional(readOnly = true)
    public Set<ReviewedKey> getReviewedPairs(Long customerId, Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return Collections.emptySet();

        List<ReviewRepository.OrderProductTuple> tuples =
                reviewRepository.findReviewedTuplesByCustomerAndOrderIds(customerId, orderIds);

        return tuples.stream()
                .map(t -> new ReviewedKey(t.getOrderId(), t.getProductId()))
                .collect(Collectors.toSet());
    }

    // (orderId, productId) 한 쌍을 담는 레코드 객체
    public record ReviewedKey(Long orderId, Long productId) {}

    // 평균 별점
    @Transactional(readOnly = true)
    public double getStoreAverageRating(Long storeId) {
        return reviewRepository.getAverageRatingByStoreId(storeId);
    }

    // 메인 리스트에서 여러 상점의 평균 별점을 한 번에 받아오고 싶을 때 사용
    @Transactional(readOnly = true)
    public Map<Long, Double> getStoreAverageRatings(Collection<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) return Map.of();

        return reviewRepository.getAverageRatingByStoreIds(storeIds).stream()
                .collect(Collectors.toMap(
                        ReviewRepository.StoreAvgTuple::getStoreId,
                        t -> Optional.ofNullable(t.getAvgRating()).orElse(0.0)
                ));
    }

    @Transactional(readOnly = true)
    public ProductRatingSummary getProductRatingSummary(Long productId) {
        double avg = reviewRepository.getAverageRatingByProductId(productId);
        long cnt = reviewRepository.countByProduct_Id(productId);
        return new ProductRatingSummary(avg,cnt);
    }

    public record ProductRatingSummary(double average, long count) {}

    @Transactional(readOnly = true)
    public Optional<Long> getMyReviewId(Long customerId, Long orderId, Long productId) {
        // 쿼리 자체가 customerId로 필터하기 때문에, 내 주문/상품이 아니면 빈 값 반환됨
        return reviewRepository.findIdByCustomerAndOrderAndProduct(customerId, orderId, productId);
    }

    @Transactional(readOnly = true)
    public List<ReviewIdItem> getMyReviewIdItemsByOrders(Long customerId, Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return List.of();
        return reviewRepository.findReviewIdTuplesByCustomerAndOrderIds(customerId, orderIds).stream()
                .map(t -> new ReviewIdItem(t.getOrderId(), t.getProductId(), t.getReviewId()))
                .toList();
    }

    // 프론트 친화형 응답 아이템
    public record ReviewIdItem(Long orderId, Long productId, Long reviewId) {}

    // 내부 변환 메서드
    private ReviewResponseDto toDto(Review review){
        return ReviewResponseDto.builder()
                .reviewId(review.getId())
                .orderId(review.getOrder().getId())
                .productId(review.getProduct().getId())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
