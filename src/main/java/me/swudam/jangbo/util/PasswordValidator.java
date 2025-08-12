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
    // 사용 가능한 특수문자 32자
    private static final String ALLOWED_SPECIALS = "!\"#$%&'()*+,-./:;<=>?@[₩]^_`{|}~";

    // 정규식에서 안전하게 쓰기 위해 이스케이프 처리
    private static final String ESCAPED_SPECIALS = Pattern.quote(ALLOWED_SPECIALS);

    // 최종 패턴
    private static final String PASSWORD_PATTERN =
            "^(?=.*[" + ESCAPED_SPECIALS + "])[A-Za-z0-9" + ESCAPED_SPECIALS + "]{8,16}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    // 인스턴스화 방지
    private PasswordValidator() {}

    // 비밀번호 검증
    public static boolean isValid(String password) {
        return password != null && pattern.matcher(password).matches();
    }
}
