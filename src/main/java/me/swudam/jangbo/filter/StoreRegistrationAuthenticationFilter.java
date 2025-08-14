package me.swudam.jangbo.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/*
  Postman API 테스트 시,
  회원가입 직후 로그인 없이 상점 등록 API를 호출할 수 있도록
  임시 Authentication 객체를 생성해주는 필터.

  - dev 프로파일에서만 활성화됨 (@Profile("dev"))
  - 세션에 저장된 가입 직후 이메일을 읽어와 인증 처리
 */
@Profile("dev")
@Component
public class StoreRegistrationAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 현재 요청에 대한 세션을 가져옴 (false: 세션이 없으면 새로 생성하지 않음)
        HttpSession session = request.getSession(false);
        if (session != null) {
            // 회원가입 직후 상태를 나타내는 세션 값 읽기
            String email = (String) session.getAttribute("justRegisteredMerchantEmail");
            if (email != null) {
                // 임시 인증 객체 생성
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        email,// principal: 이메일
                        null, // credentials: 없음 (비밀번호 불필요)
                        List.of(new SimpleGrantedAuthority("ROLE_MERCHANT")) // 권한: ROLE_MERCHANT
                );
                // SecurityContext에 인증 객체 저장 → 이후 컨트롤러에서 로그인 상태처럼 동작
                SecurityContextHolder.getContext().setAuthentication(auth);

                // 1회성 사용을 위해 세션에서 이메일 제거
                session.removeAttribute("justRegisteredMerchantEmail");
            }
        }

        // 다음 필터나 컨트롤러로 요청을 전달
        filterChain.doFilter(request, response);
    }
}
