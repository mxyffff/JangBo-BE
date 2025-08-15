package me.swudam.jangbo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 세션 기반 인증
@Configuration // 설정 클래스 명시
@EnableJpaAuditing // @CreatedDate, @LastModifiedDate 자동 세팅
public class CustomerInfraConfig {

    // 비밀번호 암호화를 위한 BCrypt 인코더 => 인터페이스 반환으로 수정함
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
//    public BCryptPasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }

    // 이메일 인증을 위한 RedisTemplate 설정
    // Redis 문자열 전용 템플릿 -> key/value를 문자열 그대로 넣고 빼고 싶을 때
    @Bean
    // 파라미터: 스프링부트가 자동으로 LettuceConnectionFactory라는 구현체를 Bean으로 등록하는데, 여기선 cf라는 이름으로 그 Bean을 받아 StringRedisTemplate에 연결해줌
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        // String 전용 RedisTemplate 객체 생성 (key=String, value=String)
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(cf);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());

        return template;
    }
}
