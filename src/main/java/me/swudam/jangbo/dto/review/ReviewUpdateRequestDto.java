package me.swudam.jangbo.dto.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 수정 요청
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewUpdateRequestDto {
    private int rating; // 1
}
