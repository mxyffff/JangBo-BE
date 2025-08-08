package me.swudam.jangbo.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

// Spring Security에서 인증되지 않은 사용자가 접근했을 때
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException)
            throws IOException, ServletException {
        if ("XMLHttpRequest".equals(request.getHeader("x-requested-with"))) { // 요청 헤더 값 확인
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // AJAX 요청이면 401(Unauthorized) 응답
        } else{ // AJAX 요청이 아니라면 (브라우저에서 직접 요청한 경우)
            response.sendRedirect("/merchant/login"); // 페이지 리다이렉트
        }
    }
}
