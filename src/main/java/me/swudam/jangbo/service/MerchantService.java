package me.swudam.jangbo.service;

import me.swudam.jangbo.dto.MerchantSignupRequestDto;
import me.swudam.jangbo.dto.MerchantUpdateDto;
import me.swudam.jangbo.entity.Merchant;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.entity.Order;
import me.swudam.jangbo.entity.OrderStatus;
import me.swudam.jangbo.repository.MerchantRepository;
import me.swudam.jangbo.repository.OrderRepository;
import me.swudam.jangbo.support.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// [온보딩] 상인
@Service
@Transactional
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository; // DB 접근용
    private final OrderRepository orderRepository; // 회원 탈퇴 시 주문 상태 MERCHANT_LEFT로
    private final PasswordEncoder passwordEncoder; // 비밀번호 암호화/검증
    private final EmailVerificationService emailVerificationService; // 이메일 인증 상태 확인/정리

    // 이메일 인증 요구할지 여부: 기본값 true - properties 설정
    @Value("${auth.email.verify.required:true}")
    private boolean verifyRequired;

    /* 신규 메서드: 회원가입 메서드 */
    public Merchant signup(MerchantSignupRequestDto requestDto) {
        final String email = normalizeEmail(requestDto.getEmail());
        final String username = safeTrim(requestDto.getUsername());
        final String rawPassword = requestDto.getPassword();

        // 1. 중복 여부 먼저 체크
        if (merchantRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        // 2. 그 다음 이메일 인증 여부 체크
        if (verifyRequired && !emailVerificationService.isVerified(email)) {
            throw new IllegalArgumentException("이메일 인증이 필요합니다.");
        }

        final String passwordHash = passwordEncoder.encode(rawPassword);

        Merchant saved = new Merchant();
        saved.setUsername(username);
        saved.setEmail(email);
        saved.setPassword(passwordHash);

        merchantRepository.save(saved);

        if(verifyRequired) {
            emailVerificationService.clearVerified(email);
        }

        return saved;
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

        // 1. 상인이 맡은 주문들 상태를 MERCHANT_LEFT로 변경
        List<Order> orders = orderRepository.findByStore_Merchant_Id(merchant.getId());
        for (Order order : orders) {
            order.setStatus(OrderStatus.MERCHANT_LEFT);
            order.setMerchant(null); // 연관 관계 제거
        }

        // 2. 상인 삭제
        merchantRepository.delete(merchant);
    }

    /* 신규 메서드: 내부 유틸 메서드 */
    private String normalizeEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        return raw.trim().toLowerCase();
    }
    private String safeTrim(String raw) {
        return raw == null ? "" : raw.trim();
    }
}


