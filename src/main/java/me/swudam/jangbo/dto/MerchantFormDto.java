package me.swudam.jangbo.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

// [온보딩] 상인
@Getter
@Setter
public class MerchantFormDto {
    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String username;

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "잘못된 이메일 형식입니다.") // 이메일 형식에 맞는지 확인
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Length(min = 8, max = 16, message = "비밀번호는 8~16자로 입력해주세요.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 입력 값입니다.")
    private String passwordConfirm;

    // PasswordValidator 유틸로 검증
    @AssertTrue(message = "잘못된 비밀번호 형식입니다.")
    public boolean isPasswordPolicySatisfied() {
        // PasswordValidator는 별도 유틸 고정 구현으로 가정
        // null 대응: @NotBlank에서 이미 차단되지만, 혹시 모를 NPE 방지
        if (password == null) return false;
        return me.swudam.jangbo.util.PasswordValidator.isValid(password);
    }
}
