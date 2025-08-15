package me.swudam.jangbo.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.MerchantFormDto;
import me.swudam.jangbo.dto.MerchantUpdateDto;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.service.MerchantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;

// [온보딩] 상인
@RequestMapping("api/merchants")
@RestController
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService; // 상인 관련 비즈니스 로직
    private final PasswordEncoder passwordEncoder; // 비밀번호 암호화 / 검증
    private final AuthenticationManager authenticationManager; // 로그인 인증 처리

    /* 1. 회원가입 API */
    // 상인 회원가입 API
    // POST - /api/merchants/signup
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody MerchantFormDto dto, HttpSession session) {
        // 비밀번호 확인
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            return ResponseEntity.badRequest().body(Map.of("created", false, "message", "비밀번호가 일치하지 않습니다."));
        }

        // DTO → Entity 변환 & 비밀번호 암호화
        Merchant merchant = Merchant.createMerchant(dto, passwordEncoder);
        merchantService.saveMerchant(merchant);

        // 세션에 1회성 가입 정보 저장
        session.setAttribute("justRegisteredMerchant", true);
        session.setAttribute("justRegisteredMerchantEmail", merchant.getEmail());

        // 201 Created
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "created", true,
                        "merchantId", merchant.getId(), "email", merchant.getEmail()
                ));
    }

    /* 2. 이메일 중복 확인 */
    @GetMapping("/exists/email")
    public ResponseEntity<?> existsEmail(@RequestParam String value) {
        boolean exists = merchantService.existsEmail(value); // 이메일 존재 여부
        return ResponseEntity.ok(Map.of(
                "exists", exists));
    }

    /* 3. 로그인 API */
    // 상인 로그인 API
    // POST - /api/merchants/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpSession session) {
        String email = request.get("email");
        String password = request.get("password");

        try {
            // Spring Security 인증
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            // SecurityContext에 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());
            session.setAttribute(
                    "merchantEmail", email); // 일반 세션에도 이메일 저장

            return ResponseEntity.ok(Map.of(
                    "loggedIn", true,
                    "email", email)
            );
        } catch (AuthenticationException e) {
            // 인증 실패 401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "loggedIn", false,
                            "message", "이메일 또는 비밀번호가 잘못되었습니다.")
                    );
        }
    }

    /* 4. 로그아웃 API */
    // 상인 로그아웃 API
    @PostMapping("/logout")
    // POST - /api/merchants/logout
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate(); // 세션 초기화
        SecurityContextHolder.clearContext(); // Spring Security 인증 정보 초기화
        return ResponseEntity.ok(Map.of(
                "loggedOut", true,
                "message", "로그아웃 성공")
        );
    }

    /* 5. 현재 로그인 여부 / 상인 정보 */
    // GET - /api/merchants/me
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        // 1. 헬퍼로 이메일만 가져오기
        String email = getAuthenticatedMerchantEmail(session);
        if (email == null) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", false,
                    "message", "로그인이 필요합니다."
            ));
        }
        // 2. Service 통해 Merchant 조회
        Merchant merchant = merchantService.getMerchantByEmail(email);
        if (merchant == null) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", false,
                    "message", "회원 정보를 찾을 수 없습니다."
            ));
        }
        // 3. 응답 구성
        Map<String, Object> body = new HashMap<>();
        body.put("authenticated", true);
        body.put("email", email);
        body.put("merchantId", merchant.getId());
        body.put("username", merchant.getUsername());

        return ResponseEntity.ok(body);
    }

    /* 6. 개인정보 수정 */
    // PUT - /api/merchants/me
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody MerchantUpdateDto dto, HttpSession session) {
        String email = getAuthenticatedMerchantEmail(session);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "updated", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        try {
            merchantService.updateMerchant(email, dto); // 이메일 기반 호출
            return ResponseEntity.ok(Map.of(
                    "updated", true,
                    "message", "정보가 수정되었습니다."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "updated", false,
                    "message", e.getMessage()
            ));
        }
    }

    /* 7. 회원탈퇴 */
    // DELETE - /api/merchants/me
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMe(HttpSession session) {
        String email = getAuthenticatedMerchantEmail(session);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "deleted", false,
                    "message", "로그인이 필요합니다."
            ));
        }
        merchantService.deleteMerchant(email); // 비밀번호 입력 없이 탈퇴
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // Helper: 현재 로그인한 상인 이메일 가져오기
    private String getAuthenticatedMerchantEmail(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = null;

        if (auth != null && auth.isAuthenticated() && !isAnonymous(auth)) {
            Object principal = auth.getPrincipal();
            email = (principal instanceof UserDetails userDetails) ? userDetails.getUsername() : principal.toString();
        }

        if (email == null) email = (String) session.getAttribute("merchantEmail");

        if (email == null) {
            Boolean justRegistered = (Boolean) session.getAttribute("justRegisteredMerchant");
            if (Boolean.TRUE.equals(justRegistered)) email = (String) session.getAttribute("justRegisteredMerchantEmail");
        }

        return email; // null이면 인증 실패
    }

    private boolean isAnonymous(Authentication auth) {
        String name = auth.getName();
        return name == null || "anonymousUser".equals(name);
    }
}