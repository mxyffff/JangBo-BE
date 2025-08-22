package me.swudam.jangbo.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// 테스트용 주석 처리 - 0822
//@Profile("dev")
@Component
public class StoreRegistrationAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            // Postman 테스트용 플래그 확인
            String email = (String) session.getAttribute("justRegisteredMerchantEmail");
            if (email != null) {
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

                // 1회성 플래그 제거 (Postman 테스트용)
                session.removeAttribute("justRegisteredMerchantEmail");
            }
        }

        filterChain.doFilter(request, response);
    }
}
