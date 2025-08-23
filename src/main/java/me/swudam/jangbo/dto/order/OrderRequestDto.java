package me.swudam.jangbo.dto.order;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// 주문 생성 요청 DTO
@Getter
@Setter
public class OrderRequestDto {
    private List<StoreOrderDto> storeOrders = new ArrayList<>(); // 기본값 빈 리스트

    @Getter @Setter
    public static class StoreOrderDto {
        private Long storeId;
        private List<ProductOrderDto> products = new ArrayList<>(); // 기본값 빈 리스트
    }

    @Getter @Setter
    public static class ProductOrderDto {
        private Long productId;
        private int quantity;
    }
}