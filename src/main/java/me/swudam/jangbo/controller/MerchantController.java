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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

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
    // 상인 회원가입 API
    // POST - /api/merchants/signup
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody MerchantFormDto merchantFormDto, HttpSession httpSession) {

        // 1-1. 비밀번호 & 비밀번호 확인 검증
        if (!merchantFormDto.getPassword().equals(merchantFormDto.getPasswordConfirm())) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "created", false,
                            "message", "비밀번호가 일치하지 않습니다."
                    ));
        }
        try {
            // 1-2. DTO → Entity 변환 및 비밀번호 암호화 적용
            Merchant merchant = Merchant.createMerchant(merchantFormDto, passwordEncoder);

            // 1-3. DB 저장
            merchantService.saveMerchant(merchant);

            // 1-4. 세션에 가입 직후 상태를 저장
            httpSession.setAttribute("justRegisteredMerchant", true);
            httpSession.setAttribute("justRegisteredMerchantEmail", merchant.getEmail());

            // 1-5. 응답 데이터 구성
            Map<String, Object> body = new HashMap<>();
            body.put("created", true);
            body.put("merchantId", merchant.getId()); // DB에 저장된 ID
            body.put("email", merchant.getEmail()); // 가입한 이메일

            // 1-6. 201 Created 응답
            return ResponseEntity.status(HttpStatus.CREATED).body(body);

        } catch (IllegalStateException e) {
            // 1-7. 중복 회원 가입 등 예외 처리
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "created", false,
                            "message", e.getMessage()
                    ));
        }
    }

    // 2. 이메일 중복 체크
    // GET - /api/merchants/exists/email?value={email}
    @GetMapping("/exists/email")
    public ResponseEntity<?> existsEmail(@RequestParam("value") String email) {
        boolean exists = merchantService.existsEmail(email); // 중복 여부 확인
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // 3. 공통 예외 처리 핸들러
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "created", false,
                        "message", e.getMessage()
                ));
    }

    /* 4. 로그인 API */
    // 상인 로그인 API
    // POST - /api/merchants/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpSession session) {
        String email = request.get("email"); // 요청 바디에서 이메일 추출
        String password = request.get("password"); // 요청 바디에서 비밃번호 추출

        // 디버그 로그
        System.out.println("POST /login 요청 값:");
        System.out.println("입력 이메일: '" + email + "'");
        System.out.println("입력 비밀번호: '" + password + "'");

        // 멘토님께서 보시는 코드
//        try {
//            // 4-1. Spring Security 인증 시도
//            Authentication authentication = authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(email, password)
//            );
//
//            // 4-2. 인증 성공 시,
//            SecurityContextHolder.getContext().setAuthentication(authentication); // SecurityContext에 저장
//            session.setAttribute("merchantEmail", email); // 세션에 상인 이메일 저장
//
//            // 4-3. 성공 응답
//            return ResponseEntity.ok(Map.of(
//                    "loggedIn", true,
//                    "email", email
//            ));

        try {
            // Spring Security 인증 시도
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            // 인증 성공 시 SecurityContext에 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 세션에 SecurityContext 저장
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );

            // Postman 테스트용 이메일 저장
            session.setAttribute("merchantEmail", email);

            // 성공 응답
            return ResponseEntity.ok(Map.of(
                    "loggedIn", true,
                    "email", email
            ));

        } catch (BadCredentialsException e) {
            // 4-4. 이메일/비밀번호 불일치
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "loggedIn", false,
                    "message", "이메일 또는 비밀번호가 잘못되었습니다."
            ));
        } catch (AuthenticationException e) {
            // 4-5. 그 외 인증 실패
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "loggedIn", false,
                    "message", "로그인 실패"
            ));
        }
    }

    /* 5. 로그아웃 API */
    // 상인 로그아웃 API
    // POST - /api/merchants/logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate(); // 5-1. 세션 초기화
        SecurityContextHolder.clearContext(); // 5-2. 시큐리티 인증 정보도 초기화
        return ResponseEntity.ok(Map.of(
                "loggedOut", true,
                "message", "로그아웃 성공"
        ));
    }

    /* 6. 현재 로그인 여부 / 상인 정보 */
    // GET - /api/merchants/me
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = null;

        // 1. SecurityContext에서 인증 정보 가져오기
        if (auth != null && auth.isAuthenticated() && !isAnonymous(auth)) {
            Object principal = auth.getPrincipal();
            email = (principal instanceof UserDetails userDetails)
                    ? userDetails.getUsername()
                    : principal.toString();
        }

        // 2. SecurityContext가 비어있으면, 일반 로그인 세션 확인
        if (email == null) {
            email = (String) session.getAttribute("merchantEmail");
        }

        // 3. 그래도 없으면, 1회성 가입 직후 세션 확인
        if (email == null) {
            Boolean justRegistered = (Boolean) session.getAttribute("justRegisteredMerchant");
            if (Boolean.TRUE.equals(justRegistered)) {
                email = (String) session.getAttribute("justRegisteredMerchantEmail");
            }
        }

        // 4. 이메일 없으면 인증 실패
        if (email == null) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        // 5. DB에서 상인 정보 조회
        Merchant merchant = merchantRepository.findByEmail(email);
        Map<String, Object> body = new HashMap<>();
        body.put("authenticated", true);
        body.put("email", email);

        if (merchant != null) {
            body.put("merchantId", merchant.getId());
            body.put("username", merchant.getUsername());
        }

        return ResponseEntity.ok(body);
    }

    private boolean isAnonymous(Authentication auth) {
        String name = auth.getName();
        return name == null || "anonymousUser".equals(name);
    }
}
