//package me.swudam.jangbo.repository;
//
//import me.swudam.jangbo.entity.Category;
//import me.swudam.jangbo.entity.Merchant;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.transaction.annotation.Transactional;
//
//// [온보딩] 상인
//@SpringBootTest
//@Transactional
//@TestPropertySource(locations="classpath:application.properties")
//class MerchantRepositoryTest {
//    @Autowired
//    MerchantRepository merchantRepository;
//
//    @Test
//    @DisplayName("회원가입 테스트 － 상점 필드 분리 후")
//    public void createMerchantTest(){
//        // 멤버 객체 세팅
//        Merchant merchant = new Merchant();
//
//        merchant.setUsername("김슈니");
//        merchant.setEmail("swu@gmail.com");
//        merchant.setPassword("password!");
//        merchant.setPhoneNumber("010-1234-5678");
//        merchant.setBusinessNumber("123-4567-8900");
//
//        // memberRepository의 save 메서드 사용해서 해당 객체 저장
//        Merchant savedMerchant = merchantRepository.save(merchant);
//
//        // println() 함수로 객체 출력
//        System.out.println(merchant.toString());
//    }
//}