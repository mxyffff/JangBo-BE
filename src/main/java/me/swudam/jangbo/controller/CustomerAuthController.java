package me.swudam.jangbo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.CustomerLoginRequestDto;
import me.swudam.jangbo.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerAuthController {

    // 스프링 시큐리티 인증 진입점
    // SecurityBeansConfig에서 노출한 AuthenticationManager 빈을 주입받음
    private final AuthenticationManager authenticationManager;
    private final CustomerRepository customerRepository; // 로그인 성공 후 고객 정보 내려줄 때 사용

    /* 로그인 */
    // - POST /api/customers/login
    // - BODY(JSON): { "email": "user@example.com", "password": "..." }
    // 응답 성공 200: { "authenticated": true, "email": "user@example.com" }

    // 1. 기본 검증 후 UsernamePasswordAuthenticationToken을 만들어 AuthenticationManager에 위임
    // 2. 인증 성공 시 SecurityContext에 저장 + HttpSession 생성(JSESSIONID)
    // 3. 응답으로 최소한의 고객 정보(식별자, 이메일, 닉네임)을 내려줌

    // 주의: 이 엔드포인트는 CSRF 면제 대상이어야 프론트에서 편하게 호출 가능
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody CustomerLoginRequestDto requestDto,
                                   HttpServletRequest httpServletRequest) {


        // 1. 이메일을 username으로, 평문 비밀번호를 credentials로 사용해 토큰 생성
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(
                        requestDto.getEmail().trim().toLowerCase(),
                        requestDto.getPassword()
                );

        // 2. AuthenticationManager가 실제 인증 수행 (UserDetailsService + PasswordEndcoder 사용)
        Authentication authentication = authenticationManager.authenticate(authRequest);

        // 세션 고정 공격 방지: 기존 세션 무효화 후 새 세션 발급
        HttpSession old = httpServletRequest.getSession(false);
        if (old != null) old.invalidate();
        HttpSession newSession = httpServletRequest.getSession(true); // 새 세션

        // 3. 인증 성공 -> SecurityContext 구성/저장
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // 4. 세션에 SecurityContext 저장하여 상태 유지
        newSession.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context
        );

        // 5. 응답 바디 구성
        Map<String, Object> body = new HashMap<>();
        body.put("authenticated", true);
        body.put("email", ((UserDetails) authentication.getPrincipal()).getUsername());

        return ResponseEntity.ok(body);
    }

    /* 로그아웃 */
    // POST /api/customers/logout
    // 응답(항상 200): { "loggedOut": true }

    // 서버 세션을 무효화하고 SecurityContext를 비움
    // 클라이언트는 Set-Cookie 헤더로 세션 쿠키가 사라지는지/갱신되는지 확인
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        // 1. SecurityContext 비우기
        SecurityContextHolder.clearContext();

        // 2. 기존 세션이 있다면 무효화
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(Map.of("loggedOut", true));
    }

    /* 현재 로그인 상태 확인 */
    // GET /api/customers/me
    // 응답(로그인 O, 200): { "authenticated": true, "email": "user@example.com" }
    // 응답(로그인 X, 200): { "authenticated": false }
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || isAnonymous(auth)) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        Object principal = auth.getPrincipal();
        String email = (principal instanceof UserDetails ud) ? ud.getUsername() : principal.toString();

        Map<String, Object> body = new HashMap<>();
        body.put("authenticated", true);
        body.put("email", email);

        return ResponseEntity.ok(body);
    }

    // 익명 인증(AnonymousAuthenticationToken) 여부 간단 판별
    private boolean isAnonymous(Authentication auth) {
        return auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;
    }
}
