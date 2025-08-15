package me.swudam.jangbo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// 상품 수정 DTO
@Getter @Setter
public class ProductUpdateRequestDto {

    @NotBlank(message = "상품명을 입력하세요.")
    @Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "원산지를 입력하세요.")
    @Size(max = 100, message = "원산지는 100자 이하여야 합니다.")
    private String origin;

    // ISO-8601 'yyyy-MM-dd'
    @NotNull(message = "유통기한을 입력하세요.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    @NotNull(message = "가격을 입력하세요.")
    @Min(value = 1, message = "가격은 1원 이상이어야 합니다.")
    private Integer price;

    // 품절 상테에서 1 이상으로 수정이 들어오면 품절 해제 + 재고 갱신
    @NotNull(message = "재고 수량을 입력하세요.")
    @Min(value = 1, message = "재고 수량은 1 이상이어야 합니다.")
    private Integer stock;

    @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
    private String imageUrl;
}
