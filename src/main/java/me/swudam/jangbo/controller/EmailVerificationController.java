package me.swudam.jangbo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.EmailRequestDto;
import me.swudam.jangbo.dto.EmailVerifyDto;
import me.swudam.jangbo.service.EmailVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/customers/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    /* 인증 코드 발송 요청 */
    // 1. DTO(@Valid)로 이메일 형식 1차 검증
    // 2. 서비스에 위임: 6자리 코드 생성 -> Redis에 저장 -> 메일 발송
    // 3. 개발 단계에서는 devCode를 응답에 포함하여 프론트 테스트 편의 제공
    // 파라미터 dto: EmailRequestDto
    // return: { "sent": true, "devCode": "123456" } -> 추후 devCdoe는 제거
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> request(@RequestBody @Valid EmailRequestDto dto) {

        String devCode = emailVerificationService.requestCode(dto.getEmail());

        // 개발 단계 응답: devCode 포함
        // 운영 단계 전환 시  Map.of("sent", true)로 변경
        return ResponseEntity.ok(Map.of(
                "sent", true,
                "devCode", devCode
        ));
    }

    /* 인증 코드 검증 */
    // 1. DTO(@Valid)로 이메일/코드 형식을 검증
    // 2. 서비스에 위임: Redis에서 코드 조회 -> 일치 시 "verified 플래그" 저장
    // 3. 사용한 코드는 즉시 삭제 (재사용 방지)
    // 파라미터 dto: EmailVerifyDto
    // return: { "verified": true }
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody @Valid EmailVerifyDto dto) {
        emailVerificationService.verifyCode(dto.getEmail(), dto.getCode());

        return ResponseEntity.ok(Map.of("verified", true));
    }
}
