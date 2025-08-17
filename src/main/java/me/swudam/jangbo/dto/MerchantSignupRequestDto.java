package me.swudam.jangbo.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

// [온보딩] 상인
@Getter
@Setter
public class MerchantSignupRequestDto {
    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String username;

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식으로 입력해주세요.") // 이메일 형식에 맞는지 확인
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Length(min = 8, max = 16, message = "비밀번호는 8~16자로 입력해주세요.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 입력 값입니다.")
    private String passwordConfirm;

    // PasswordValidator 유틸로 검증
    @AssertTrue(message = "비밀번호는 8~16자이며, 허용 특수문자 8자( ! # $ % & * @ ^ ) 중 최소 1개를 포함해야 합니다.")
    public boolean isPasswordPolicySatisfied() {
        // PasswordValidator는 별도 유틸 고정 구현으로 가정
        // null 대응: @NotBlank에서 이미 차단되지만, 혹시 모를 NPE 방지
        if (password == null) return false;
        return me.swudam.jangbo.util.PasswordValidator.isValid(password);
    }

    // 새로 추가
    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordConfirmed() {
        if (password == null || passwordConfirm == null) return false;
        return password.equals(passwordConfirm);
    }
}
