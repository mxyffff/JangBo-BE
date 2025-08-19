package me.swudam.jangbo.dto.cart;

import lombok.Builder;
import lombok.Getter;

// [응답 DTO] 선택 삭제/비우기 결과
@Getter
@Builder
public class DeleteItemsResponseDto {
    private final int deletedCount;
    private final String message; // 예: "선택한 2개 항목을 삭제했습니다."
}
