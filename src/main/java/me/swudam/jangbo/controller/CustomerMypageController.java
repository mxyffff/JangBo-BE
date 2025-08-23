package me.swudam.jangbo.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.CustomerUpdateDto;
import me.swudam.jangbo.repository.CustomerRepository;
import me.swudam.jangbo.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerMypageController {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;

    // 1. 개인정보 수정
    // PUT - /api/customers/me
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody CustomerUpdateDto dto, HttpSession session) {
        String email = getAuthenticatedCustomerEmail(session);
        if (email == null) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }
        customerService.updateCustomer(email, dto); // 이메일 기반 호출
        return ResponseEntity.ok(Map.of(
                "updated", true,
                "message", "정보가 수정되었습니다."
        ));
    }

    // 2. 회원탈퇴
    // DELETE - /api/customers/me
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMe(HttpSession session) {
        String email = getAuthenticatedCustomerEmail(session);
        if (email == null) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }
        customerService.deleteCustomer(email); // 비밀번호 입력 없이 탈퇴
        return ResponseEntity.ok(Map.of(
                "deleted", true,
                "message", "회원 탈퇴가 완료되었습니다."
        ));
    }

    // 3. 닉네임만 반환
    // GET - /api/customers/me/username
    @GetMapping("/me/username")
    public ResponseEntity<?> getUsername(HttpSession session) {
        String email = getAuthenticatedCustomerEmail(session);
        if (email == null) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }
        String username = customerService.getCustomerByEmail(email).getUsername();

        return ResponseEntity.ok(Map.of(
                "username", username
        ));
    }


    // Helper: 현재 로그인한 고객 이메일 가져오기
    private String getAuthenticatedCustomerEmail(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = null;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails ud) email = ud.getUsername();
            else if (principal instanceof String s) email = s;
        }
        if (email == null) {
            email = (String) session.getAttribute("customerEmail");
        }
        return email; // null이면 인증 실패
    }
}
