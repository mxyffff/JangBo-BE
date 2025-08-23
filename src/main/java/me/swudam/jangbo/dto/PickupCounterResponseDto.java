package me.swudam.jangbo.dto;

import lombok.*;
import me.swudam.jangbo.dto.order.OrderResponseDto;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 픽업대 상태 보여주는 DTO
public class PickupCounterResponseDto {
    private int counterNumber;
    private OrderResponseDto order; // 없으면 null
}
