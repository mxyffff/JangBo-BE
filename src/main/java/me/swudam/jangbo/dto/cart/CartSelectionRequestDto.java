package me.swudam.jangbo.dto.cart;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// [요청 DTO] 선택된 장바구니 항목 목록
@Getter @Setter
public class CartSelectionRequestDto {

    private List<Long> selectedItemIds; // null/empty -> 전체
}
