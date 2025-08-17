//package me.swudam.jangbo.repository;
//
//import me.swudam.jangbo.entity.Category;
//import me.swudam.jangbo.entity.DayOff;
//import me.swudam.jangbo.entity.Merchant;
//import me.swudam.jangbo.entity.Store;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Set;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//// 상인 상점등록
//@SpringBootTest
//@Transactional
//@TestPropertySource("classpath:application.properties")
//class StoreRepositoryTest {
//    @Autowired
//    StoreRepository storeRepository;
//
//    @Autowired
//    MerchantRepository merchantRepository;
//
//    @Test
//    @DisplayName("상점 저장 및 조회 테스트")
//    public void saveAndFindStoreTest() {
//        // Merchant 생성 (테스트용)
//        Merchant merchant = new Merchant();
//        merchant.setUsername("상점주인");
//        merchant.setEmail("owner@gmail.com");
//        merchant.setPassword("password!");
//        merchant.setPhoneNumber("010-0000-0000");
//        merchant.setBusinessNumber("000-0000-00000");
//        merchantRepository.save(merchant);
//
//        // Store 생성
//        Store store = new Store();
//        store.setStoreName("맛집");
//        store.setStoreAddress("서울시 강남구");
//        store.setBusinessHours("09:00-18:00");
//        store.setDayOff(Set.of(DayOff.MONDAY, DayOff.WEDNESDAY));
//        store.setStorePhoneNumber("02-1234-5678");
//        store.setCategory(Category.수산);
//        store.setMerchant(merchant);
//
//        Store savedStore = storeRepository.save(store);
//
//        // 조회해서 비교
//        Store foundStore = storeRepository.findById(savedStore.getId()).orElse(null);
//
//        // println() 함수로 객체 출력
//        System.out.println(merchant.toString());
//    }
//}