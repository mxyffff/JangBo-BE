package me.swudam.jangbo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customers", uniqueConstraints = {
        // DB 레벨(유니크 인덱스)에서 최종 중복 차단 — 동시요청 경쟁상황에도 안전
        @UniqueConstraint(name = "uk_customers_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_customers_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_phone_number", columnNames = "phone_number")

})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Customer extends BaseTimeEntity {

    // 기본키, auto_increment 설정
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // username (닉네임, 로그인 id는 email 사용)
    @Column(nullable = false, length = 30)
    private String username;

    // email (로그인 id)
    @Column(nullable = false, length = 255)
    private String email;

    // BCrypt 해시 적용된 password
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    // 전화번호
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;
}
