package me.swudam.jangbo.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.MerchantFormDto;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.repository.MerchantRepository;
import me.swudam.jangbo.service.MerchantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.BadCredentialsException;

// [온보딩] 상인
@RequestMapping("api/merchants")
@RestController
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService; // 상인 관련 로직
    private final PasswordEncoder passwordEncoder; // 비밀번호 암호화/검증
    private final AuthenticationManager authenticationManager; // 로그인 인증 처리
    private final MerchantRepository merchantRepository;

    /* 1. 회원가입 API */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody MerchantFormDto dto, HttpSession session) {

        // 1-1. 비밀번호 & 비밀번호 확인 검증
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "created", false,
                    "message", "비밀번호가 일치하지 않습니다."
            ));
        }

        try {
            // 1-2. 회원가입 처리 (DTO → Entity + DB 저장)
            Merchant merchant = merchantService.registerMerchant(dto);

            // 1-3. 세션에 가입 직후 상태 저장
            session.setAttribute("justRegisteredMerchant", true);
            session.setAttribute("justRegisteredMerchantEmail", merchant.getEmail());

            // 1-4. 응답 데이터 구성
            Map<String, Object> body = new HashMap<>();
            body.put("created", true);
            body.put("merchantId", merchant.getId());
            body.put("email", merchant.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(body);

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "created", false,
                    "message", e.getMessage()
            ));
        }
    }

    /* 2. 이메일 중복 체크 */
    @GetMapping("/exists/email")
    public ResponseEntity<?> existsEmail(@RequestParam("value") String email) {
        boolean exists = merchantService.existsEmail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /* 3. 로그인 API */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpSession session) {
        String email = request.get("email");
        String password = request.get("password");

        try {
            // 3-1. 인증 처리
            merchantService.authenticate(email, password);

            // 3-2. 세션에 SecurityContext 저장
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );

            // 3-3. Postman 테스트용 이메일 저장
            session.setAttribute("merchantEmail", email);

            return ResponseEntity.ok(Map.of(
                    "loggedIn", true,
                    "email", email
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "loggedIn", false,
                    "message", e.getMessage()
            ));
        }
    }

    /* 4. 로그아웃 API */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate(); // 4-1. 세션 초기화
        SecurityContextHolder.clearContext(); // 4-2. 시큐리티 인증 정보도 초기화
        return ResponseEntity.ok(Map.of(
                "loggedOut", true,
                "message", "로그아웃 성공"
        ));
    }

    /* 5. 현재 로그인 여부 / 상인 정보 */
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {

        // Service에서 이메일 확인 + DB 조회까지 위임
        Merchant merchant = merchantService.getAuthenticatedMerchant(session);

        if (merchant == null) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("authenticated", true);
        body.put("email", merchant.getEmail());
        body.put("merchantId", merchant.getId());
        body.put("username", merchant.getUsername());

        return ResponseEntity.ok(body);
    }

    private boolean isAnonymous(Authentication auth) {
        String name = auth.getName();
        return name == null || "anonymousUser".equals(name);
    }
}