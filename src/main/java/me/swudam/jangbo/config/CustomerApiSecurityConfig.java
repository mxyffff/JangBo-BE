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
                .securityMatcher("/api/customers/**", "/api/products/**", "/api/carts/**", "/api/ai/**"
                , "/api/reviews/**", "api/public/**")
                .authenticationProvider(customerDaoAuthProvider)

                // REST API는 폼 로그인/로그인 페이지가 필요 없음
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // CORS (개발 중 프론트 로컬 허용, 운영 시 도메인으로 제한
                .cors(Customizer.withDefaults())

                // 개발/테스트 편의로 CSRF 비활성화 (운영 전 CookieCsrfTokenRepository로 전환 권장)
                .csrf(csrf -> csrf.disable())
                // 배포 전환 시:
                // .csrf(csrf -> csrf
                //     .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                //     .ignoringRequestMatchers(
                //         "/api/customers/email/**", "/api/customers/login", "/api/customers/logout", "/api/customers/signup"
                //     )
                // )

                .authorizeHttpRequests(req -> req
                        // 공개 엔드포인트
                        .requestMatchers(HttpMethod.POST, "/api/customers/signup").permitAll()
                        .requestMatchers("/api/customers/email/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/customers/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/customers/exists/username").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/ai/ingredients/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/ai/recommendations/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()

                        // 고객 상태 확인/로그아웃은 인증 필요
                        .requestMatchers("/api/customers/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/customers/logout").authenticated()
                        .requestMatchers("/api/carts/**").authenticated()
                        .requestMatchers("/api/ai/cart/bulk-add").authenticated()
                        .requestMatchers("/api/reviews/**").authenticated()

                        // 고객 공개 상품 조회(API)는 전체 공개 (ProductCustomerController)
                        .requestMatchers("/api/products/**").permitAll()

                        // 마이페이지 API는 인증 필요
                        .requestMatchers("/api/customers/me/**").authenticated()

                        // 그 외는 기본 막기 → 점진 개방
                        .anyRequest().denyAll()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                        .accessDeniedHandler(new CustomAccessDeniedHandler())
                );

        return http.build();
    }
}
