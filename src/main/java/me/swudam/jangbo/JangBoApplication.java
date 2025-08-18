package me.swudam.jangbo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class JangBoApplication {
    public static void main(String[] args) {
        SpringApplication.run(JangBoApplication.class, args);
    }
}
