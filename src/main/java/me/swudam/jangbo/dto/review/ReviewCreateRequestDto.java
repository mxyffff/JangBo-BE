package me.swudam.jangbo.dto.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 생성 요청
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateRequestDto {
    private Long orderId;     // 어떤 주문의
    private Long productId;   // 어떤 상품에 대해
    private int rating;       // 1~5
}
