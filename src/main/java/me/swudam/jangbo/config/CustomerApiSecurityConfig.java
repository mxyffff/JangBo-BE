package me.swudam.jangbo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class CustomerApiSecurityConfig {

    @Qualifier("customerDaoAuthProvider")
    private final AuthenticationProvider customerDaoAuthProvider;

    @Bean
    @Order(2) // merchant 체인 다음 우선순위
    public SecurityFilterChain customerApiFilterChain(
            HttpSecurity http
    ) throws Exception {
        http
                .securityMatcher("/api/**") // 이 체인은 /api/**에만 적용
                .authenticationProvider(customerDaoAuthProvider)

                // REST API는 폼 로그인/로그인 페이지가 필요 없음
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // CORS (개발 중 프론트 로컬 허용, 운영 시 도메인으로 제한
                .cors(Customizer.withDefaults())

                // 개발/테스트 편의로 CSRF 비활성화 (운영 전 CookieCsrfTokenRepository로 전환 권장)
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(req -> req
                        // 회원가입/로그인/이메일 인증 요청은 모두 허용
                        .requestMatchers(HttpMethod.POST, "/api/customers/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/customers/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/customers/logout").permitAll()
                        .requestMatchers("/api/customers/email/**").permitAll()

                        // 나머지 API는 우선 전부 허용하고, 이후 단계적으로 보호
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
