package me.swudam.jangbo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.ai.AiBulkAddToCartRequestDto;
import me.swudam.jangbo.dto.ai.AiBulkAddToCartResponseDto;
import me.swudam.jangbo.dto.ai.AiRecommendationRequestDto;
import me.swudam.jangbo.entity.Customer;
import me.swudam.jangbo.repository.CustomerRepository;
import me.swudam.jangbo.service.AiCartFacadeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI 장보 - 3차 처리 컨트롤러
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiShopController {

    private final AiCartFacadeService aiCartFacadeService;
    private final CustomerRepository customerRepository;

    // 4.8 추천 결과 일괄 담기
    // POST /api/ai/cart/bulk-add
    // body: { "items": [ {"productId": 10, "quantity": 1}, {"productId": 22, "quantity": 2} ] }
    // 응답: 담긴 항목 요약 + 최신 장바구니 합계/수수료
    @PostMapping("/cart/bulk-add")
    public ResponseEntity<AiBulkAddToCartResponseDto> addAllToCart(
            @RequestBody @Valid AiBulkAddToCartRequestDto requestDto
    ) {
        Long customerId = getCurrentCustomerIdOrThrow();
        var res = aiCartFacadeService.addAll(customerId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /* 인증 유틸 (다른 컨트롤러와 동일 패턴) */
    private Long getCurrentCustomerIdOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || isAnonymous(auth)) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }
        String email = (auth.getPrincipal() instanceof UserDetails ud)
                ? ud.getUsername()
                : String.valueOf(auth.getPrincipal());
        Customer customer = customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("고객 정보를 찾을 수 없습니다."));
        return customer.getId();
    }

    private boolean isAnonymous(Authentication auth) {
        String name = auth.getName();
        return name == null || "anonymousUser".equals(name);
    }
}
