package me.swudam.jangbo.service;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.order.OrderRequestDto;
import me.swudam.jangbo.dto.cart.*;
import me.swudam.jangbo.entity.*;
import me.swudam.jangbo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    /* 생성/조회 */
    // 고객의 장바구니(아이템 포함)를 조회, 없으면 빈 장바구니를 생성
    // items/product/store를 @EntityGraph로 함께 로딩
    @Transactional
    public Cart getOrCreateCart(Long customerId) {
        return cartRepository.findWithItemsByCustomer_Id(customerId)
                .orElseGet(() -> createEmptyCart(customerId));
    }

    // 장바구니 전체 상세 조회
    // 초기 장바구니 화면
    @Transactional
    public CartSummaryResponseDto getCartDetail(Long customerId) {
        Cart cart = getOrCreateCart(customerId);
        List<CartItem> items = cartItemRepository.findAllByCart_Id(cart.getId()); // product/store 동시 로딩

        // 합계/수수료/총액 계산 (전체 기준)
        int subtotal = calcSubtotal(items);
        long distinctStoreCount = countDistinctStore(items);
        int pickupFee = calcPickupFee(distinctStoreCount);
        int total = subtotal + pickupFee;

        // 응답 DTO 구성 (전체 항목)
        List<CartItemResponseDto> itemDtos = items.stream()
                .map(this::toItemDto)
                .toList();

        return CartSummaryResponseDto.builder()
                .items(itemDtos)
                .selectedItemCount(itemDtos.size())
                .selectedStoreCount((int) distinctStoreCount)
                .subtotal(subtotal)
                .pickupFee(pickupFee)
                .total(total)
                .build();
    }

    // 선택 항목 기준 합계/수수료/총액 요약
    @Transactional
    public CartSummaryResponseDto getSelectionSummary(Long customerId, Collection<Long> selectedItemIds) {
        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            // 선택이 없으면 0 세팅
            return CartSummaryResponseDto.builder()
                    .items(List.of())
                    .selectedItemCount(0)
                    .selectedStoreCount(0)
                    .subtotal(0)
                    .pickupFee(0)
                    .total(0)
                    .build();
        }

        Cart cart = cartRepository.findByCustomer_Id(customerId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 없습니다."));

        // 선택 항목만 로딩 (product/store 포함)
        List<CartItem> selected = cartItemRepository.findAllByCart_IdAndIdIn(cart.getId(), selectedItemIds);
        // (보안) 본인 카트의 항목만 남기기
        selected = selected.stream()
                .filter(ci -> Objects.equals(ci.getCart().getId(), cart.getId()))
                .toList();

        int subtotal = calcSubtotal(selected);
        // 성능: DB로 상점 수 카운트
        long distinctStoreCount = cartItemRepository.countDistinctStoreInSelected(cart.getId(), selectedItemIds);
        int pickupFee = calcPickupFee(distinctStoreCount);
        int total = subtotal + pickupFee;

        List<CartItemResponseDto> itemDtos = selected.stream()
                .map(this::toItemDto)
                .toList();

        return CartSummaryResponseDto.builder()
                .items(itemDtos)
                .selectedItemCount(itemDtos.size())
                .selectedStoreCount((int) distinctStoreCount)
                .subtotal(subtotal)
                .pickupFee(pickupFee)
                .total(total)
                .build();
    }

    /* 변경 (담기/수정/삭제) */
    // 상품 담기 (이미 담긴 상품이면 수량만 증가)
    @Transactional
    public AddToCartResponseDto addToCart(Long customerId, AddToCartRequestDto req) {
        if (req == null || req.getProductId() == null) {
            throw new IllegalArgumentException("상품 Id는 필수입ㄴ디ㅏ.");
        }
        int addQty = Math.max(1, req.getQuantity());

        // Cart 잠금 (동시성 제어)
        Cart cart = cartRepository.findByCustomerIdForUpdate(customerId)
                .orElseGet(() -> createEmptyCart(customerId));

        // 상품/상점 조회
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        Store store = product.getStore(); // 요청 DTO에 storeID가 없으므로 상품에서 추출

        // 이미 담긴 상품인지 검사
        Optional<CartItem> existingOpt = cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), product.getId());
        CartItem target;
        int finalQty;

        if (existingOpt.isPresent()) {
            // 수량 증가
            target = existingOpt.get();
            finalQty = target.getQuantity() + addQty;
            target.changeQuantity(finalQty);
        } else {
            // 새 줄 추가
            target = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .store(store)
                    .quantity(addQty)
                    .build();
            cart.getItems().add(target);
            cartItemRepository.save(target);
            finalQty = addQty;
        }

        return AddToCartResponseDto.builder()
                .cartId(cart.getId())
                .itemId(target.getId())
                .quantity(finalQty)
                .message("장바구니에 담았습니다.")
                .build();
    }

    // 수량 지정 변경
    @Transactional
    public UpdateQuantityResponseDto updateQuantity(Long customerId, UpdateQuantityRequestDto req) {
        if (req == null || req.getItemId() == null) {
            throw new IllegalArgumentException("장바구니 항목 Id는 필수입니다.");
        }
        if (req.getQuantity() < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }

        Cart cart = cartRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 없습니다."));
        CartItem item = cartItemRepository.findById(req.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("장바구니 항목을 찾을 수 없습니다."));

        verifyOwnerShip(cart, item);

        item.changeQuantity(req.getQuantity());

        return UpdateQuantityResponseDto.builder()
                .itemId(item.getId())
                .quantity(item.getQuantity())
                .message("수량을 " + item.getQuantity() + "개로 변경했습니다.")
                .build();
    }

    // 수량 증감 변경
    @Transactional
    public UpdateQuantityResponseDto changeQuantityByDelta(Long customerId, Long itemId, int delta) {
        Cart cart = cartRepository.findByCustomerIdForUpdate(customerId)  // 원하면 락
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 없습니다."));
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("항목을 찾을 수 없습니다."));

        int next = item.getQuantity() + delta;
        if (next < 1) throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        item.changeQuantity(next);
        return UpdateQuantityResponseDto.builder().itemId(itemId).quantity(next).message("OK").build();
    }

    // 선택 항목 주문하기
    @Transactional(readOnly = true)
    public OrderRequestDto buildOrderRequestFromSelection(Long customerId, Collection<Long> selectedItemIds) {
        // 1. 내 장바구니 로딩
        Cart cart = cartRepository.findWithItemsByCustomer_Id(customerId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 없습니다."));
        // 2. 결제 대상 아이템 목록 결정 (선택 없으면 전체)
        final List<CartItem> targets;
        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            targets = cartItemRepository.findAllByCart_Id(cart.getId());
        } else {
            targets = cartItemRepository.findAllByCart_IdAndIdIn(cart.getId(), selectedItemIds).stream()
                    // 보안: 내 장바구니 항목만
                    .filter(ci -> Objects.equals(ci.getCart().getId(), cart.getId()))
                    .toList();
        }
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("주문할 항목이 없습니다.");
        }

        // 3. 상점별 그룹핑 -> OrderRequestDto.StoreOrderDto 리스트 구성
        Map<Long, List<CartItem>> byStore = targets.stream()
                .collect(Collectors.groupingBy(ci -> ci.getStore().getId()));

        List<OrderRequestDto.StoreOrderDto> storeOrderDtos = new ArrayList<>();

        for (Map.Entry<Long, List<CartItem>> entry : byStore.entrySet()) {
            Long storeId = entry.getKey();
            List<CartItem> items = entry.getValue();

            OrderRequestDto.StoreOrderDto sod = new OrderRequestDto.StoreOrderDto();
            sod.setStoreId(storeId);

            List<OrderRequestDto.ProductOrderDto> productOrderDtos = new ArrayList<>();
            for (CartItem ci : items) {
                OrderRequestDto.ProductOrderDto pd = new OrderRequestDto.ProductOrderDto();
                pd.setProductId(ci.getProduct().getId());
                pd.setQuantity(ci.getQuantity());
                productOrderDtos.add(pd);
            }
            sod.setProducts(productOrderDtos);
            storeOrderDtos.add(sod);
        }

        // 4) OrderRequestDto 만들어 채우고 반환
        OrderRequestDto req = new OrderRequestDto();
        req.setStoreOrders(storeOrderDtos);
        return req;
    }

    // 단일 항목 삭제
    @Transactional
    public DeleteItemsResponseDto removeOne(Long customerId, Long cartItemId) {
        Cart cart = cartRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 없습니다."));
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 항목을 찾을 수 없습니다."));

        verifyOwnerShip(cart, item);

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        return DeleteItemsResponseDto.builder()
                .deletedCount(1)
                .message("해당 항목을 삭제했습니다.")
                .build();
    }

    // 선택 항목 일괄 삭제
    @Transactional
    public DeleteItemsResponseDto removeSelected(Long customerId, RemoveItemsRequestDto req) {
        List<Long> itemIds = (req == null || req.getItemIds() == null) ? List.of() : req.getItemIds();
        if (itemIds.isEmpty()) {
            return DeleteItemsResponseDto.builder()
                    .deletedCount(0)
                    .message("삭제할 항목이 없습니다.")
                    .build();
        }

        Cart cart = cartRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 없습니다."));

        // 본인 카트 항목만 삭제하도록 1차 필터링
        List<CartItem> targets = cartItemRepository.findAllByCart_IdAndIdIn(cart.getId(), itemIds).stream()
                .filter(ci -> Objects.equals(ci.getCart().getId(), cart.getId()))
                .toList();

        if (targets.isEmpty()) {
            return DeleteItemsResponseDto.builder()
                    .deletedCount(0)
                    .message("삭제할 항목이 없습니다.")
                    .build();
        }

        // DB 삭제
        cartItemRepository.deleteByCart_IdAndIdIn(cart.getId(),
                targets.stream().map(CartItem::getId).toList());
        // 메모리 컬렉션에서도 제거
        Set<Long> toRemove = targets.stream().map(CartItem::getId).collect(Collectors.toSet());
        cart.getItems().removeIf(i -> toRemove.contains(i.getId()));

        return DeleteItemsResponseDto.builder()
                .deletedCount(targets.size())
                .message("선택한 " + targets.size() + "개 항목을 삭제했습니다.")
                .build();
    }

    // 장바구니 비우기 (모든 항목 삭제)
    @Transactional
    public DeleteItemsResponseDto clearCart(Long customerId) {
        Cart cart = cartRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 없습니다."));
        int size = cart.getItems().size();

        cartItemRepository.deleteByCart_Id(cart.getId());
        cart.getItems().clear();

        return DeleteItemsResponseDto.builder()
                .deletedCount(size)
                .message("장바구니를 비웠습니다.")
                .build();
    }

    /* 내부 유틸 메서드 */
    // 빈 카드 생성 (고객 존재 검증 포함)
    private Cart createEmptyCart(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("고객을 찾을 수 없습니다."));
        Cart cart = Cart.builder()
                .customer(customer)
                .build();
        return cartRepository.save(cart);
    }

    // 본인 카드 소유 항목인지 검증
    private void verifyOwnerShip(Cart cart, CartItem item) {
        if (!Objects.equals(item.getCart().getId(), cart.getId())) {
            throw new IllegalArgumentException("본인의 장바구니 항목이 아닙니다.");
        }
    }

    // 소계 (실시간 가격 * 수량)
    private int calcSubtotal(List<CartItem> items) {
        int sum = 0;
        for (CartItem i : items) {
            int unitPrice = i.getProduct().getPrice(); // 실시간 가격
            sum += unitPrice * i.getQuantity();
        }

        return sum;
    }

    // 상점 수 (storeId로 구분)
    private long countDistinctStore(List<CartItem> items) {
        return items.stream().map(ci -> ci.getStore().getId()).distinct().count();
    }

    // 픽업 수수료 규칙
    private int calcPickupFee(long distinctStroeCount) {
        if (distinctStroeCount <= 0) return 0;
        int fee = 800 + (int) Math.max(0, distinctStroeCount - 1) * 500;
        return Math.min(fee, 2300);
    }

    private CartItemResponseDto toItemDto(CartItem item) {
        int unitPrice = item.getProduct().getPrice(); // 실시간 가격
        int lineTotal = unitPrice * item.getQuantity();

        return CartItemResponseDto.builder()
                .itemId(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .storeId(item.getStore().getId())
                .storeName(item.getStore().getStoreName())
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .imageUrl(item.getProduct().getImageUrl())
                .build();
    }
}
