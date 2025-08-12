package me.swudam.jangbo.service;

import me.swudam.jangbo.entity.Merchant;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.repository.MerchantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

// [온보딩] 상인
@Service
@Transactional
@RequiredArgsConstructor
public class MerchantService implements UserDetailsService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
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
    public Merchant saveMerchant(Merchant merchant){
        validateDuplicateMerchant(merchant);
        return merchantRepository.save(merchant);
    }

    // 비밀번호 재입력 확인
    private void validatePasswordMatch(String password, String passwordConfirm){
        if(!password.equals(passwordConfirm)){
            throw new IllegalArgumentException("비밀번호와 비밀번호 재입력이 일치하지 않습니다.");
        }
    }

    // 중복 회원 확인 (이메일, 사업자등록번호, 전화번호 기준)
    private void validateDuplicateMerchant(Merchant merchant){
        Merchant findByEmail = merchantRepository.findByEmail(merchant.getEmail());
        if(findByEmail != null){
            throw new IllegalStateException("이미 가입된 회원입니다.");
        }

        Merchant findByBusinessNumber = merchantRepository.findByBusinessNumber(merchant.getBusinessNumber());
        if(findByBusinessNumber != null){
            throw new IllegalStateException("이미 등록된 사업자등록번호입니다.");
        }

        Merchant findByPhoneNumber = merchantRepository.findByPhoneNumber(merchant.getPhoneNumber());
        if(findByPhoneNumber != null){
            throw new IllegalStateException("이미 등록된 전화번호입니다.");
        }
    }

}


