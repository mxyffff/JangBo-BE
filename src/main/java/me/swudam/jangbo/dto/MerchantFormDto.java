package me.swudam.jangbo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import me.swudam.jangbo.entity.Category;
import org.hibernate.validator.constraints.Length;

// [온보딩] 상인
@Getter
@Setter
public class MerchantFormDto {
    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String username;

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식으로 입력해주세요.") // 이메일 형식에 맞는지 확인
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Length(min = 8, max = 16, message = "비밀번호는 8~16자로 입력해주세요.")
    @Pattern(
            regexp = "^(?=.*[!\"#$%&'()*+,-./:;<=>?@\\[\\]₩^_`{|}~])[A-Za-z0-9!\"#$%&'()*+,-./:;<=>?@\\[\\]₩^_`{|}~]{8,16}$",
            message = "비밀번호는 8~16자의 영문 대소문자, 숫자, 특수문자 조합이어야 하며, 특수문자는 최소 1자 이상 포함해야 합니다."
    )
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 입력 값입니다.")
    private String passwordConfirm;

    @NotBlank(message = "전화번호는 필수 입력 값입니다.")
    private String phoneNumber;

    @NotBlank(message = "사업자등록번호는 필수 입력 값입니다.")
    private String businessNumber;
}
