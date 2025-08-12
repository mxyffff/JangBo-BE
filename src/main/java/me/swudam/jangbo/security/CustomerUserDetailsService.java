package me.swudam.jangbo.security;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.entity.Customer;
import me.swudam.jangbo.repository.CustomerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/* 스프링 시큐리티가 "인증"을 수행할 때 호출하는 서비스 */
// - 파라미터 username에는 "로그인 식별자"인 이메일이 들어옴
// - 이메일로 DB에서 Customer을 조회한 뒤, UserDetails(User 객체)로 변환하여 반환함
// - 못 찾으면 UsernameNotFoundException을 던져 시큐리티가 인증 실패로 처리함

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 전용: 트랜잭션 오버헤드/락 최소화
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    /* 스프링 시큐리티 규약 메서드 */
    // - username 파라미터: 로그인 시 클라이언트가 보낸 아이디 (== 이메일)
    // - return: UserDetails 구현체 (CustomerUserDetails)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 방어적 전처리 (null/공백 체크)
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("이메일이 비어있습니다.");
        }

        // 2. 이메일 정규화
        final String email = username.trim().toLowerCase();

        // 3. DB 조회: 이메일 대소문자 무시하고 고객 검색
        // -> 존재하지 않으면 UsernameNotFoundException 던져 인증 실패로 연결
        Customer customer = customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("이메일을 찾을 수 없습니다." + email));

        // 4. Customer 엔티티를 시큐리티 표준 모델(UserDetails)로 감싸서 반환
        // - 내부에서 getUsername()은 이메일, getPassword()는 BCrypt 해시를 제공함
        return new CustomerUserDetails(customer);
    }
}
