package me.swudam.jangbo.dto;

// 이메일 인증 => 서비스 레이어에서 처리하므로 DTO에서는 입력값 검증에만 집중!

import jakarta.validation.constraints.*;
import lombok.Data;

// 회원가입 요청 DTO(이메일 인증+가입 전 단계에서 사용)
@Data
public class CustomerSignupRequestDto {

    // 화면 표시용 닉네임
    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(min = 2, max = 10, message = "닉네임은 2~10자로 입력해주세요.")
    private String username;

    // 로그인용 이메일
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식을 확인해주세요.")
    private String email;

    // 비밀번호 (평문은 DB에 저장X)
    // 길이 제한은 @Size로 1차 검증
    // 상세 제한은 맨 아래 @AssertTrue 메서드에서 PasswordValidator 클래스를 호출하여 2차 검증
    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, max = 16, message = "비밀번호는 8~16자여야 합니다.")
    private String password;

    // 비밀번호 재입력(확인) - 화면에서 두 번 입력 받아 일치 여부를 검증
    @NotBlank(message = "비밀번호 확인란을 입력해주세요.")
    private String passwordConfirm;

    // 전화번호
    @NotBlank(message = "전화번호를 입력해주세요.")
    @Pattern(regexp = "^01[0-9]{8,9}$", message = "010으로 시작하는 숫자만 입력해주세요.")
    private String phoneNumber;


    /* 1. 비밀번호 정책 검증 */
    // 허용 특수문자 32자(₩ 포함), 특수문자 최소 1개 이상
    // PasswordValidator 유틸로 검증
    @AssertTrue(message = "비밀번호는 8~16자이며, 허용 특수문자 32자 중 최소 1개를 포함해야 합니다.")
    public boolean isPasswordPolicySatisfied() {
        // PasswordValidator는 별도 유틸 고정 구현으로 가정
        // null 대응: @NotBlank에서 이미 차단되지만, 혹시 모를 NPE 방지
        if (password == null) return false;
        return me.swudam.jangbo.util.PasswordValidator.isValid(password);
    }

    /* 2. 비밀번호 재입력 일치 검증 */
    // password와 passwordConfim이 동일해야 함
    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordConfirmed() {
        if(password == null || passwordConfirm == null) return false;
        return password.equals(passwordConfirm);
    }
}
