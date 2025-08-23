package me.swudam.jangbo.service;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.ai.*;
import me.swudam.jangbo.entity.Product;
import me.swudam.jangbo.repository.ProductRepository;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

// 2차 처리: 1차 AI가 뽑아준 ‘식재료 단어들’ + 고정 필터 1개 → 상품 추천
@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final ProductRepository productRepository;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    // 공개 API: 2차 추천 메인 엔트리
    @Transactional
    public AiRecommendationResponseDto recommend(AiRecommendationRequestDto requestDto) {
        // 0. 파라미터 방어
        List<String> ingredients = Optional.ofNullable(requestDto.getIngredients()).orElse(List.of())
                .stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("ingredients가 비어 있습니다.");
        }
        if (requestDto.getFilter() == null) {
            throw new IllegalArgumentException("filter는 필수입니다.");
        }

        // 1. 필터 분기
        return switch (requestDto.getFilter()) {
            case CHEAPEST -> recommendCheapest(ingredients);
            case LONGEST_EXPIRY -> recommendLongestExpiry(ingredients);
            case MAX_COVERAGE_ONE_STORE -> recommendMaxCoverageOneStore(ingredients);
        };
    }

    // 1. 가장 저렴한 식재료
    private AiRecommendationResponseDto recommendCheapest(List<String> ingredients) {
        List<IngredientPickDto> picks = new ArrayList<>();

        for (String ing : ingredients) {
            String q = expandKeyword(ing);

            // 가격 오름차순으로 전부 조회
            List<Product> candidates = productRepository
                    .findByNameContainingIgnoreCaseOrderByPriceAsc(q);

            // 품절 제외 + 재고 1 이상만
            Optional<Product> cheapest = candidates.stream()
                    .filter(this::isSellable)
                    .findFirst(); // 정렬되어 있는 데이터므로 첫 번째가 최저가

            cheapest.map(this::toSummary)
                    .ifPresent(summary -> picks.add(
                            IngredientPickDto.builder()
                                    .ingredient(ing)
                                    .product(summary)
                                    .build()
                    ));
        }
        return AiRecommendationResponseDto.builder()
                .filter(AiFilterType.CHEAPEST)
                .picks(picks)
                .build();
    }

    // 2. 유통기한이 가장 많이 남은 식재료
    private AiRecommendationResponseDto recommendLongestExpiry(List<String> ingredients) {
        List<IngredientPickDto> picks = new ArrayList<>();

        for (String ing : ingredients) {
            String q = expandKeyword(ing);

            // 유통기한 내림차순
            List<Product> candidates = productRepository
                    .findByNameContainingIgnoreCaseOrderByExpiryDateDesc(q);

            Optional<Product> freshest = candidates.stream()
                    .filter(this::isSellable)
                    .findFirst();

            freshest.map(this::toSummary)
                    .ifPresent(summary -> picks.add(
                            IngredientPickDto.builder()
                                    .ingredient(ing)
                                    .product(summary)
                                    .build()
                    ));
        }
        return AiRecommendationResponseDto.builder()
                .filter(AiFilterType.LONGEST_EXPIRY)
                .picks(picks)
                .build();
    }

    // 3. 한 상점에서 최대한 많이 살 수 있도록
    // 로직:
    // - 각 식재료에 대해 '가격 오름차순' 목록을 가져온 뒤, 상점별로 "그 식재료의 최저가 후보"만 남김
    // - 모든 식재료를 누적하여 상점별 커버 수를 계산
    // - (동률 시) 총액이 더 낮은 상점 선택
    private AiRecommendationResponseDto recommendMaxCoverageOneStore(List<String> ingredients) {
        // storeId -> (ingredient -> 이 상점에서 그 식재료로 고를 최저가 상품)
        Map<Long, Map<String, Product>> byStore = new HashMap<>();

        for (String ing : ingredients) {
            String q = expandKeyword(ing);

            // 해당 식재료 후보 (가격 오름차순)
            List<Product> candidates = productRepository
                    .findByNameContainingIgnoreCaseOrderByPriceAsc(q)
                    .stream()
                    .filter(this::isSellable)
                    .toList();

            // 상점별로 "그 식재료의 최저가" 하나씩만 보관
            Map<Long, Product> cheapestPerStore = new HashMap<>();
            for (Product p : candidates) {
                Long storeId = p.getStore().getId();
                Product cur = cheapestPerStore.get(storeId);
                if (cur == null || p.getPrice() < cur.getPrice()) {
                    cheapestPerStore.put(storeId, p);
                }
            }
            // 마스터 맵에 병합 (ingredient 기준으로 저장)
            for (Map.Entry<Long, Product> e : cheapestPerStore.entrySet()) {
                byStore.computeIfAbsent(e.getKey(), k -> new HashMap<>())
                        .put(ing, e.getValue());
            }
        }

        // 후보가 하나라도 없으면 빈 응답
        if (byStore.isEmpty()) {
            return AiRecommendationResponseDto.builder()
                    .filter(AiFilterType.MAX_COVERAGE_ONE_STORE)
                    .picks(List.of())
                    .selectedStoreId(null)
                    .selectedStoreName(null)
                    .coveredCount(0)
                    .sumPrice(0)
                    .build();
        }
        // 상점 선택: (1) 커버 수 최대 (2) 총액 최소
        Long bestStoreId = null;
        int bestCovered = -1;
        int bestSumPrice = Integer.MAX_VALUE;

        for (Map.Entry<Long, Map<String, Product>> e : byStore.entrySet()) {
            int covered = e.getValue().size();
            int sum = e.getValue().values().stream().mapToInt(Product::getPrice).sum();

            if (covered > bestCovered || (covered == bestCovered && sum < bestSumPrice)) {
                bestCovered = covered;
                bestSumPrice = sum;
                bestStoreId = e.getKey();
            }
        }

        // 최종 선정된 상점에서의 대표 상품 목록 구성
        Map<String, Product> chosen = byStore.get(bestStoreId);
        List<IngredientPickDto> picks = new ArrayList<>();
        String storeName = null;

        for (String ing : ingredients) {
            Product p = chosen.get(ing);
            if (p != null) {
                storeName = p.getStore().getStoreName();
                picks.add(IngredientPickDto.builder()
                        .ingredient(ing)
                        .product(toSummary(p))
                        .build());
            }
        }

        return AiRecommendationResponseDto.builder()
                .filter(AiFilterType.MAX_COVERAGE_ONE_STORE)
                .picks(picks)
                .selectedStoreId(bestStoreId)
                .selectedStoreName(storeName)
                .coveredCount(bestCovered)
                .sumPrice(bestSumPrice)
                .build();
    }

    /* 내부 유틸 메서드 */
    // 판매 가능 여부 (품절 아님 + 재고 1 이상)
    private boolean isSellable(Product product) {
        // soldOut true면 재고 0 규칙이므로 둘 다 체크
        return Boolean.FALSE.equals(product.getSoldOut()) && product.getStock() != null && product.getStock() > 0;
    }

    // 상품 엔티티 -> 응답용 최소 정보
    private ProductSummaryDto toSummary(Product product) {
        return ProductSummaryDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .expiryDate(product.getExpiryDate() == null ? null : product.getExpiryDate().format(DATE))
                .stock(product.getStock())
                .soldOut(product.getSoldOut())
                .storeId(product.getStore().getId())
                .storeName(product.getStore().getStoreName())
                .imageUrl(product.getImageUrl())
                .build();
    }

    // 키워드 보정(동의어/표기 차이 대응)
    // - 필요시 확장 (ex: "계란" -> "달걀 동시 탐색 등)
    // - 현재는 있는 그대로 반환
    private String expandKeyword(String raw) {
        if (raw == null) return "";
        return raw.trim();
    }
}
