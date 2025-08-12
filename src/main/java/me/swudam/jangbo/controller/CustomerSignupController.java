package me.swudam.jangbo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.CustomerSignupRequestDto;
import me.swudam.jangbo.entity.Customer;
import me.swudam.jangbo.service.CustomerService;
import me.swudam.jangbo.service.EmailVerificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerSignupController {

    private final CustomerService customerService; // 비즈니스 로직

    /* 회원가입 엔드포인트 */
    // Method: POST
    // URL: /api/customers/signup
    // BODY: CustomerSignupRequestDto (JSON)

    // 1. @Valid로 1차 입력 검증
    // 2. CustomerService.signup(dto)로 가입 처리 위임
    // 3. 생성된 고객 id와 email을 201 Created로 반환

    // 응답 형식(성공): { "created": true, "customerId": 1, "email": "user@example.com" }
    // 응답 형식(실패): { "created": false, "message": "에러 메시지" }
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody CustomerSignupRequestDto requestDto) {
        // 실제 가입 로직은 서비스 계층에 위임
        Customer saved = customerService.signup(requestDto);

        // 성공 응답 바디 구성 (필요시 더 많은 필드 내려주기 가능)
        Map<String, Object> body = new HashMap<>();
        body.put("created", true);
        body.put("customerId", saved.getId());
        body.put("email", requestDto.getEmail());

        // REST 관례상 성공은 201 Created
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /* 닉네임 중복 체크 API */
    // 프론트에서 실시간 중복확인을 하고 싶을 때 사용
    // GET /api/customers/exists/username?value=닉네임
    // 응답 예시: { "exists": true/false }
    @GetMapping("/exists/username")
    public ResponseEntity<?> existsUsername(@RequestParam("value") String username) {
        boolean exists = customerService.existsUsername(username);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /* 이메일 중복 체크 API */
    // GET /api/customers/exists/email?value=이메일
    @GetMapping("/exists/email")
    public ResponseEntity<?> existsEmail(@RequestParam("value") String email) {
        boolean exists = customerService.existsEmail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /* 전화번호 중복 체크 API */
    // GET /api/customers/exists/phone?value=전화번호
    @GetMapping("/exists/phone")
    public ResponseEntity<?> existsPhone(@RequestParam("value") String phoneNumber) {
        boolean exists = customerService.existsPhoneNumber(phoneNumber);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /* 서비스에서 던지는 IllegalArgumentException을 400 응답으로 매핑 */
    // 응답 형식(실패): { "created": false, "message": "에러 메시지" }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        // 이메일 미인증 시 가입 불가 -> 400 BAD_REQUEST와 함께 메시지 반환
        Map<String, Object> body = new HashMap<>();
        body.put("created", false);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
