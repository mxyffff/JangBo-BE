package me.swudam.jangbo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customers", uniqueConstraints = {
        // DB 레벨(유니크 인덱스)에서 최종 중복 차단 — 동시요청 경쟁상황에도 안전
        @UniqueConstraint(name = "uk_customers_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_customers_email", columnNames = "email"),

})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Customer extends BaseTimeEntity {

    // 기본키, auto_increment 설정
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long id;

    // username (닉네임, 로그인 id는 email  사용)
    @Setter // 마이페이지 - 개인정보 변경 위해 Setter 추가
    @Column(nullable = false, length = 30)
    private String username;

    // email (로그인 id)
    @Setter // 마이페이지 - 회원탈퇴 Setter 추가
    @Column(nullable = false, length = 255)
    private String email;

    // BCrypt 해시 적용된 password
    @Setter // 마이페이지 - 개인정보 변경 위해 Setter 추가
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    // 마이페이지 - 회원탈퇴용 삭제 플래그
    @Setter
    private boolean deleted = false;
}
