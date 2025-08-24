package me.swudam.jangbo.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 응답(리스트/상세 공용
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewResponseDto {
    private Long reviewId;
    private Long orderId;
    private Long productId;
    private int rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
