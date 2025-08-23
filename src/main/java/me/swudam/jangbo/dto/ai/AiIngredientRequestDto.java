package me.swudam.jangbo.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// 사용자의 1차 입력 DTO
@Getter @Setter
public class AiIngredientRequestDto {
    // 사용자의 자연어 질문
    @NotBlank(message = "질문이 비어있습니다.")
    private String question;
}
