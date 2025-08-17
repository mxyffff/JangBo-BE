package me.swudam.jangbo.config;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.filter.StoreRegistrationAuthenticationFilter;
import me.swudam.jangbo.security.MerchantUserDetailsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// 상인 전용 Spring Security Config 설정
// Order(1)로 먼저 적용
@Configuration
@RequiredArgsConstructor
public class MerchantSecurityConfig {

    // 상인 계정 정보를 로드하는 서비스
    private final MerchantUserDetailsService merchantUserDetailsService;
    // 비밀번호 암호화
    private final PasswordEncoder passwordEncoder;

    @Qualifier("merchantDaoAuthProvider")
    private final AuthenticationProvider merchantDaoAuthProvider;

    // 상인 API 전용 Security Filter Chain
    @Bean
    @Order(1) // 먼저 매칭되도록
    public SecurityFilterChain filterChain(HttpSecurity http, me.swudam.jangbo.filter.StoreRegistrationAuthenticationFilter storeRegistrationAuthenticationFilter) throws Exception {
        http
                .securityMatcher("/api/merchants/**", "/api/stores/**")
                .authenticationProvider(merchantDaoAuthProvider)

                .formLogin(form -> form.disable()) // HTML 기반 로그인 완전히 비활성화
                .httpBasic(basic -> basic.disable()) // Basic Auth도 사용 안 함
                .csrf(csrf -> csrf.disable()) // API는 CSRF 토큰 X
                .cors(Customizer.withDefaults()) // React 연동 대비

                // 로그인 시 세션이 필요하면 만들고, 필요 없으면 만들지 않음 (REQUIRED)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                .authorizeHttpRequests(requests -> requests
                        // 회원가입/중복체크/로그인/로그아웃 API 허용
                        .requestMatchers(HttpMethod.POST, "/api/merchants/signup").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/merchants/exists/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/merchants/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/merchants/logout").permitAll()

                        // 상점 조회 API는 누구나 가능
                        .requestMatchers(HttpMethod.GET, "/api/stores/**").permitAll()

                        // 상점 등록, 로그인 여부 조회, 상점 수정, 상점 삭제 API는 인증 필요
                        .requestMatchers(HttpMethod.POST, "/api/stores").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/merchants/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/stores/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/stores/**").authenticated()

                        // 상인 전용 상품 API: 상인만 접근
                        .requestMatchers("/api/merchants/products/**").authenticated()

                        // 그 외 API는 인증
                        .anyRequest().authenticated()
                )
                // 인증/권한 예외 핸들링
                .exceptionHandling(error -> error
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint()) // 401 인증 실패 JSON 응답
                        .accessDeniedHandler(new CustomAccessDeniedHandler()) // 403 JSON 응답
                );
                // 상점 등록 세션 Postman API 테스트용
                //.addFilterBefore(storeRegistrationAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        // dev 환경에서만 필터 추가
        if ("dev".equals(System.getProperty("spring.profiles.active"))) {
            http.addFilterBefore(new StoreRegistrationAuthenticationFilter(),
                    UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }

}