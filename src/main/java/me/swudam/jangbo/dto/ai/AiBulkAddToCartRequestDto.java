package me.swudam.jangbo.dto.ai;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// 추천 결과를 한 번에 장바구니에 담는 요청 DTO
// - items: {productId, quantity} 배열 (quantity 미지정 시 1로 처리)
@Getter @Setter
public class AiBulkAddToCartRequestDto {

    @NotEmpty(message = "담을 상품 목록(items)는 비어 있을 수 없습니다.")
    private List<Item> items = new ArrayList<>();

    @Getter @Setter
    public static class Item {
        @NotNull(message = "productId는 필수입니다.")
        private Long productId;

        @Min(value = 1, message = "quantity는 1 이상이어야 합니다.")
        private Integer quantity = 1;
    }
}
