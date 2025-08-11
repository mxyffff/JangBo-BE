package me.swudam.jangbo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// 이메일 인증 "코드 발송 요청" DTO
// 역할: 사용자가 입력한 이메일을 서버로 전달
@Data
public class EmailRequestDto {
    // 인증 코드를 보낼 대상 이메일
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식을 확인해주세요.")
    private String email;
}
