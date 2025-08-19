package me.swudam.jangbo.dto.cart;

import lombok.Builder;
import lombok.Getter;

// [응답 DTO] 수량 변경 후 결과 알림
@Getter @Builder
public class UpdateQuantityResponseDto {
    private final Long itemId;
    private final int quantity;   // 변경 후 수량
    private final String message; // 예: "수량을 3개로 변경했습니다."
}
