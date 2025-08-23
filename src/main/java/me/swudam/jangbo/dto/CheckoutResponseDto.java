package me.swudam.jangbo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// 결제 직전 주문/결제 정보 확인
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutResponseDto {

    // 주문 상품 리스트
    private List<OrderProductInfo> items;

    // 픽업 시장
    private String pickupMarket;

    // 주문자 정보
    private String customerName;
    private String customerEmail;

    // 결제 관련 정보
    private int orderAmount; // 주문 금액
    private int pickupTip;   // 픽업 수수료
    private int totalAmount; // 최종 결제 금액

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderProductInfo {
        private String productName;
        private String storeName;
        private int price;
        private int quantity;
    }
}
