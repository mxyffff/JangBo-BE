package me.swudam.jangbo.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 주문 생성 요청 DTO
@Getter
@Setter
public class OrderRequestDto {
    private Long storeId; // 주문할 상점
    private List<OrderProductRequestDto> products; // 주문 상품 리스트
}