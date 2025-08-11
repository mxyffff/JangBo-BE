package me.swudam.jangbo.security;

import me.swudam.jangbo.entity.Customer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/* 스프링 시큐리티가 이해할 수 있는 형태(UserDetails)로 Customer 엔티티를 감싸는 어댑터 클래스 */
// - getUsername(): 로그인 식별자(email 사용)
// - getPassword(): 암호화된 비밀번호
// - getAuthorities(): 권한 리스트(간단히 ROLE_CUSTOMER 하나 부여)
// - 나머지 계정 상태 플래그: 기본 true (만료/잠금/비활성화 안씀)
public class CustomerUserDetails implements UserDetails {

    private final Long id; // 도메인 PK (편의상 보관)
    private final String email; // 로그인용 이메일
    private final String password; // BCrypt 해시
    private final List<GrantedAuthority> authorities;

    public CustomerUserDetails(Customer customer) {
        this.id = customer.getId();
        this.email = customer.getEmail();
        this.password = customer.getPassword();
        // 최소 권한 하나: ROLE_CUSTOMER
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    public Long getId() {
        return id;
    }

    // 스프링 시큐리티 관점의 사용자명 -> 이메일
    @Override
    public String getUsername() {
        return email;
    }

    // 암호화된 비밀번호 반환
    @Override
    public String getPassword() {
        return password;
    }

    // 권한 목록
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // 아래 4개는 계정 상태. 지금은 모두 true로 사용
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    @Override
    public boolean isEnabled() {
        return true;
    }
}
