package me.swudam.jangbo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.MerchantUpdateDto;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.repository.MerchantRepository;
import me.swudam.jangbo.service.EmailVerificationService;
import me.swudam.jangbo.service.MerchantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// [온보딩] 상인
// 실패 응답은 전부 GlobalExceptionHandler가 통일 포맷(JSON)으로 처리하도록 예외를 던지는 방식 채택
@RequestMapping("/api/merchants")
@RestController
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService; // 상인 관련 비즈니스 로직
    private final AuthenticationManager authenticationManager; // 비밀번호 암호화 / 검증
    private final MerchantRepository merchantRepository; // 로그인 인증 처리
    private final EmailVerificationService emailVerificationService; // 추가

    /* 1. 회원가입 */
    // 상인 회원가입 API
    // POST - /api/merchants/signup
    // 기존 코드 삭제 -> MerchantSignupController 생성

    /* 3. 로그인 */
    // 상인 로그인 API
    // POST - /api/merchants/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String rawEmail = request.get("email");
        String password = request.get("password");
        if (rawEmail == null || rawEmail.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("이메일과 비밀번호는 필수입니다.");
        }
        String email = rawEmail.trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        // 세션 고정 방지
        HttpSession old = httpRequest.getSession(false);
        if (old != null) old.invalidate();
        HttpSession newSession = httpRequest.getSession(true);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        newSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        // 일반 세션에도 이메일 저장
        newSession.setAttribute("merchantEmail", ((UserDetails) authentication.getPrincipal()).getUsername());

        // storeId 조회 0824
        Merchant merchant = merchantRepository.findByEmail(email);
        Long storeId = null;
        if (merchant != null && !merchant.getStores().isEmpty()) {
            storeId = merchant.getStores().get(0).getId(); // 유일한 상점
        }

        return ResponseEntity.ok(Map.of(
                "loggedIn", true,
                "email", ((UserDetails) authentication.getPrincipal()).getUsername(),
                "storeId", storeId
        ));
    }

    /* 4. 로그아웃 */
    // 상인 로그아웃 API
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        SecurityContextHolder.clearContext();
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("loggedOut", true, "message", "로그아웃 성공"));
    }

    /* 5. 현재 로그인 여부 / 상인 정보 */
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        // 1. 헬퍼로 이메일만 가져오기
        String email = getAuthenticatedMerchantEmail(session);
        if (email == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        // 2. Service 통해 Merchant 조회
        Merchant merchant = merchantRepository.findByEmail(email);
        // 3. 응답 구성
        Map<String, Object> body = new HashMap<>();
        body.put("authenticated", true);
        body.put("email", email);
        if (merchant != null) {
            body.put("merchantId", merchant.getId());
            body.put("username", merchant.getUsername());
        }
        return ResponseEntity.ok(body);
    }

    /* 6. 개인정보 수정 */
    // PUT - /api/merchants/me
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody MerchantUpdateDto dto, HttpSession session) {
        String email = getAuthenticatedMerchantEmail(session);
        if (email == null) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }
        merchantService.updateMerchant(email, dto); // 이메일 기반 호출
        return ResponseEntity.ok(Map.of("updated", true, "message", "정보가 수정되었습니다."));
    }

    /* 7. 회원탈퇴 */
    // DELETE - /api/merchants/me
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMe(HttpSession session) {
        String email = getAuthenticatedMerchantEmail(session);
        if (email == null) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }
        merchantService.deleteMerchant(email); // 비밀번호 입력 없이 탈퇴
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    /* 8. 이름만 반환 */
    // GET - /api/merchants/me/username
    @GetMapping("/me/username")
    public ResponseEntity<?> getUsername(HttpSession session) {
        String email = getAuthenticatedMerchantEmail(session);
        if (email == null) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }
        String username = merchantService.getMerchantByEmail(email).getUsername();

        return ResponseEntity.ok(Map.of(
                "username", username
        ));
    }

    // Helper: 현재 로그인한 상인 이메일 가져오기
    private String getAuthenticatedMerchantEmail(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = null;
        if (auth != null && auth.isAuthenticated() && !isAnonymous(auth)) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails ud) email = ud.getUsername();
            else if (principal instanceof String s) email = s;
        }
        if (email == null) email = (String) session.getAttribute("merchantEmail");
        if (email == null) {
            Boolean justRegistered = (Boolean) session.getAttribute("justRegisteredMerchant");
            if (Boolean.TRUE.equals(justRegistered)) {
                email = (String) session.getAttribute("justRegisteredMerchantEmail");
            }
        }
        return email;  // null이면 인증 실패
    }

    private boolean isAnonymous(Authentication auth) {
        String name = auth.getName();
        return name == null || "anonymousUser".equals(name);
    }
}
