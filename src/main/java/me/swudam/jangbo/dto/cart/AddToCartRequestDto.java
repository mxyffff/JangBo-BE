package me.swudam.jangbo.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// [요청 DTO] - 장바구니에 상품을 담을 때 사용
@Getter @Setter
public class AddToCartRequestDto {

    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    @Min(value = 1, message = "수량은 기본 1 이상이어야 합니다.")
    private int quantity = 1; // 기본값 1
}
