package me.swudam.jangbo.dto;

import lombok.Getter;
import lombok.Setter;

// 상인 회원 개인정보 수정용 DTO
// 수정용 DTO: PUT /me
@Getter @Setter
public class MerchantUpdateDto {
    private String username;          // 변경할 이름
    private String newPassword;       // 변경할 비밀번호
    private String newPasswordConfirm;// 변경할 비밀번호 재입력
}
