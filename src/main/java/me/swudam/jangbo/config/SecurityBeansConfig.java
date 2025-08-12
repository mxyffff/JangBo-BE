package me.swudam.jangbo.config;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.security.CustomerUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class SecurityBeansConfig {

    private final CustomerUserDetailsService customerUserDetailsService;
    private final PasswordEncoder passwordEncoder; // CustormerInfraConfig에 Bcrypt 등록되어 있음

    @Bean
    public AuthenticationProvider customerDaoAuthProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customerUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        // AuthenticationConfiguration이 위 provider를 인지해서 AuthenticationManager를 만들어줌
        return configuration.getAuthenticationManager();
    }
}
