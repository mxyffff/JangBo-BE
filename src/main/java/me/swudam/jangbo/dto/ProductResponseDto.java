package me.swudam.jangbo.dto;

import lombok.Builder;
import lombok.Value;
import me.swudam.jangbo.entity.Product;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 상품 단건 응답 DTO (외부 노출 전용 모델)
@Value
@Builder
public class ProductResponseDto {

    Long id;

    String name;
    String origin;
    String expiryDate; // 표준 ISO(yyyy-MM-dd) 문자열로 응답
    Integer stock;
    Integer price;
    Boolean soldOut;

    String imageUrl;

    String createdAt; // ISO-8601 문자열
    String updatedAt; // ISO-8601 문자열

    // 응답용 식별자 필드
    Long storeId; // 소유 상점 id
    Long merchantId; // 소유 상인 id

    // 엔티티 -> 응답 DTO 변환
    public static ProductResponseDto from(Product p) {
        DateTimeFormatter d = DateTimeFormatter.ISO_LOCAL_DATE;

        return ProductResponseDto.builder()
                .storeId(p.getStore().getId()) // 상점 id
                .merchantId(p.getMerchant().getId()) // 상인 id
                .id(p.getId())
                .name(p.getName())
                .origin(p.getOrigin())
                .expiryDate(toDateString(p.getExpiryDate(), d))
                .stock(p.getStock())
                .price(p.getPrice())
                .soldOut(p.getSoldOut())
                .imageUrl(p.getImageUrl())
                .createdAt(toString(p.getCreatedAt()))
                .updatedAt(toString(p.getUpdatedAt()))
                .build();
    }

    // 엔티티 리스트 -> 응답 리스트 반환
    public static List<ProductResponseDto> fromList(List<Product> products) {
        return products.stream().map(ProductResponseDto::from).toList();
    }

    // 내부 메서드
    private static String toDateString(LocalDate date, DateTimeFormatter f) {
        return (date == null) ? null : date.format(f);
        // 필요 시, 한국어 포맷 등으로 변경 가능: DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private static String toString(java.time.LocalDateTime t) {
        return (t == null) ? null : t.toString();
    }
}
