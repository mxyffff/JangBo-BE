package me.swudam.jangbo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import me.swudam.jangbo.entity.Category;
import org.hibernate.validator.constraints.Length;

// 상인-2 - [온보딩] 상인 상점 등록
@Getter
@Setter
public class MerchantFormDto {
    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String username;

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식으로 입력해주세요.") // 이메일 형식에 맞는지 확인
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Length(min=8, message = "비밀번호는 8자 이상으로 입력해주세요.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[!\"#$%&'()*+,-./:;<=>?@\\[\\]₩^_`{|}~])[a-z!\"#$%&'()*+,-./:;<=>?@\\[\\]₩^_`{|}~]+$",
            message = "비밀번호는 영어 소문자와 특수문자만 포함할 수 있습니다."
    )
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 입력 값입니다.")
    private String passwordConfirm;

    @NotBlank(message = "전화번호는 필수 입력 값입니다.")
    private String phoneNumber;

    @NotBlank(message = "상점 이름은 필수 입력 값입니다.")
    private String storeName;

    @NotBlank(message = "상점 위치는 필수 입력 값입니다.")
    private String storeAddress;

    @NotBlank(message = "사업자등록번호는 필수 입력 값입니다.")
    private String businessNumber;

    @NotNull(message = "카테고리는 한 가지를 필수 선택해야 합니다.")
    private Category category; // 단일 선택
}
