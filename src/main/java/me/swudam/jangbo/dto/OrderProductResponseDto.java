package me.swudam.jangbo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OrderProductResponseDto {
    private Long productId;
    private String productName;
    private int price;
    private int quantity;
}