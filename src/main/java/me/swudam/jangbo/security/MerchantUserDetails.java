package me.swudam.jangbo.security;

import lombok.Getter;
import lombok.Setter;
import me.swudam.jangbo.entity.Merchant;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Spring Security의 UserDetails 구현체
// 상인 계정 정보를 인증 컨텍스트에 담기 위해 사용
// AuthenticationProvider가 인증 성공 시, SecurityContextHolder에 저장
@Getter
@Setter
public class MerchantUserDetails implements UserDetails {
    private final Long id;
    private final String email;
    private final String password;
    private final List<GrantedAuthority> authorities;

    // Merchant 엔티티를 받아 UserDetails로 변환
    // ROLE_MERCHANT 권한을 기본 부여
    public MerchantUserDetails(Merchant merchant) {
        this.id = merchant.getId();
        this.email = merchant.getEmail();
        this.password = merchant.getPassword();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"));
    }

    public Long getId() { return id; }
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isAccountNonExpired() { return true; } // 계정 만료 여부. true면 만료되지 않음
    @Override public boolean isAccountNonLocked() { return true; } // 계정 잠금 여부. true면 잠금되지 않음
    @Override public boolean isCredentialsNonExpired() { return true; } // 비밀번호 만료 여부. true면 만료되지 않음
    @Override public boolean isEnabled() { return true; } // 계정 활성화 여부. true면 활성화 상태
}
