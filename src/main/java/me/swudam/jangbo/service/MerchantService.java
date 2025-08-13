package me.swudam.jangbo.service;

import me.swudam.jangbo.entity.Merchant;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.repository.MerchantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;

import java.util.List;

// [온보딩] 상인
@Service
@Transactional
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;

    // 스프링 시큐리티 로그인 기능
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{
        Merchant merchant = merchantRepository.findByEmail(email);

        // 상인 유저가 존재하지 않는다면 UsernameNotFoundException 처리
        if(merchant == null){
            throw new UsernameNotFoundException(email);
        }

        // UserDetails를 구현하고 있는 User 객체 반환
        return User.builder()
                .username(merchant.getEmail()) // 로그인 ID로 이메일 사용
                .password(merchant.getPassword()) // 암호화된 비밀번호
                .authorities(List.of()) // 별도의 권한 없이 빈 리스트만 할당
                .build();
    }

    // 회원 저장 (회원가입)
    public Merchant saveMerchant(Merchant merchant) {
        if (existsEmail(merchant.getEmail())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }
        return merchantRepository.save(merchant);
    }

    // 이메일 중복 여부 확인
    @Transactional(readOnly = true)
    public boolean existsEmail(String email){
        return merchantRepository.existsByEmail(email);
    }
}


