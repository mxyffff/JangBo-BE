package me.swudam.jangbo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

// 인증되지 않은 사용자가 접근했을 때 JSON 반환
//public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
//
//    private final ObjectMapper om = new ObjectMapper();
//
//    @Override
//    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
//        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
//        res.setCharacterEncoding("UTF-8");
//        // 응답 바디 구성
//        Map<String, Object> body = Map.of(
//                "success", false,
//                "status", 401,
//                "code", "UNAUTHORIZED",
//                "message", "인증이 필요합니다.",
//                "path", req.getRequestURI(),
//                "timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString()
//        );
//        om.writeValue(res.getWriter(), body);
//    }
//}

public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        String path = req.getRequestURI();

        // permitAll URL은 인증 없이 통과
        if ("/api/merchants/signup".equals(path)
                || path.startsWith("/api/merchants/exists/")
                || "/api/merchants/login".equals(path)
                || "/api/merchants/logout".equals(path)) {
            return; // 그냥 통과
        }

        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "success", false,
                "status", 401,
                "code", "UNAUTHORIZED",
                "message", "인증이 필요합니다.",
                "path", path,
                "timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString()
        );
        om.writeValue(res.getWriter(), body);
    }
}