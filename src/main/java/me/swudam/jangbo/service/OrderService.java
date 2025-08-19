package me.swudam.jangbo.service;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.OrderProductResponseDto;
import me.swudam.jangbo.dto.OrderRequestDto;
import me.swudam.jangbo.dto.OrderResponseDto;
import me.swudam.jangbo.entity.*;
import me.swudam.jangbo.repository.CustomerRepository;
import me.swudam.jangbo.repository.MerchantRepository;
import me.swudam.jangbo.repository.OrderRepository;
import me.swudam.jangbo.repository.ProductRepository;
import me.swudam.jangbo.repository.StoreRepository;
import me.swudam.jangbo.support.OutOfStockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final MerchantRepository merchantRepository;

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
    public OrderResponseDto createOrder(Long customerId, OrderRequestDto dto) {
        // 1) 고객 확인
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고객입니다."));

        // 2) 상점 확인
        Store store = storeRepository.findById(dto.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상점입니다."));

        // 3) 주문 상품 처리
        List<OrderProduct> orderProducts = dto.getProducts().stream().map(p -> {
            Product product = productRepository.findById(p.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

            if (product.getSoldOut() || product.getStock() < p.getQuantity()) {
                throw new OutOfStockException("재고가 부족합니다: " + product.getName());
            }

            // 재고 감소 및 품절 처리
            int remainingStock = product.getStock() - p.getQuantity(); // 남은 재고 계산
            if (remainingStock <= 0) {
                product.markSoldOut(); // 품절 처리
            } else {
                product.updateProduct(
                        product.getName(),
                        product.getOrigin(),
                        product.getExpiryDate(),
                        product.getPrice(),
                        remainingStock,
                        product.getImageUrl()
                ); // 재고 업데이트
            }

            // OrderProduct 객체 생성 (주문 당시 가격과 수량 스냅샷)
            OrderProduct op = new OrderProduct();
            op.setProduct(product); // 주문 상품에 상품 정보 설정
            op.setQuantity(p.getQuantity()); // 주문 수량 설정
            op.setPrice(product.getPrice()); // 주문 당시 가격 설정
            return op;
        }).toList();

        // 4) 주문 생성
        Order order = new Order();
        order.setCustomer(customer); // 주문 고객 설정
        order.setStore(store); // 주문 상점 설정
        order.setStatus(OrderStatus.REQUESTED); // 주문 상태 초기화

        // 배송비 계산 → 추후 수정 가능성
        int fee = Math.min(BASE_DELIVERY_FEE + STORE_ADDITIONAL_FEE, MAX_DELIVERY_FEE);
        order.setDeliveryFee(fee); // 배송비 설정

        orderProducts.forEach(order::addOrderProduct); // 주문 상품 추가
        order.calculateTotalPrice(); // 총 금액 계산

        orderRepository.save(order); // DB 저장

        // 5) DTO 변환
        List<OrderProductResponseDto> productDtos = orderProducts.stream()
                .map(op -> new OrderProductResponseDto(
                        op.getProduct().getId(),
                        op.getProduct().getName(),
                        op.getPrice(),
                        op.getQuantity()))
                .toList();

        long remainingMinutes = 0L; // 주문 생성 시점에는 0 - 주문 생성 시점 남은 시간

        return new OrderResponseDto(
                order.getId(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getDeliveryFee(),
                order.getCreatedAt() != null ? order.getCreatedAt().format(FORMATTER) : null, // 주문 생성일
                order.getCancelReason(), // 취소 사유
                productDtos, // 주문 상품 DTO
                Long.valueOf(remainingMinutes), // 남은 시간
                order.getUpdatedAt() != null ? order.getUpdatedAt().format(FORMATTER) : null // 업데이트 시간
        );
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

        Long remainingMinutes = 0L; // 남은 준비 시간 초기화
        if (order.getStatus() == OrderStatus.ACCEPTED || order.getStatus() == OrderStatus.PREPARING) {
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
                order.getUpdatedAt() != null ? order.getUpdatedAt().format(FORMATTER) : null // updatedAt
        );
    }
}
