package me.swudam.jangbo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.swudam.jangbo.entity.OrderStatus;

import java.util.List;

// 주문 응답 DTO
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDto {
    private Long orderId;
    private OrderStatus status;
    private int totalPrice;
    private int deliveryFee;
    private String orderDate; // yyyy-MM-dd HH:mm:ss
    private String cancelReason;
    private List<OrderProductResponseDto> products;
    private Long remainingMinutes; // 분 단위 남은 시간
    private String updatedAt; // yyyy-MM-dd HH:mm:ss
}