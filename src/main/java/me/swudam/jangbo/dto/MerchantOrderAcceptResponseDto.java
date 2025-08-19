package me.swudam.jangbo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.swudam.jangbo.entity.OrderStatus;

// 상인 주문 수락 응답 DTO
@Getter
@Setter
@AllArgsConstructor
public class MerchantOrderAcceptResponseDto {
    private Long orderId;
    private OrderStatus status;
    private int totalPrice;
    private int deliveryFee;
    private int prepareTimeMinutes; // 선택한 준비시간
}