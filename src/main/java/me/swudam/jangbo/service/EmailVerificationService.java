package me.swudam.jangbo.service;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.repository.CustomerRepository;
import me.swudam.jangbo.repository.MerchantRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    // 문자열 전용 Redis 템플릿
    private final StringRedisTemplate redisTemplate;

    // 메일 발송기 (Gmail SMTP 설정은 application.properties에 구성)
    private final JavaMailSender mailSender;

    // 가입 여부 중복 체크를 위해 사용
    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository; // 추가

    /* Redis 키 네이밍 컨벤션 */
    // 인증코드 저장 키
    private static final String KEY_CODE_PREFIX = "email:code:";
    // 인증완료 플래그 키
    private static final String KEY_VERIFIED_PREFIX = "email:verified:";

    /* TTL (만료시간) 정책 */
    // 인증코드 유효시간 30분으로 설정
    private static final Duration CODE_TTL = Duration.ofMinutes(30);
    // 인증완료 플래그 유효시간 24시간으로 설정
    private static final Duration VERIFIED_TTL = Duration.ofHours(24);

    /* 퍼블릭 API */
    // 인증코드 요청
    // 1. 이미 가입된 이메일이면 거절
    // 2. 6자리 코드 생성 ("000000" ~ "999999")
    // 3. Redis에 (email->code) 저장 + TTL(30분) 설정
    // 4. 메일 발송
    // 5. [개발 단계 전용] devCode 반호나
    public String requestCode(String rawEmail) {
        final String email = normalizeEmail(rawEmail);

        // 1. 이미 가입된 이메일인지 체크 -> 고객/상인 둘 다 중복 차단
        if (customerRepository.existsByEmail(email) || merchantRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 2. 6자리 코드 생성
        String code = generate6DigitCode();

        // 3. Redis에 코드 저장 (TTL=30분). 같은 이메일로 재요청시 최신 코드로 덮어씀
        String codeKey = KEY_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(codeKey, code, CODE_TTL);

        //  4. 이메일 발송 (간단 텍스트) -> 필요시 HTML 템플릿으로 개선 가능
        sendMail(
                email,
                "[장보는친구] 이메일 인증코드",
                "아래 6자리 코드를 인증 화면에 입력해주세요.\n\n" + code + "\n\n" + "유효시간: 30분"
        );

        // 5. 개발 단계에서는 프론트 테스트 편의를 위해 코드 반환
        return code;
    }

    // 인증코드 검증
    // 1. Redis에서 저장된 코드 조회
    // 2. 코드 일치 확인 -> 인증완료 플래그(24시간) 저장
    // 3. 재사용 방지를 위해 기존 코드(key)는 즉시 삭제
    public void verifyCode(String rawEmail, String rawCode) {
        final String email = normalizeEmail(rawEmail);
        final String input = rawCode == null ? "" : rawCode.trim();

        String codeKey = KEY_CODE_PREFIX + email;
        String savedCode = redisTemplate.opsForValue().get(codeKey);

        if (savedCode == null) {
            // 저장된 코드가 없거나 TTL 만료
            throw new IllegalArgumentException("인증코드가 만료되었거나 존재하지 않습니다.");
        }
        if (!savedCode.equals(input)) {
            // 코드 불일치
            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다.");
        }

        // 코드 일치 -> 인증완료 플래그 저장(24시간 유지)
        String verifiedKey = KEY_VERIFIED_PREFIX + email;
        redisTemplate.opsForValue().set(verifiedKey, "true", VERIFIED_TTL);

        // 기존 코드 삭제 (재사용 방지)
        redisTemplate.delete(codeKey);
    }

    // 가입 직전에 호출하여 "이메일 인증이 완료되었는지" 확인
    // return true => 인증 완료 상태
    public boolean isVerified(String rawEmail) {
        final String email = normalizeEmail(rawEmail);
        String verifiedKey = KEY_VERIFIED_PREFIX + email;
        return "true".equals(redisTemplate.opsForValue().get(verifiedKey));
    }

    // 가입이 성공적으로 끝났을 경우 인증완료 플래그를 정리
    // (운영 정책에 따라 남겨둘 수도 있지만 보통은 가입 완료 후 정리하여 깔끔하게 유지한다)
    public void clearVerified(String rawEmail) {
        final String email = normalizeEmail(rawEmail);
        redisTemplate.delete(KEY_VERIFIED_PREFIX + email);
    }

    /* 내부 유틸 */
    // 이메일 표준화: 앞뒤 공백 제거 + 소문자 변환
    private String normalizeEmail(String email) {
        if (email == null) throw new IllegalArgumentException("이메일이 비어있습니다.");
        return email.trim().toLowerCase();
    }

    // 6자리 숫자 코드 생성: 00000 ~ 999999
    private String generate6DigitCode() {
        int num = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("%06d", num);
    }

    // 간단 텍스트 메일 발송 (JavaMailSender)
    // HTML 본문/템플릿이 필요하면 MimeMessageHelper로 확장 가능
    private void sendMail(String to, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.send(msg);
    }
}
