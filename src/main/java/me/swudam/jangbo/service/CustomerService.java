package me.swudam.jangbo.service;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.CustomerSignupRequestDto;
import me.swudam.jangbo.dto.CustomerUpdateDto;
import me.swudam.jangbo.entity.Customer;
import me.swudam.jangbo.entity.Order;
import me.swudam.jangbo.entity.OrderStatus;
import me.swudam.jangbo.repository.CartRepository;
import me.swudam.jangbo.repository.CustomerRepository;
import me.swudam.jangbo.repository.OrderRepository;
import me.swudam.jangbo.support.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {
    // 의존성 주입
    private final CustomerRepository customerRepository; // DB 접근
    private final EmailVerificationService emailVerificationService; // 이메일 인증 상태 확인/정리
    private final PasswordEncoder passwordEncoder; // 비밀번호 해시

    // 마이페이지
    private final CartRepository cartRepository; // 장바구니
    private final OrderRepository orderRepository; // 주문

    // 이메일 인증 요구할지 여부: 기본값 true - properties 설정
    @Value("${auth.email.verify.required:true}")
    private boolean verifyRequired;

    /* 회원 가입*/
    // 1. 입력 정리(정규화): email 소문자/trim, username trim, phoneNumber trim
    // 2. 이메일 인증 완료 여부 확인 (Redis의 verified 플래그)
    // 3. username / email / phoneNumber 중복 검사
    // 4. 비밀번호 BCrypt 해시
    // 5. 저장
    // 6. 이메일 인증 플래그 정리
    // 파라미터 req: 회원가입 요청 DTO
    // return: 저장된 Customer 엔티티
    @Transactional
    public Customer signup(CustomerSignupRequestDto req) {

        final String email = normalizeEmail(req.getEmail());
        final String username = safeTrim(req.getUsername());
        // 평문 비밀번호: DTO 1차 검증 + PasswordValidator 2차 검증을 이미 통과했다고 가정
        final String rawPassword = req.getPassword();

        // 1. 중복 여부 먼저 체크
        if (customerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        // DB에도 유니크 제약이 있지만 사용자 경험을 위해 애플리케이션 레벨에서 먼저 체크
        if (customerRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 2. 그 다음 이메일 인증 여부 체크
        if (verifyRequired && !emailVerificationService.isVerified(email)) {
            // 프론트에서 "인증 메일 보내기 -> 코드 입력 -> 인증 확인" 플로우가 끝나지 않은 상태
            throw new IllegalArgumentException("이메일 인증이 필요합니다.");
        }

        // 3. 해시 결과는 매번 달라지지만 matches(raw, hash)로 검증 가능
        final String passwordHash = passwordEncoder.encode(rawPassword);

        // 4. 고객 엔티티 저장
        Customer saved = customerRepository.save(
                Customer.builder()
                        .username(username)
                        .email(email)
                        .password(passwordHash)
                        .build()
        );

        // 5. 인증이 끝난 이메일 정보 삭제
        if (verifyRequired) {
            emailVerificationService.clearVerified(email);
        }

        // 저장된 엔티티 반환 (컨트롤러에서 DTO로 변환해서 내려줌)
        return saved;
    }

    /* 내부 유틸 */
    // 이메일 표준화: null 방지 + trim(앞뒤 공백 제거) + 소문자
    private String normalizeEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        return raw.trim().toLowerCase();
    }

    // 중복 체크: 닉네임
    @Transactional(readOnly = true) // 읽기 전요 트랜잭션
    public boolean existsUsername(String username) {
        // 공백이나 null이면 “중복 아님(false)”로 응답해서 프론트가 계속 입력하도록 유도
        if (username == null || username.isBlank()) return false;
        return customerRepository.existsByUsername(safeTrim(username));
    }

    // 중복 체크: 이메일
    @Transactional(readOnly = true)
    public boolean existsEmail(String email) {
        if (email == null || email.isBlank()) return false;
        // 이메일은 normalizeEmail로 소문자/trim 처리 후 조회
        return customerRepository.existsByEmail(normalizeEmail(email));
    }

    // 고객 마이페이지
    // 1. 이메일 기반 고객 조회
    @Transactional(readOnly = true)
    public Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email) // 이메일로 고객 조회
                .orElseThrow(() -> new NotFoundException("고객을 찾을 수 없습니다.")); // 없으면 예외
    }

    // 2. 개인정보 수정 - 이메일 기반
    public void updateCustomer(String email, CustomerUpdateDto dto) {
        Customer customer = getCustomerByEmail(email);

        // 비밀번호 변경
        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            if (!dto.getNewPassword().equals(dto.getNewPasswordConfirm())) {
                throw new IllegalArgumentException("새 비밀번호와 확인이 일치하지 않습니다.");
            }
            customer.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }
        // 닉네임 변경
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            String newUsername = safeTrim(dto.getUsername());

            // 중복 체크 (본인 제외)
            Customer existing = customerRepository.findByUsername(newUsername).orElse(null);
            if (existing != null && !existing.getEmail().equals(email)) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
            customer.setUsername(newUsername);
        }
        customerRepository.save(customer);
    }

    // 3. 회원탈퇴
    @Transactional
    public void deleteCustomer(String email) {
        Customer customer = getCustomerByEmail(email);

        // 1. 완료되지 않은 장바구니 삭제
        cartRepository.deleteByCustomerId(customer.getId());

        // 2. 주문 상태만 변경
        List<Order> orders = orderRepository.findByCustomerId(customer.getId());
        for (Order order : orders) {
            order.setStatus(OrderStatus.CUSTOMER_LEFT);
        }
        orderRepository.saveAll(orders);

        // 3. 고객 삭제 대신 soft delete
        customer.setDeleted(true);
        customerRepository.save(customer);
    }

    // 안전한 trim: null -> 빈문자열 방지 & 에러 메시지 일관화에 유리
    private String safeTrim(String raw) {
        if (raw == null) return "";
        return raw.trim();
    }
}
