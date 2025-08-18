package me.swudam.jangbo.service;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.OrderProductResponseDto;
import me.swudam.jangbo.dto.OrderRequestDto;
import me.swudam.jangbo.dto.OrderResponseDto;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;

    // 기본 픽업 수수료
    private static final int BASE_DELIVERY_FEE = 800;
    private static final int STORE_ADDITIONAL_FEE = 500;
    private static final int MAX_DELIVERY_FEE = 2300;

    // ------------------------
    // 1. 고객 주문 생성
    // ------------------------
    @Transactional
    public OrderResponseDto createOrder(Long customerId, OrderRequestDto dto) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고객입니다."));

        Store store = storeRepository.findById(dto.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상점입니다."));

        List<OrderProduct> orderProducts = dto.getProducts().stream()
                .map(p -> {
                    Product product = productRepository.findById(p.getProductId())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

                    if (product.getSoldOut() || product.getStock() < p.getQuantity()) {
                        throw new OutOfStockException("재고가 부족합니다: " + product.getName());
                    }

                    product.setStock(product.getStock() - p.getQuantity());
                    if (product.getStock() == 0) product.setSoldOut(true);

                    OrderProduct op = new OrderProduct();
                    op.setProduct(product);
                    op.setQuantity(p.getQuantity());
                    op.setPrice(product.getPrice());
                    return op;
                })
                .toList();

        Order order = new Order();
        order.setCustomer(customer);
        order.setStore(store);
        order.setStatus(OrderStatus.REQUESTED);

        int fee = Math.min(BASE_DELIVERY_FEE + STORE_ADDITIONAL_FEE, MAX_DELIVERY_FEE);
        order.setDeliveryFee(fee);

        orderProducts.forEach(order::addOrderProduct);
        order.calculateTotalPrice();

        orderRepository.save(order);

        List<OrderProductResponseDto> productDtos = orderProducts.stream()
                .map(op -> new OrderProductResponseDto(
                        op.getProduct().getId(),
                        op.getProduct().getName(),
                        op.getPrice(),
                        op.getQuantity()))
                .toList();

        return new OrderResponseDto(order.getId(), order.getStatus(), order.getTotalPrice(), order.getDeliveryFee(), productDtos);
    }

    // ------------------------
    // 2. 고객 주문 취소
    // ------------------------
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 고객은 REQUESTED 상태에서만 취소 가능
        if (order.getStatus() != OrderStatus.REQUESTED) {
            throw new IllegalStateException("조리 중이거나 수락된 주문은 취소할 수 없습니다.");
        }

        order.setStatus(OrderStatus.CANCELED);

        // 재고 복원
        for (OrderProduct op : order.getOrderProducts()) {
            Product product = op.getProduct();
            product.setStock(product.getStock() + op.getQuantity());
            if (product.getStock() > 0) product.setSoldOut(false);
        }
    }

    // ------------------------
    // 3. 상인 주문 수락 + 준비시간 설정
    // ------------------------
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

        order.setStatus(OrderStatus.ACCEPTED);
        order.setPreparationTime(preparationTime);
    }

    // ------------------------
    // 4. 상인 주문 준비 완료
    // ------------------------
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

        order.setStatus(OrderStatus.READY);
    }

    // ------------------------
    // 5. 상인 주문 취소 + 취소 사유
    // ------------------------
    @Transactional
    public void cancelOrderByMerchant(Long merchantId, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

        if (!order.getStore().getMerchant().getId().equals(merchantId)) {
            throw new IllegalStateException("본인 상점의 주문만 취소할 수 있습니다.");
        }

        if (order.getStatus() != OrderStatus.REQUESTED) {
            throw new IllegalStateException("이미 처리된 주문은 취소 불가합니다.");
        }

        order.setStatus(OrderStatus.CANCELED);
        order.setCancelReason(reason);
    }

    // ------------------------
    // 6. 고객 기준 주문 목록 조회
    // ------------------------
    public List<OrderResponseDto> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::toDto)
                .toList();
    }

    // ------------------------
    // 7. 상인 기준 주문 목록 조회
    // ------------------------
    public List<OrderResponseDto> getOrdersByMerchant(Long merchantId) {
        return orderRepository.findByStore_Merchant_Id(merchantId).stream()
                .map(this::toDto)
                .toList();
    }

    // ------------------------
    // 8. 주문 상세 조회
    // ------------------------
    public OrderResponseDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        return toDto(order);
    }

    // ------------------------
    // Helper: Order → DTO 변환
    // ------------------------
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private OrderResponseDto toDto(Order order) {
        List<OrderProductResponseDto> products = order.getOrderProducts().stream()
                .map(op -> new OrderProductResponseDto(
                        op.getProduct().getId(),
                        op.getProduct().getName(),
                        op.getPrice(),
                        op.getQuantity()))
                .toList();

        Long remainingMinutes = 0L;
        if(order.getStatus() == OrderStatus.ACCEPTED || order.getStatus() == OrderStatus.PREPARING) {
            LocalDateTime readyTime = order.getAcceptedAt().plusMinutes(order.getPreparationTime());
            remainingMinutes = Math.max(Duration.between(LocalDateTime.now(), readyTime).toMinutes(), 0);
        }

        return new OrderResponseDto(
                order.getId(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getDeliveryFee(),
                order.getCreatedAt() != null ? order.getCreatedAt().format(FORMATTER) : null,
                order.getUpdatedAt() != null ? order.getUpdatedAt().format(FORMATTER) : null,
                order.getCancelReason(),
                products,
                remainingMinutes // 남은 시간 계산
        );
    }
