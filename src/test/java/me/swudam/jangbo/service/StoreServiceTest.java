//package me.swudam.jangbo.service;
//
//import me.swudam.jangbo.dto.StoreFormDto;
//import me.swudam.jangbo.entity.Category;
//import me.swudam.jangbo.entity.DayOff;
//import me.swudam.jangbo.entity.Merchant;
//import me.swudam.jangbo.entity.Store;
//import me.swudam.jangbo.repository.MerchantRepository;
//import me.swudam.jangbo.repository.StoreRepository;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//import java.util.Set;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//@Transactional
//@TestPropertySource(locations = "classpath:application.properties")
//class StoreServiceTest {
//
//    @Autowired
//    StoreService storeService;
//
//    @Autowired
//    StoreRepository storeRepository;
//
//    @Autowired
//    MerchantRepository merchantRepository;
//
//    // 테스트용 Merchant 생성 메서드
//    private Merchant createTestMerchant() {
//        Merchant merchant = new Merchant();
//        merchant.setUsername("테스트상점주");
//        merchant.setEmail("testowner@gmail.com");
//        merchant.setPassword("password!");
//        merchant.setPhoneNumber("010-1234-5678");
//        merchant.setBusinessNumber("000-0000-00000");
//        return merchantRepository.save(merchant);
//    }
//
//    // 테스트용 StoreFormDto 생성 메서드
//    private StoreFormDto createStoreFormDto() {
//        StoreFormDto storeFormDto = new StoreFormDto();
//        storeFormDto.setStoreName("테스트 상점");
//        storeFormDto.setStoreAddress("서울시 종로구");
//        storeFormDto.setBusinessHours("10:00-19:00");
//        storeFormDto.setDayOff(List.of(DayOff.FRIDAY, DayOff.MONDAY)); // ALWAYS_OPEN으로 바꾸면 테스트 실패(상점 등록 안 됨)
//        storeFormDto.setStorePhoneNumber("02-9876-5432");
//        storeFormDto.setCategory(Category.야채);
//        return storeFormDto;
//    }
//
//    @Test
//    @DisplayName("상점 저장 테스트 (이미지 미포함)")
//    void saveStoreWithoutImage() throws Exception {
//        Merchant merchant = createTestMerchant();
//        StoreFormDto storeFormDto = createStoreFormDto();
//
//        Long savedStoreId = storeService.saveStore(storeFormDto, merchant, null);
//
//        Store savedStore = storeRepository.findById(savedStoreId).orElse(null);
//
//        assertNotNull(savedStore);
//        assertEquals(storeFormDto.getStoreName(), savedStore.getStoreName());
//        assertEquals(storeFormDto.getStoreAddress(), savedStore.getStoreAddress());
//        assertEquals(storeFormDto.getBusinessHours(), savedStore.getBusinessHours());
//        assertEquals(storeFormDto.getStorePhoneNumber(), savedStore.getStorePhoneNumber());
//        assertEquals(storeFormDto.getCategory(), savedStore.getCategory());
//        assertEquals(2, savedStore.getDayOff().size());
//        assertTrue(savedStore.getDayOff().contains(DayOff.MONDAY));
//        assertTrue(savedStore.getDayOff().contains(DayOff.FRIDAY));
//        assertNull(savedStore.getStoreImg());
//    }
//
//    @Test
//    @DisplayName("상점 저장 테스트 (이미지 포함)")
//    void saveStoreWithImage() throws Exception {
//        Merchant merchant = createTestMerchant();
//        StoreFormDto storeFormDto = createStoreFormDto();
//
//        MockMultipartFile storeImage = new MockMultipartFile(
//                "storeImage",
//                "testImage.jpg",
//                "image/jpeg",
//                "dummy image content".getBytes(StandardCharsets.UTF_8)
//        );
//
//        Long savedStoreId = storeService.saveStore(storeFormDto, merchant, storeImage);
//
//        Store savedStore = storeRepository.findById(savedStoreId).orElse(null);
//
//        assertNotNull(savedStore);
//        assertNotNull(savedStore.getStoreImg());
//        assertNotNull(savedStore.getStoreImgPath());
//        assertTrue(savedStore.getStoreImg().contains("testImage.jpg"));
//    }
//
//    @Test
//    @DisplayName("휴무 요일 제약 조건 테스트 - 연중무휴와 다른 요일 동시 선택 불가")
//    void dayOffConstraintTest() {
//        Merchant merchant = createTestMerchant();
//        StoreFormDto storeFormDto = createStoreFormDto();
//
//        storeFormDto.setDayOff(List.of(DayOff.ALWAYS_OPEN, DayOff.MONDAY));
//
//        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
//            storeService.saveStore(storeFormDto, merchant, null);
//        });
//
//        assertEquals("연중무휴는 다른 요일과 함께 선택할 수 없습니다.", exception.getMessage());
//    }
//}
