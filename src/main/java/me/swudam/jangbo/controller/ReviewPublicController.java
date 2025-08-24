package me.swudam.jangbo.controller;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

// 퍼블릭 리뷰 조회 API
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class ReviewPublicController {

    private final ReviewService reviewService;

    // 상점 평균 별점 (단일)
    @GetMapping("/stores/{storeId}/rating")
    public ResponseEntity<?> storeRating(@PathVariable Long storeId) {
        double avg = reviewService.getStoreAverageRating(storeId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "storeId", storeId,
                "average", avg
        ));
    }

    // 상점 평균 별점 (메인 페이지 리스트용)
    @PostMapping("/stores/ratings")
    public ResponseEntity<?> storeRatings(@RequestBody StoreIdsRequest body) {
        var ratings = reviewService.getStoreAverageRatings(body.storeIds());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "ratings", ratings  // {"321":4.4, ...}
        ));
    }

    // 요청 바디 DTO: 여러 상점 id
    public record StoreIdsRequest(Collection<Long> storeIds) {}
}
