package me.swudam.jangbo.config;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.security.CustomerUserDetailsService;
import me.swudam.jangbo.security.MerchantUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class SecurityBeansConfig {

    private final PasswordEncoder passwordEncoder;

    private final CustomerUserDetailsService customerUserDetailsService;
    private final MerchantUserDetailsService merchantUserDetailsService;

    @Bean("customerDaoAuthProvider")
    public AuthenticationProvider customerDaoAuthProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customerUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean("merchantDaoAuthProvider")
    public AuthenticationProvider merchantDaoAuthProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(merchantUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(
                Arrays.asList(
                        customerDaoAuthProvider(), // 로그인 시 우선 시도
                        merchantDaoAuthProvider() // (실패 시) 다음 순서로 시도
                )
        );

    }
}