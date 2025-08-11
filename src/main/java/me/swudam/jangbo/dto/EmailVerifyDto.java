package me.swudam.jangbo.dto;

// 이메일 인증 "코드 검증" DTO
// 역할: 사용자가 메일로 받은 코드르 제출 -> 서버가 Redis의 코드와 비교 검증

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

// 흐름
// 1. POST /api/customers/email/verify
// 2. 본 DTO로 (email, code)를 받고, 서비스에서
// - Redis의 email:code:{email} 값을 조회
// - 일치하면 email:verified:{email} = "true"(TTL 30m) 저장 후 code 키 삭제
// 3. 이후 회원가입 시 이 verified 플래그를 확인하여 통과 여부 결정
@Data
public class EmailVerifyDto {
    // 검증 대상 이메일
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식을 확인해주세요.")
    private String email;

    // 메일로 받은 6자리 인증 코드 (숫자만)
    @NotBlank(message = "인증 코드를 입력해주세요.")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자입니다.")
    private String code;
}
