package me.swudam.jangbo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerLoginRequestDto {

    // 로그인 식별자로 사용할 이메일
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식을 확인해주세요.")
    private String email;

    // 비밀번호 평문 (DB에 저장X)
    // - 여기서는 기본적인 길이만 확인
    // - 실제 인증은 BCrypt 해시 비교(스프링 시큐리티)가 수행함
    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, max = 16, message = "비밀번호는 8~16자 사이입니다.")
    private String password;
}
