package me.swudam.jangbo.controller;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.review.ReviewCreateRequestDto;
import me.swudam.jangbo.dto.review.ReviewResponseDto;
import me.swudam.jangbo.dto.review.ReviewUpdateRequestDto;
import me.swudam.jangbo.repository.CustomerRepository;
import me.swudam.jangbo.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// 리뷰 - 고객 전용 API (인증 필요)
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewCustomerController {

    private final ReviewService reviewService;
    private final CustomerRepository customerRepository;

    // 리뷰 작성
    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid ReviewCreateRequestDto requestDto) {
        Long customerId = currentCustomerIdOrThrow(); // 로그인 안되어 있으면 401 예외
        ReviewResponseDto responseDto = reviewService.create(customerId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "created", true,
                "review", responseDto
        ));
    }

    // 리뷰 수정
    @PatchMapping("/{reviewId}")
    public ResponseEntity<?> update(@PathVariable Long reviewId,
                                    @RequestBody @Valid ReviewUpdateRequestDto requestDto) {
        Long customerId = currentCustomerIdOrThrow();
        ReviewResponseDto responseDto = reviewService.update(customerId, reviewId, requestDto);
        return ResponseEntity.ok(Map.of(
                "updated", true,
                "review", responseDto
        ));
    }

    // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> delete(@PathVariable Long reviewId) {
        Long customerId = currentCustomerIdOrThrow();
        reviewService.delete(customerId, reviewId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // 내 리뷰 최신순 목록
    @GetMapping("/me")
    public ResponseEntity<?> myReviews() {
        Long customerId = currentCustomerIdOrThrow();
        List<ReviewResponseDto> list = reviewService.getMyReviewsByRecent(customerId);
        return ResponseEntity.ok(Map.of(
                "found", true,
                "reviews", list
        ));
    }

    // “리뷰 남기기” 화면에서 이미 리뷰한 (orderId, productId) 쌍 조회
    @PostMapping("/me/reviewed-pairs")
    public ResponseEntity<?> reviewedPairs(@RequestBody ReviewedPairsRequest body) {
        Long customerId = currentCustomerIdOrThrow();
        var pairs = reviewService.getReviewedPairs(customerId, body.orderIds());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "reviewed", pairs
        ));
    }


    /* ----- 내부 유틸: 현재 로그인 고객 id 꺼내기 ----- */
    private Long currentCustomerIdOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || isAnonymous(auth)) {
            // 전역 예외처리기에서 401 + 통일 포맷으로 변환됨
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }
        String email = (auth.getPrincipal() instanceof UserDetails ud) ? ud.getUsername()
                : String.valueOf(auth.getPrincipal());
        return customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("로그인 정보를 찾을 수 없습니다."))
                .getId();
    }

    private boolean isAnonymous(Authentication auth) {
        String name = auth.getName();
        return name == null || "anonymousUser".equals(name);
    }

    /** 요청 바디 DTO(간단): 여러 주문 id */
    public record ReviewedPairsRequest(Collection<Long> orderIds) {}
}
