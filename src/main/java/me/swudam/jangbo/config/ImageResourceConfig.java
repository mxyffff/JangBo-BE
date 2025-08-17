package me.swudam.jangbo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ImageResourceConfig implements WebMvcConfigurer {

    // uploadPath는 application.properties/application-prod.properties 두 곳에 존재하지만,
    // 서버 실행 시 prod 환경으로 실행을 하며,
    // 활성화된 프로파일(prod)을 기준으로 주입하기에 문제 x
    @Value("${uploadPath}")
    private String uploadPath;

    /*
     * 이미지 업로드 파일 서빙
     * - 클라이언트가 "/images/**" 경로로 요청하면 서버 파일 시스템의 uploadPath 폴더에서 파일을 찾아 응답
     * - Nginx를 거치지 않고도 Spring이 직접 static resource처럼 처리 가능
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:///" + uploadPath + "/");
    }
}