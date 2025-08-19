package me.swudam.jangbo.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// [요청 DTO] 장바구니 항목 수량 ‘지정’ 변경 (증감 방식이 아닌 지정 방식)
@Getter @Setter
public class UpdateQuantityRequestDto {

    @NotNull(message = "장바구니 항목 id는 필수입니다.")
    private Long itemId;

    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private int quantity;
}
