package me.swudam.jangbo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.ProductCreateRequestDto;
import me.swudam.jangbo.dto.ProductResponseDto;
import me.swudam.jangbo.dto.ProductUpdateRequestDto;
import me.swudam.jangbo.entity.Product;
import me.swudam.jangbo.security.MerchantUserDetails;
import me.swudam.jangbo.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

// 상인 전용 상품 REST API 컨트롤러
// Prefix URL: /api/merchant/products
// 정렬 옵션(sort):
// - latest : 최신순
// - cheap : 저가순
// - fresh : 신선순
// - 인기순 : 추후 구현
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/merchants/products")
public class ProductMerchantController {

    private final ProductService productService;

    // 현재 로그인 세션의 상인 id를 꺼내는 메서드
    private Long currentMerchantId(MerchantUserDetails principal) {
        return principal.getId();
    }

    // 1. Create 상품 생성
    // POST /api/merchants/products
    // Body: ProductCreateRequestDto
    // 성공: 201 Created + Location 헤더 + ProductResponseDto
    @PostMapping
    public ResponseEntity<ProductResponseDto> create(
            @AuthenticationPrincipal MerchantUserDetails principal, // 세션에서 현재 상인 정보 주입
            @Valid @RequestBody ProductCreateRequestDto requestDto // 요청 본문(JSON)을 DTO로 바인딩 + 유효성 검증
    ) {
        final Long merchantId = currentMerchantId(principal);

        // Service: 입력 검증(2차), 소유자 세팅, 저장
        Product created = productService.create(merchantId, requestDto);

        ProductResponseDto body = ProductResponseDto.from(created);
        URI location = URI.create("/api/merchants/products/" + created.getId());

        return ResponseEntity.created(location).body(body);
    }

    // 2. Read One 상품 단건 조회
    // GET /api/merchants/products/{productId}
    // - merchantId(소유권)가 일치하는 상품만 조회 가능
    // - 존재하지 않거나 merchantId 불일치 시: 서비스에서 예외 발생
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getOne(
            @AuthenticationPrincipal MerchantUserDetails principal,
            @PathVariable Long productId
    ) {
        final Long merchantId = currentMerchantId(principal);

        Product product = productService.getProductById(merchantId, productId);

        return ResponseEntity.ok(ProductResponseDto.from(product));
    }

    // 3. Update 상품 수정
    // PATCH /api/merchants/products/{productId}
    // - 부분 수정을 고려하여 PATCH 사용
    // - 서비스에서 null값 처리
    @PatchMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> update(
            @AuthenticationPrincipal MerchantUserDetails principal,
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequestDto requestDto
    ) {
        final Long merchantId = currentMerchantId(principal);

        Product updated = productService.update(merchantId, productId, requestDto);

        return ResponseEntity.ok(ProductResponseDto.from(updated));
    }

    // 4. Delete 상품 삭제
    // DELETE /api/merchants/products/{productId}
    // - 소유권 검증 후 삭제
    // - 성공 시 본문없이 204 반환
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal MerchantUserDetails principal,
            @PathVariable Long productId
    ) {
        final Long merchantId = currentMerchantId(principal);

        productService.delete(merchantId, productId);

        return ResponseEntity.noContent().build();
    }

    // 5. Read 상품 목록 조회 (특정 상인 고정)
    // GET /api/merchants/products?sort={recent|cheap|fresh|popular}
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> list(
            @AuthenticationPrincipal MerchantUserDetails principal,
            @RequestParam(name = "sort", required = false, defaultValue = "recent") String sort) {

        final Long merchantId = currentMerchantId(principal);

        List<Product> products = productService.getProductsByMerchant(merchantId, sort);
        List<ProductResponseDto> body = products.stream()
                .map(ProductResponseDto::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    // 6. 품절 처리
    // PATCH /api/merchants/products/{productId}/sold-out
    // - 재고를 0으로 만들고 soldOut = true로 처리
    @PatchMapping("/{productId}/sold-out")
    public ResponseEntity<ProductResponseDto> markSoldOut(
            @AuthenticationPrincipal MerchantUserDetails principal,
            @PathVariable Long productId
    ) {
        final Long merchantId = currentMerchantId(principal);

        Product product = productService.markSoldOut(merchantId, productId);

        return ResponseEntity.ok(ProductResponseDto.from(product));
    }
}
