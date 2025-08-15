package me.swudam.jangbo.service;

import me.swudam.jangbo.dto.MerchantUpdateDto;
import me.swudam.jangbo.entity.Merchant;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.repository.MerchantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository; // DB 접근용
    private final PasswordEncoder passwordEncoder; // 비밀번호 암호화/검증

    // 회원가입
    public Merchant saveMerchant(Merchant merchant) {
        // 이메일 중복 검사
        if (existsEmail(merchant.getEmail())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }
        return merchantRepository.save(merchant);
    }

    @Transactional(readOnly = true)
    public boolean existsEmail(String email){
        return merchantRepository.existsByEmail(email); // 이메일 존재 여부 확인
    }

    @Transactional(readOnly = true)
    public Merchant getMerchantByEmail(String email) {
        Merchant merchant = merchantRepository.findByEmail(email); // 이메일로 상인 조회
        if (merchant == null) throw new IllegalStateException("상인을 찾을 수 없습니다."); // 없으면 예외
        return merchant;
    }

    // 개인정보 수정 - 이메일 기반
    public void updateMerchant(String email, MerchantUpdateDto dto) {
        Merchant merchant = getMerchantByEmail(email);
        if (merchant == null) {
            throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다.");
        }

        // 비밀번호 변경
        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            if (!dto.getNewPassword().equals(dto.getNewPasswordConfirm())) {
                throw new IllegalArgumentException("새 비밀번호와 확인이 일치하지 않습니다.");
            }
            merchant.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }
        // username 업데이트
        if (dto.getUsername() != null) {
            merchant.setUsername(dto.getUsername());
        }
        merchantRepository.save(merchant);
    }

    // 회원탈퇴
    public void deleteMerchant(String email) {
        Merchant merchant = getMerchantByEmail(email);
        merchantRepository.delete(merchant);
    }

    // Spring Security 로그인
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        Merchant merchant = merchantRepository.findByEmail(email); // 이메일로 상인 조회
        if (merchant == null) throw new UsernameNotFoundException(email); // 없으면 예외

        // UserDetails 객체 반환 (Spring Security 인증용)
        return User.builder()
                .username(merchant.getEmail()) // 로그인 ID
                .password(merchant.getPassword()) // 암호화된 비밀번호
                .authorities(List.of()) // (빈) 권한 리스트
                .build();
    }
}


