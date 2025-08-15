package me.swudam.jangbo.service;

import me.swudam.jangbo.dto.MerchantUpdateDto;
import me.swudam.jangbo.entity.Merchant;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.repository.MerchantRepository;
import me.swudam.jangbo.support.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    private final MerchantRepository merchantRepository; // DB 접근용
    private final PasswordEncoder passwordEncoder; // 비밀번호 암호화/검증

    public PasswordEncoder getPasswordEncoder() { return passwordEncoder; }

    // 회원 저장 (회원가입)
    public Merchant saveMerchant(Merchant merchant) {
        // 이메일 중복 검사
        if (existsEmail(merchant.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        return merchantRepository.save(merchant);
    }

    @Transactional(readOnly = true)
    public boolean existsEmail(String email) {
        return merchantRepository.existsByEmail(email); // 이메일 존재 여부 확인
    }

    @Transactional(readOnly = true)
    public Merchant getMerchantByEmail(String email) {
        Merchant merchant = merchantRepository.findByEmail(email); // 이메일로 상인 조회
        if (merchant == null) throw new NotFoundException("상인을 찾을 수 없습니다."); // 없으면 예외
        return merchant;
    }

    // 개인정보 수정 - 이메일 기반
    public void updateMerchant(String email, MerchantUpdateDto dto) {
        Merchant merchant = getMerchantByEmail(email);

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
}


