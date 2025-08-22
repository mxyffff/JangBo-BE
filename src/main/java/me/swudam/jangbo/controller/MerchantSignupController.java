package me.swudam.jangbo.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.MerchantSignupRequestDto;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.service.MerchantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import java.util.List;

import java.util.Map;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantSignupController {

    private final MerchantService merchantService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody MerchantSignupRequestDto dto, HttpSession session) {
        // 실제 가입 로직은 서비스 계층에 위임
        Merchant saved = merchantService.signup(dto);

        // (기존 흐름 유지) 가입 직후 1회성 세션 플래그
        session.setAttribute("justRegisteredMerchant", true);
        session.setAttribute("justRegisteredMerchantEmail", saved.getEmail());

        // 자동 로그인 처리 - 0822
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        saved.getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))
                );
        SecurityContextHolder.getContext().setAuthentication(auth);

        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("created", true,
                        "merchantId", saved.getId(),
                        "email", saved.getEmail()));
    }
}
