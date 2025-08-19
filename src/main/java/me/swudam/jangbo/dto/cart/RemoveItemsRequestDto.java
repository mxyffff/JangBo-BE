package me.swudam.jangbo.dto.cart;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// [요청 DTO] 선택 항목 삭제
@Getter @Setter
public class RemoveItemsRequestDto {

    @NotEmpty(message = "삭제할 항목 id 목록이 비어있습니다.")
    private List<Long> itemIds;
}
