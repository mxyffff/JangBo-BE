package me.swudam.jangbo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

// Spring Security 설정
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 로그인
                .formLogin(form -> form
                        .loginPage("/merchants/login")
                        .defaultSuccessUrl("/")
                        .usernameParameter("email")
                        .failureUrl("/merchants/login/error")
                )
                // 로그아웃
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/merchants/logout"))
                        .logoutSuccessUrl("/")
                );

        // 보안 검사
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/merchants/new").permitAll() // 회원가입 페이지는 누구나 접근 가능
                        .requestMatchers("/merchants/login").anonymous() // 로그인 페이지 : 로그인하지 않은 사용자만
                        .requestMatchers("/merchants/logout").authenticated() // 로그아웃 페이지 : 로그인한 사용자만
                        .anyRequest().permitAll()
                );

        // 인증 실패시 대처 방법 커스텀
        http
                .exceptionHandling(error -> error
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                        .accessDeniedHandler(new CustomAccessDeniedHandler()) // 403 에러 핸들링
                );

        // Postman 테스트용 CSRF 토큰 비활성화
        //http.csrf(csrf -> csrf.disable());

        return http.build();
    }
}
