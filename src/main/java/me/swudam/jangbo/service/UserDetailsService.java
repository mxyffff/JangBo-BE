package me.swudam.jangbo.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserDetailsService {
    // 상인 정보가 존재하지 않을 경우 예외를 발생시켜 인증 실패 처리
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
