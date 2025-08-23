package me.swudam.jangbo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class OrderSecurityConfig {

    @Bean
    @Order(3) // Merchant, CustomerConfig보다 뒤에 적용
    public SecurityFilterChain orderFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/orders/**", "/api/merchants/orders/**", "/api/payments/**", "/api/public/**")
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(requests -> requests
                        // 고객 주문 API (세션 로그인 필요, ROLE_CUSTOMER 권한)
                        .requestMatchers("/api/orders/**").hasRole("CUSTOMER")
                        // 상인 주문 API (세션 로그인 필요, ROLE_MERCHANT 권한)
                        .requestMatchers("/api/merchants/orders/**").hasRole("MERCHANT")
                        // 결제 API
                        .requestMatchers("/api/payments/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/merchants/payments/**").hasRole("MERCHANT")

                        // 픽업대 조회 API (PUBLIC)
                        .requestMatchers("/api/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(error -> error
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                        .accessDeniedHandler(new CustomAccessDeniedHandler())
                );

        return http.build();
    }
}