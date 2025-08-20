package me.swudam.jangbo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

// 상품 주문 응답 DTO
@Getter
@Setter
@AllArgsConstructor
public class OrderProductResponseDto {
    private Long productId;
    private String productName;
    private int price;
    private int quantity;
}