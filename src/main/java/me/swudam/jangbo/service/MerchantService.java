package me.swudam.jangbo.service;

import jakarta.servlet.http.HttpSession;
import me.swudam.jangbo.dto.MerchantFormDto;
import me.swudam.jangbo.entity.Merchant;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.repository.MerchantRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;

import java.util.List;

// [온보딩] 상인
@Service
@Transactional
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder; // 비밀번호 암호화
    private final AuthenticationManager authenticationManager; // 로그인 인증 처리

    // 스프링 시큐리티 로그인 기능
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Merchant merchant = merchantRepository.findByEmail(email);

        // 상인 유저가 존재하지 않는다면 UsernameNotFoundException 처리
        if (merchant == null) {
            throw new UsernameNotFoundException(email);
        }

        // UserDetails를 구현하고 있는 User 객체 반환
        return User.builder()
                .username(merchant.getEmail()) // 로그인 ID로 이메일 사용
                .password(merchant.getPassword()) // 암호화된 비밀번호
                .authorities(List.of()) // 별도의 권한 없이 빈 리스트만 할당
                .build();
    }

    // 회원 저장 (회원가입)
    public Merchant saveMerchant(Merchant merchant) {
        if (existsEmail(merchant.getEmail())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }
        return merchantRepository.save(merchant);
    }

    // DTO → Entity 변환 + 비밀번호 암호화 후 저장
    public Merchant registerMerchant(MerchantFormDto dto) {
        Merchant merchant = Merchant.createMerchant(dto, passwordEncoder);
        return saveMerchant(merchant);
    }

    // 이메일 중복 여부 확인
    @Transactional(readOnly = true)
    public boolean existsEmail(String email) {
        return merchantRepository.existsByEmail(email);
    }

    // 로그인 인증 처리
    public void authenticate(String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            // 인증 성공 시 SecurityContext에 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 잘못되었습니다.");
        }
    }

    // 이메일로 상인 조회
    @Transactional(readOnly = true)
    public Merchant findByEmail(String email) {
        return merchantRepository.findByEmail(email);
    }

    // 로그인 정보 조회용 (me)
    @Transactional(readOnly = true)
    public Merchant getAuthenticatedMerchant(HttpSession session) {
        // 1. SecurityContext에서 이메일 조회
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = null;

        if (auth != null && auth.isAuthenticated() && !isAnonymous(auth)) {
            Object principal = auth.getPrincipal();
            email = (principal instanceof UserDetails userDetails)
                    ? userDetails.getUsername()
                    : principal.toString();
        }

        // 2. SecurityContext가 비어있으면 세션 확인
        if (email == null) {
            email = (String) session.getAttribute("merchantEmail");
        }

        // 3. 1회성 가입 직후 세션 확인
        if (email == null) {
            Boolean justRegistered = (Boolean) session.getAttribute("justRegisteredMerchant");
            if (Boolean.TRUE.equals(justRegistered)) {
                email = (String) session.getAttribute("justRegisteredMerchantEmail");
            }
        }

        // 4. 이메일 없으면 인증 실패
        if (email == null) return null;

        // 5. DB 조회
        return findByEmail(email);
    }

    private boolean isAnonymous(Authentication auth) {
        String name = auth.getName();
        return name == null || "anonymousUser".equals(name);
    }
}

