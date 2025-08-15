package me.swudam.jangbo.util;

import java.util.regex.Pattern;

/**
 * 비밀번호 정책 검증 유틸리티
 * ---------------------------------------------------------
 * [정책]
 *  - 길이: 8~16자
 *  - 허용 문자: 영문 대/소문자, 숫자, 특정 32개 특수문자(₩ 포함)
 *  - 특수문자: 최소 1개 필수
 */
public class PasswordValidator {
    // 문자 클래스에서 안전하게 쓸 수 있도록 직접 이스케이프
    private static final String SPECIALS_CLASS = "!#$%&*@\\^";

    private static final String PASSWORD_PATTERN =
            "^(?=.*[" + SPECIALS_CLASS + "])[A-Za-z0-9" + SPECIALS_CLASS + "]{8,16}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    private PasswordValidator() {}

    public static boolean isValid(String password) {
        return password != null && pattern.matcher(password).matches();
    }
}
