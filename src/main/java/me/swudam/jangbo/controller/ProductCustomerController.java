package me.swudam.jangbo.controller;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.ProductResponseDto;
import me.swudam.jangbo.entity.Product;
import me.swudam.jangbo.repository.ProductRepository;
import me.swudam.jangbo.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

// 고객 전용 상품 조회 REST API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductCustomerController {

    private final ProductService productService;

    // 1. 단건 상세 조회
    // GET /api/products/{productId}
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getOne(@PathVariable Long productId) {
        Product product = productService.getPublicProduct(productId);
        return ResponseEntity.ok(ProductResponseDto.from(product));
    }

    // 2. 특정 상인(상점 페이지) 목록
    // GET /api/products/merchants/{merchantId}?sort={recent|cheap|fresh} -> 인기순 sort 추후 추가
    @GetMapping("/merchants/{merchantId}")
    public ResponseEntity<List<ProductResponseDto>> listByMerchant(
            @PathVariable Long merchantId,
            @RequestParam(name = "sort", required = false, defaultValue = "recent") String sortRaw
    ) {
        String sort = normalizeSort(sortRaw);
        List<Product> products = productService.getPublicProductsByMerchant(merchantId, sort);
        return ResponseEntity.ok(ProductResponseDto.fromList(products));
    }

    // 3. 전역 이름 검색 + 정렬
    // GET /api/products/search?keyword=...&sort={recent|cheap|fresh}&MerchantId=...
    // - recent: 특정 상인 목록 내에서 최신순 → merchantId 반드시 필요
    // - cheap  : 전역 저가순 → merchantId 무시
    // - fresh  : 전역 신선순 → merchantId 무시

    // 프론트 전달 (AI 장보 2차 카테고리)
    // - 최대한 상점 한 곳에서 구매할 수 있는 식재료: sort=recent&merchantId={상인ID}
    // - 유통기한, 가격순: sort=cheap|fresh 로 호출 (merchantId 생략)
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDto>> search(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sort", required = false, defaultValue = "recent") String sortRaw,
            @RequestParam(name = "merchantId", required = false) Long merchantId // recent일 때만 필요
    ) {
        String sort = normalizeSort(sortRaw);
        List<Product> products = productService.searchPublicProducts(merchantId, keyword, sort);
        return ResponseEntity.ok(ProductResponseDto.fromList(products));
    }

    // 내부 유틸: 정렬 파라미터 보정
    private String normalizeSort(String raw) {
        String key = (raw == null) ? "recent" : raw.toLowerCase(Locale.ROOT).trim();
        return switch (key) {
            case "recent", "cheap", "fresh", "popular" -> key;
            default -> "recent";
        };
    }
}
