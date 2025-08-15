package me.swudam.jangbo.service;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.ProductCreateRequestDto;
import me.swudam.jangbo.dto.ProductUpdateRequestDto;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.entity.Product;
import me.swudam.jangbo.repository.MerchantRepository;
import me.swudam.jangbo.repository.ProductRepository;
import me.swudam.jangbo.support.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

// 상품 도메인 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final MerchantRepository merchantRepository;

    /* 조회 */

    // 단건 조회 (상인 전용: 소유권 검증 포함)
    public Product getProductById(Long merchantId, Long productId) {
        return productRepository.findByIdAndMerchantId(productId, merchantId)
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없거나 접근 권한이 없습니다."));
    }

    // 단건 조회 (고객 전용: 소유권 미검증)
    public Product getPublicProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다."));
    }

    // 특정 상인 목록 정렬 조회 (상인/고객 공용: 상점 페이지)
    // recent : 최신순, cheap : 저가순, fresh : 신선순
    public List<Product> getProductsByMerchant(Long merchantId, String sort) {
        String key = normalizeSort(sort);
        return switch (key) {
            case "recent" -> productRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId);
            case "cheap" -> productRepository.findAllByMerchantIdOrderByPriceAsc(merchantId);
            case "fresh" -> productRepository.findAllByMerchantIdOrderByExpiryDateDesc(merchantId);
            default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준: " + sort);
        };
    }

    // 고객 전용 공개 조회
    // 전역 목록 정렬
    public List<Product> getPublicProducts(String sort) {
        String key = normalizeSort(sort);
        return switch (key) {
            case "recent" -> productRepository.findAllByOrderByCreatedAtDesc();
            case "cheap" -> productRepository.findAllByOrderByPriceAsc();
            case "fresh" -> productRepository.findAllByOrderByExpiryDateDesc();
            default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준: " + sort);
        };
    }

    // 전역 이름 검색 + 정렬 (고객 전용)
    // - recent : 특정 상인 + 최신순 => merchantId 필수
    // - cheap : 전역 저가순
    // - fresh : 전역 신선순
    public List<Product> searchPublicProducts(Long merchantId, String keyword, String sort) {
        final String q = (keyword == null) ? "" : keyword.trim();
        if (q.isEmpty()) {
            // 키워드가 없으면 전역 목록 동작으로 위임 -> AI 기능 구현시 예외처리 필요 !!!
            return getPublicProducts(sort);
        }

        String key = normalizeSort(sort);
        return switch (key) {
            case "recent" -> {
                if (merchantId == null)
                    throw new IllegalArgumentException("recent 검색은 merchantId가 필요합니다.");
                yield productRepository.findByMerchantIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(merchantId, q);
            }
            case "cheap" -> productRepository.findByNameContainingIgnoreCaseOrderByPriceAsc(q);
            case "fresh" -> productRepository.findByNameContainingIgnoreCaseOrderByExpiryDateDesc(q);
            default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준: " + sort);
        };
    }

    // 특정 상인 페이지용 공개 목록 (고객 전용)
    public List<Product> getPublicProductsByMerchant(Long merchantId, String sort) {
        return getProductsByMerchant(merchantId, sort);  // 상점 페이지는 동일 로직 재사용
    }

    /* CUD (상인 전용 */

    // 상품 생성
    @Transactional
    public Product create(Long merchantId, ProductCreateRequestDto dto) {
        Merchant owner = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("상인을 찾을 수 없습니다."));

        if (dto.getStock() == null || dto.getStock() < 1) {
            throw new IllegalArgumentException("재고는 1 이상이어야 합니다.");
        }
        if (dto.getPrice() == null || dto.getPrice() < 1) {
            throw new IllegalArgumentException("가격은 1원 이상이어야 합니다.");
        }

        Product product = Product.builder()
                .merchant(owner)
                .name(dto.getName())
                .origin(dto.getOrigin())
                .expiryDate(dto.getExpiryDate())
                .stock(dto.getStock())
                .price(dto.getPrice())
                .imageUrl(dto.getImageUrl())
                .soldOut(false) // 등록 시 기본값
                .build();

        return productRepository.save(product);
    }

    // 상품 수정
    @Transactional
    public Product update(Long merchantId, Long productId, ProductUpdateRequestDto dto) {
        Product product = getProductById(merchantId, productId);

        // 방어적 체크 (DTO @Valid로 1차 검증, 여기서 2차 보강)
        if (dto.getStock() == null || dto.getStock() < 1) {
            throw new IllegalArgumentException("재고는 1 이상이어야 합니다.");
        }
        if (dto.getPrice() == null || dto.getPrice() < 1) {
            throw new IllegalArgumentException("가격은 1원 이상이어야 합니다.");
        }

        // 엔티티에 집약된 규칙 메서드 사용
        product.updateProduct(
                dto.getName(),
                dto.getOrigin(),
                dto.getExpiryDate(),
                dto.getPrice(),
                dto.getStock(), // 1 이상이므로 품절 상태면 자동 해제됨
                dto.getImageUrl()
        );

        return product; // JPA Dirty Checking
    }

    // 상품 삭제
    @Transactional
    public void delete(Long merchantId, Long productId) {
        Product product = getProductById(merchantId, productId);

        productRepository.delete(product);
    }

    // 품절 처리 (재고를 강제로 0으로 만들고 soldOut = true)
    // 재고 0을 만드는 유일한 엔드포인트
    @Transactional
    public Product markSoldOut(Long merchantId, Long productId) {
        Product product = getProductById(merchantId, productId);
        product.markSoldOut();
        return product;
    }


    // 내부 유틸 메서드
    private String normalizeSort(String sort) {
        String key = (sort == null) ? "recent" : sort.toLowerCase(Locale.ROOT).trim();
        return switch (key) {
            case "recent", "cheap", "fresh" -> key;
            default -> "recent";
        };
    }
}
