package me.swudam.jangbo.security;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.repository.MerchantRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Spring Security에서 사용자 인증 시,
// DB에서 사용자 정보를 조회하는 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantUserDetailsService implements UserDetailsService {

    // 상인 정보를 DB에서 조회
    private final MerchantRepository merchantRepository;

    // Spring Security가 로그인 시 호출하는 메서드
    // username 파라미터 → 여기서는 이메일로 사용
    /*
    @param username 로그인 시 입력된 아이디(이메일)
    @return UserDetails 구현체 (MerchantUserDetails)
    @throws UsernameNotFoundException 이메일이 없거나 유효하지 않을 때 예외 발생
    */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 이메일이 null이거나 빈 문자열인 경우 예외
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("이메일이 비어있습니다.");
        }

        // 이메일 앞뒤 공백 제거 + 소문자로 변환 (대소문자 구분 없이 검색)
        final String email = username.trim().toLowerCase();

        // 이메일로 상인 조회(없으면 예외 발생)
        Merchant merchant = merchantRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일의 상인을 찾을 수 없습니다: " + email));

        // Spring Security에서 사용할 UserDetails 객체 반환
        return new MerchantUserDetails(merchant);
    }
}
