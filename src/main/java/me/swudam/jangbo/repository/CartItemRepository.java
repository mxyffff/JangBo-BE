package me.swudam.jangbo.repository;

import me.swudam.jangbo.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// 장바구니 항목 CartItem 전용 레포지토리
// - 동일 상품이 이미 담겨있는지 검사
// - 카트의 모든 아이템/선택 아이템을 product, store까지 묶어서 가져오기
// - 선택 삭제/비우기 등 일괄 삭제
// - DB에서 바로 상점 개수 count
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Cart 안에 같은 상품이 이미 담겨있는지 검사할 때 사용
    Optional<CartItem> findByCart_IdAndProduct_Id(Long cartId, Long productId);

    // Cart의 모든 아이템을 product/store까지 함께 로딩
    @EntityGraph(attributePaths = {"product", "store"})
    List<CartItem> findAllByCart_Id(Long cartId);

    // 선택된 itemId 목록만 로딩 (product/store 포함)
    @EntityGraph(attributePaths = {"product", "store"})
    List<CartItem> findAllByCart_IdAndIdIn(Long cartId, Collection<Long> itemIds);

    // 카트 비우기
    void deleteByCart_Id(Long cartId);

    // 선택 항목만 일괄 삭제
    void deleteByCart_IdAndIdIn(Long cartId, Collection<Long> itemIds);

    // DB에서 바로 상점 개수를 세고 싶을 때 사용
    @Query("select count(distinct i.store.id) " +
            "from CartItem i " +
            "where i.cart.id = :cartId and i.id in :itemIds")
    long countDistinctStoreInSelected(Long cartId, Collection<Long> itemIds);
}
