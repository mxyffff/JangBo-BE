package me.swudam.jangbo.dto.ai;

import lombok.Builder;
import lombok.Value;

// 프론트에 내려줄 상품 최소 정보 묶음
@Value
@Builder
public class ProductSummaryDto {
    Long id;
    String name;
    Integer price;
    String expiryDate;
    Integer stock;
    Boolean soldOut;

    Long storeId;
    String storeName;

    String imageUrl;
}
