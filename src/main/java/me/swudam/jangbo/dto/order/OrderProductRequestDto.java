package me.swudam.jangbo.dto.order;

import lombok.Getter;
import lombok.Setter;

// 상품 주문 요청 DTO
@Getter
@Setter
public class OrderProductRequestDto {
    private Long productId;
    private int quantity;
}