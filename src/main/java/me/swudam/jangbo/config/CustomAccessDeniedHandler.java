package me.swudam.jangbo.config;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

// 로그인은 했으나 접근(인가) 권한이 없을 때 (403 Forbidden)
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        // 에러 처리 로직
        response.sendRedirect("/merchant/login/error"); // 권한이 없는 경우 error 페이지로 리다이렉트
    }
}
