package me.swudam.jangbo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderProductRequestDto {
    private Long productId;
    private int quantity;
}