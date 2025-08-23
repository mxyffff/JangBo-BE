package me.swudam.jangbo.service;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.order.OrderProductResponseDto;
import me.swudam.jangbo.dto.order.OrderRequestDto;
import me.swudam.jangbo.dto.order.OrderResponseDto;
import me.swudam.jangbo.dto.PickupCounterResponseDto;
import me.swudam.jangbo.entity.*;
import me.swudam.jangbo.repository.CustomerRepository;
import me.swudam.jangbo.repository.OrderRepository;
import me.swudam.jangbo.repository.ProductRepository;
import me.swudam.jangbo.repository.StoreRepository;
import me.swudam.jangbo.support.OutOfStockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/*
    ORDER + PICKUP
*/
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;

    // 픽업대 관련 상수
    private static final int MAX_PICKUP_SLOT = 10; // 픽업대 최대 개수

    // 배송비 관련 상수
    private static final int BASE_DELIVERY_FEE = 800; // [기본 배송비]
    private static final int STORE_ADDITIONAL_FEE = 500; // [상점 추가 배송비]
    private static final int MAX_DELIVERY_FEE = 2300; // [최대 배송비]

    // 날짜 포맷
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /*
     * [1] 고객 주문 생성
     * - 고객, 상점 존재 확인
     * - 주문 상품 처리: 재고 확인, 재고 감소, 품절 처리
     * - Order 생성 및 DB 저장
     * - OrderResponseDto 변환 후 반환
     */
    @Transactional
    public List<OrderResponseDto> createOrders(Long customerId, OrderRequestDto orderRequestDto, int oneTimePickupFee) {
        if (orderRequestDto.getStoreOrders() == null || orderRequestDto.getStoreOrders().isEmpty()) {
            throw new IllegalArgumentException("주문할 상점 정보가 없습니다.");
        }

        // 1) 고객 확인
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고객입니다."));

        List<OrderResponseDto> result = new ArrayList<>();
        boolean feeApplied = false; // 첫 주문에만 수수료 적용

        for (OrderRequestDto.StoreOrderDto storeOrder : orderRequestDto.getStoreOrders()) {
            Store store = storeRepository.findById(storeOrder.getStoreId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상점입니다."));

            // 상품 → OrderProduct 변환 + 재고 차감/품절 처리
            List<OrderProduct> orderProducts = storeOrder.getProducts().stream().map(p -> {
                Product product = productRepository.findById(p.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
                if (product.getSoldOut() || product.getStock() < p.getQuantity()) {
                    throw new OutOfStockException("재고가 부족합니다: " + product.getName());
                }

                int remainingStock = product.getStock() - p.getQuantity();
                if (remainingStock <= 0) product.markSoldOut();
                else product.updateProduct(product.getName(), product.getOrigin(), product.getExpiryDate(),
                        product.getPrice(), remainingStock, product.getImageUrl());

                OrderProduct op = new OrderProduct();
                op.setProduct(product);
                op.setQuantity(p.getQuantity());
                op.setPrice(product.getPrice());
                return op;
            }).toList();

            // 주문 생성
            Order order = new Order();
            order.setCustomer(customer);
            order.setStore(store);
            order.setStatus(OrderStatus.REQUESTED);

            // ★ 수수료 한 번만 적용
            if (!feeApplied) {
                order.setDeliveryFee(oneTimePickupFee);
                feeApplied = true;
            } else {
                order.setDeliveryFee(0);
            }

            orderProducts.forEach(order::addOrderProduct);
            order.calculateTotalPrice();

            // 픽업대 배정
            int pickupSlot = assignPickupSlot(store);
            order.setPickupSlot(pickupSlot);

            orderRepository.save(order);
            result.add(toDto(order));
        }
        return result;
    }

    // 픽업대 번호 배정
    private int assignPickupSlot(Store store) {
        // 현재 상점의 주문 중 비어있지 않은 슬롯 조회
        List<Integer> usedSlots = orderRepository.findPickupSlotsByStoreId(store.getId());

        for (int i = 1; i <= MAX_PICKUP_SLOT; i++) {
            if (!usedSlots.contains(i)) {
                return i; // 가장 작은 번호부터 배정
            }
        }
        // 전부 차있으면 예외
        throw new IllegalStateException("모든 픽업대가 사용 중입니다. 주문을 받을 수 없습니다.");
    }

    /*
     * [2] 고객 주문 취소
     * - 주문 존재 확인
     * - 주문 상태 확인 후 취소 가능 여부 판단
     * - 주문 상태를 CANCELED로 변경
     * - 주문 상품 재고 복원
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if (!order.getStatus().canBeCanceledByCustomer()) {
            throw new IllegalStateException("수락된 주문은 취소할 수 없습니다.");
        }

        order.setStatus(OrderStatus.CANCELED); // 주문 취소 상태로 변경
        order.setPickupSlot(null);

        // 재고 복원
        for (OrderProduct op : order.getOrderProducts()) {
            Product product = op.getProduct();
            int restoredStock = product.getStock() + op.getQuantity(); // 재고 복원
            product.updateProduct(
                    product.getName(),
                    product.getOrigin(),
                    product.getExpiryDate(),
                    product.getPrice(),
                    restoredStock,
                    product.getImageUrl()
            );
        }
    }

    /*
     * [3] 상인 주문 수락 및 준비시간 설정
     * - 주문 존재 확인
     * - 상인 권한 확인
     * - 주문 상태 확인 후 ACCEPTED로 변경
     * - 준비 시간 및 수락 시간 설정
     */
    @Transactional
    public void acceptOrder(Long merchantId, Long orderId, Integer preparationTime) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

        if (!order.getStore().getMerchant().getId().equals(merchantId)) {
            throw new IllegalStateException("본인 상점의 주문만 처리할 수 있습니다.");
        }

        if (order.getStatus() != OrderStatus.REQUESTED) {
            throw new IllegalStateException("이미 처리된 주문입니다.");
        }

        order.setStatus(OrderStatus.ACCEPTED); // 주문 상태 수락으로 변경
        order.setPreparationTime(preparationTime); // 준비시간 설정
        order.setAcceptedAt(LocalDateTime.now()); // 수락 시간 기록
    }

    /*
     * [4] 상인 주문 준비 완료
     * - 주문 존재 확인
     * - 상인 권한 확인
     * - 준비 중인 주문만 READY로 변경
     */
    @Transactional
    public void markOrderReady(Long merchantId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

        if (!order.getStore().getMerchant().getId().equals(merchantId)) {
            throw new IllegalStateException("본인 상점의 주문만 처리할 수 있습니다.");
        }

        if (order.getStatus() != OrderStatus.ACCEPTED && order.getStatus() != OrderStatus.PREPARING) {
            throw new IllegalStateException("준비 중인 주문만 완료 처리 가능합니다.");
        }

        order.setStatus(OrderStatus.READY); // 주문 상태 준비로 변경
    }

    /*
     * [5] 상인 주문 취소 + 취소 사유
     * - 주문 존재 확인
     * - 상인 권한 확인
     * - 주문 상태 확인 후 CANCELED로 변경
     * - 취소 사유 기록
     */
    @Transactional
    public void cancelOrderByMerchant(Long merchantId, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

        if (!order.getStore().getMerchant().getId().equals(merchantId)) {
            throw new IllegalStateException("본인 상점의 주문만 취소할 수 있습니다.");
        }

        if (!order.getStatus().canBeCanceledByMerchant()) {
            throw new IllegalStateException("이미 처리된 주문은 취소 불가합니다.");
        }

        order.setStatus(OrderStatus.CANCELED); // 주문 상태 취소로 변경
        order.setCancelReason(reason); // 취소 사유 기록
    }

    /*
     * [6] 고객 기준 주문 목록 조회
     * - 고객 ID로 주문 조회 후 DTO 변환
     */
    public List<OrderResponseDto> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::toDto)
                .toList();
    }

    /*
     * [7] 상인 기준 주문 목록 조회
     * - 상인 ID로 주문 조회 후 DTO 변환
     */
    public List<OrderResponseDto> getOrdersByMerchant(Long merchantId) {
        return orderRepository.findByStore_Merchant_Id(merchantId).stream()
                .map(this::toDto)
                .toList();
    }

    /*
     * [8] 주문 상세 조회
     * - 주문 ID로 조회 후 DTO 변환
     */
    public OrderResponseDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        return toDto(order);
    }

    /*
     * [9] 고객 픽업 완료 처리
     * - 완료 시 픽업대 번호 초기화
     */
    @Transactional
    public void completePickup(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        if (order.getStatus() != OrderStatus.READY) {
            throw new IllegalStateException("준비 완료된 주문만 픽업할 수 있습니다.");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setPickupSlot(null); // 픽업대 해제
    }

    /*
     * [10] PUBLIC - 특정 픽업대 현황 조회
     * - 특정 픽업대 + 주문 조회
     */
    public List<PickupCounterResponseDto> getCountersByStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상점입니다."));
        List<PickupCounterResponseDto> counters = new ArrayList<>();

        // 1~10번 픽업대 체크
        for (int i = 1; i <= MAX_PICKUP_SLOT; i++) {
            // 해당 슬롯에 주문 있는지 조회 (CANCELED, COMPLETED 제외)
            Order order = orderRepository.findByPickupSlotAndStoreId(i, storeId)
                    .orElse(null);
            counters.add(PickupCounterResponseDto.builder()
                    .counterNumber(i)
                    .order(order != null ? toDto(order) : null)
                    .build());
        }
        return counters;
    }

    /*
     * [11] PUBLIC - 모든 픽업대 현황 조회
     * - 1~10번 픽업대 + 주문 조회
     */
    public Map<Long, List<PickupCounterResponseDto>> getAllStoresCounters() {
        Map<Long, List<PickupCounterResponseDto>> result = new HashMap<>();
        List<Store> stores = storeRepository.findAll();

        for (Store store : stores) {
            List<PickupCounterResponseDto> counters = new ArrayList<>();
            for (int i = 1; i <= MAX_PICKUP_SLOT; i++) {
                Order order = orderRepository.findByPickupSlotAndStoreId(i, store.getId()).orElse(null);
                counters.add(PickupCounterResponseDto.builder()
                        .counterNumber(i)
                        .order(order != null ? toDto(order) : null)
                        .build());
            }
            result.put(store.getId(), counters);
        }
        return result;
    }

    /*
     * [Helper] Order → OrderResponseDto 변환
     * - 주문 상품 리스트 변환
     * - 주문 상태에 따라 남은 준비 시간 계산
     * - DTO 생성 및 반환
     */
    private OrderResponseDto toDto(Order order) {
        List<OrderProductResponseDto> products = order.getOrderProducts().stream()
                .map(op -> new OrderProductResponseDto(
                        op.getProduct().getId(),
                        op.getProduct().getName(),
                        op.getPrice(),
                        op.getQuantity()))
                .toList(); // 주문 상품 DTO 반환

        // 남은 준비시간/수락시간
        Long remainingMinutes = 0L;
        // 준비시간 및 수락시간 모두 null이 아닌 경우만 남은 시간 계산
        if ((order.getStatus() == OrderStatus.ACCEPTED || order.getStatus() == OrderStatus.PREPARING)
                && order.getPreparationTime() != null && order.getAcceptedAt() != null) {
            LocalDateTime readyTime = order.getAcceptedAt().plusMinutes(order.getPreparationTime());
            remainingMinutes = Math.max(Duration.between(LocalDateTime.now(), readyTime).toMinutes(), 0);
        }
        return new OrderResponseDto(
                order.getId(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getDeliveryFee(),
                order.getCreatedAt() != null ? order.getCreatedAt().format(FORMATTER) : null, // orderDate
                order.getCancelReason(), // 취소 사유
                products, // 주문 상품 DTO
                remainingMinutes, // 남은 준비 시간
                order.getUpdatedAt() != null ? order.getUpdatedAt().format(FORMATTER) : null, // updatedAt
                order.getPickupSlot()
        );
    }

    /*
     * 실제 엔티티 반환용
     * - 고객 컨트롤러에서 본인 주문 체크 시 사용
     */
    public Order getOrderByIdEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
    }
}