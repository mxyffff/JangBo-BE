package me.swudam.jangbo;

import me.swudam.jangbo.dto.*;
import me.swudam.jangbo.dto.cart.AddToCartRequestDto;
import me.swudam.jangbo.dto.cart.AddToCartResponseDto;
import me.swudam.jangbo.dto.cart.CartSummaryResponseDto;
import me.swudam.jangbo.dto.order.OrderRequestDto;
import me.swudam.jangbo.dto.order.OrderResponseDto;
import me.swudam.jangbo.entity.*;
import me.swudam.jangbo.entity.DayOff;
import me.swudam.jangbo.repository.StoreRepository;
import me.swudam.jangbo.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class CartToPickupFlowTest {

    @Autowired MerchantService merchantService;
    @Autowired CustomerService customerService;
    @Autowired StoreService storeService;
    @Autowired ProductService productService;
    @Autowired CartService cartService;
    @Autowired OrderService orderService;
    @Autowired PaymentService paymentService;
    @Autowired StoreRepository storeRepository;

    @MockBean // 에러 무시하셔도 됩니다!
    EmailVerificationService emailVerificationService;

    @Test
    void fullFlowTest() {
        // 이메일 인증 무조건 true 반환 (인증 완료 상태 시뮬레이션)
        when(emailVerificationService.isVerified(anyString())).thenReturn(true);

        // 1. 상인 회원가입
        MerchantSignupRequestDto merchantDto = new MerchantSignupRequestDto();
        merchantDto.setEmail("MerchantForServiceTest@test.com");
        merchantDto.setUsername("MerchantForServiceTest");
        merchantDto.setPassword("password!");
        Merchant merchant = merchantService.signup(merchantDto);
        assertThat(merchant).isNotNull();

        // 2. 상점 등록 (MultipartFile 없는 상태로 저장)
        StoreFormDto storeFormDto = new StoreFormDto();
        storeFormDto.setStoreName("테스트상점 - ServiceTest");
        storeFormDto.setStoreAddress("테스트시 테스트구");
        storeFormDto.setStorePhoneNumber("02-9999-9999");
        storeFormDto.setDayOff(new ArrayList<>(Set.of(DayOff.MONDAY)));
        storeFormDto.setCategory(Category.과일);
        storeFormDto.setOpenTime(LocalTime.of(9, 0));
        storeFormDto.setCloseTime(LocalTime.of(18, 0));

        Long storeId = storeService.saveStore(storeFormDto, merchant, null);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("상점이 없습니다."));

        // 3. 상품 등록
        ProductCreateRequestDto productDto = new ProductCreateRequestDto();
        productDto.setName("테스트사과 - ServiceTest");
        productDto.setPrice(10000);
        productDto.setStock(20);
        productDto.setImageUrl(null);
        productDto.setOrigin("국내산");
        productDto.setExpiryDate(LocalDate.now().plusDays(7));

        Product product = productService.create(merchant.getId(), productDto);

        // 4. 고객 회원가입
        CustomerSignupRequestDto customerDto = new CustomerSignupRequestDto();
        customerDto.setEmail("CustomerForServiceTest@test.com");
        customerDto.setUsername("CustomerForServiceTest");
        customerDto.setPassword("password!");
        Customer customer = customerService.signup(customerDto);
        assertThat(customer).isNotNull();

        // 5. 장바구니에 상품 담기
        AddToCartRequestDto addToCartRequest = new AddToCartRequestDto();
        addToCartRequest.setProductId(product.getId());
        addToCartRequest.setQuantity(3);
        AddToCartResponseDto addToCartResponse = cartService.addToCart(customer.getId(), addToCartRequest);
        assertThat(addToCartResponse.getQuantity()).isEqualTo(3);

        // 6. 장바구니 내용 확인
        CartSummaryResponseDto cartSummary = cartService.getCartDetail(customer.getId());
        assertEquals(1, cartSummary.getSelectedStoreCount());
        assertEquals(3 * product.getPrice(), cartSummary.getSubtotal());

        // 7. 주문 생성
        OrderRequestDto orderRequestDto = cartService.buildOrderRequestFromSelection(customer.getId(), null);
        int oneTimePickupFee = 800;
        List<OrderResponseDto> orders = orderService.createOrders(customer.getId(), orderRequestDto, oneTimePickupFee);

        assertFalse(orders.isEmpty());
        OrderResponseDto orderResponse = orders.get(0);
        assertEquals(OrderStatus.REQUESTED, orderResponse.getStatus());

        // 8. 결제 요청 (주문 생성 후)
        paymentService.requestPayment(orderResponse.getOrderId());

        // 9. 상인 주문 수락 + 준비 시간 설정
        orderService.acceptOrder(merchant.getId(), orderResponse.getOrderId(), 30);

        // 10. 상인 주문 준비 완료 처리
        orderService.markOrderReady(merchant.getId(), orderResponse.getOrderId());

        OrderResponseDto readyOrder = orderService.getOrderById(orderResponse.getOrderId());
        assertEquals(OrderStatus.READY, readyOrder.getStatus());

        // 11. 고객 픽업 완료 처리
        orderService.completePickup(orderResponse.getOrderId());

        OrderResponseDto completedOrder = orderService.getOrderById(orderResponse.getOrderId());
        assertEquals(OrderStatus.COMPLETED, completedOrder.getStatus());
    }
}
