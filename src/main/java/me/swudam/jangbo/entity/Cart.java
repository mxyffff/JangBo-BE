package me.swudam.jangbo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// 고객별 장바구니 (활성 카드 1개 가정)
// - 고객과 1:1로 매핑 (UNIQUE)
// - 아이템은 CartItem에 저장 (CASCADE + ORPHAN)
@Entity
@Table(name = "carts",
        uniqueConstraints = {
                // 고객당 활성 카드 1개만 허용 (status = ACTIVE일 때)
                @UniqueConstraint(name = "uk_cart_customer_active", columnNames = "customer_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Cart extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long id;

    // 장바구니 소유 고객 (1:1)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // 장바구니 항목들 (setter 없이 add/remove로만 관리)
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    // 상품 추가 (같은 상품 있으면 수량 합치기)
    public CartItem addItem(Product product, int qty) {
        if (qty < 1) throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");

        // 이미 담긴 상품이면 수량만 증가
        for (CartItem item : items) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.changeQuantity(item.getQuantity() + qty);
                return item;
            }
        }

        // 새 줄 추가
        CartItem newItem = CartItem.builder()
                .product(product)
                .store(product.getStore())
                .quantity(qty)
                .build();

        // 양방향 연결 (Setter는 PACKAGE 범위)
        newItem.setCart(this);
        items.add(newItem);
        return newItem;
    }

    // 특정 항목 수량 변경
    public void changeQuantity(Long itemId, int qty) {
        if (qty < 1) throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        CartItem item = items.stream()
                .filter(i -> Objects.equals(i.getId(), itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("장바구니 항목을 찾을 수 없습니다."));
        item.changeQuantity(qty);
    }

    // 항목 제거
    public void removeItem(Long itemId) {
        items.removeIf(i -> Objects.equals(i.getId(), itemId));
    }

    // 전체 비우기
    public void clear() {
        items.clear();
    }

    // 장바구니 전체 합계
    public int subtotalAll() {
        return items.stream().mapToInt(CartItem::lineTotal).sum();
    }

    // 선택된 itemId만 합계
    public int subtotalSelected(Set<Long> selectedItemIds) {
        if (selectedItemIds == null || selectedItemIds.isEmpty()) return 0;
        return items.stream()
                .filter(i -> selectedItemIds.contains(i.getId()))
                .mapToInt(CartItem::lineTotal)
                .sum();
    }

    // 선택한 itemId 기준 픽업 수수료 계산 (최대 2300원)
    public int pickupFeeForSelected(Set<Long> selectedItemIds) {
        if (selectedItemIds == null || selectedItemIds.isEmpty()) return 0;

        long distinctStoreCount = items.stream()
                .filter(i -> selectedItemIds.contains(i.getId()))
                .map(i -> i.getStore().getId())
                .distinct()
                .count();
        if (distinctStoreCount == 0) return 0;

        int base = 800;
        int extraPerStore = 500;
        int max = 2300;

        int fee = base + (int) Math.max(0, (distinctStoreCount - 1)) * extraPerStore;
        return Math.min(fee, max);
    }

    // 선택한 itemId 총 결제 금액 (상품합계 + 수수료)
    public int totalForSelected(Set<Long> selectedItemIds) {
        return subtotalSelected(selectedItemIds) + pickupFeeForSelected(selectedItemIds);
    }
}
