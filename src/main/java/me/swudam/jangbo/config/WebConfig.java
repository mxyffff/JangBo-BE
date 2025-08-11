package me.swudam.jangbo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    /* 전역 CORS 설정 */
    // - addMapping("/**"): 모든 엔드포인트에 대해 CORS 허용
    // - allwedOrigins("http://localhost:3000"): 개발 단계에서 React 개발 서버의 출처를 허용함
    // -> 배포시 (예시) "https://app.jangbo.com" 형태로 교체해야 함
    // - allowedMethods: 프론트에서 사용 가능한 HTTP 메서드 지정
    // - allowedHeaders: 클라이언트가 전송 가능한 커스텀 헤더 허용
    // - exposedHeaders: 프론트에서 읽을 수 있게 노출시킬 응답 헤더
    // - allowCredentials(true): 쿠키/세션 전송 허용(세션 인증 시 필수)
    // - maxAge(3600): 브라우저가 Preflight(OPTIONS) 결과를 캐시하는 시간(초)
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 API 경로 허용
                .allowedOrigins(
                        "http://localhost:3000" // 개발 단계 로컬 React 서버
                        // 배포 시: "https://프론트_실서비스_도메인"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*") // 필요한 경우 구체적으로 명시 가능
                .allowedHeaders("Location") // 예: 생성 리소스의 URI를 Location 헤더로 노출할 때
                .allowCredentials(true) // 세션/쿠키 사용 시 반드시 true
                .maxAge(3600); // Preflight 캐시 1시간
    }
}
