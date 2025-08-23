package me.swudam.jangbo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Spring AI가 제공하는 ChatClient를 빈으로 노출
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        // builder는 Spring AI가 자동 주입 (OpenAI 설정 포함)
        return builder.build();
    }
}
