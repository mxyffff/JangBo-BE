//package me.swudam.jangbo.service;
//
//import me.swudam.jangbo.dto.MerchantFormDto;
//import me.swudam.jangbo.entity.Category;
//import me.swudam.jangbo.entity.Merchant;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//// [온보딩] 상인
//@SpringBootTest
//@Transactional
//@TestPropertySource("classpath:application.properties")
//class MerchantServiceTest {
//    @Autowired
//    MerchantService merchantService;
//
//    @Autowired
//    PasswordEncoder passwordEncoder;
//
//    public Merchant createMerchant(){
//        MerchantFormDto merchantFormDto = new MerchantFormDto();
//
//        merchantFormDto.setUsername("김슈니");
//        merchantFormDto.setEmail("swu@gmail.com");
//        merchantFormDto.setPassword("password!");
//        merchantFormDto.setPhoneNumber("010-1234-5678");
//        merchantFormDto.setBusinessNumber("123-4567-8900");
//
//        Merchant merchant = Merchant.createMerchant(merchantFormDto, passwordEncoder);
//        return merchant;
//    }
//
//    @Test
//    @DisplayName("회원가입 테스트")
//    public void saveMerchant(){
//        Merchant merchant1 = this.createMerchant(); // createMerchant로 회원 객체 생성
//        Merchant merchant2 = merchantService.saveMerchant(merchant1); // DB에 저장
//
//        // assertEquals로 생성한 객체와 저장한 객체의 데이터가 일치하는지 개별 비교
//        assertEquals(merchant1.getUsername(), merchant2.getUsername());
//        assertEquals(merchant1.getEmail(), merchant2.getEmail());
//        assertEquals(merchant1.getPassword(), merchant2.getPassword());
//        assertEquals(merchant1.getPhoneNumber(), merchant2.getPhoneNumber());
//        assertEquals(merchant1.getBusinessNumber(), merchant2.getBusinessNumber());
//    }
//
//    @Test
//    @DisplayName("중복 회원가입 테스트")
//    public void setDuplicateMerchantTest() {
//        Merchant merchant1 = createMerchant();
//        Merchant merchant2 = createMerchant();
//
//        merchantService.saveMerchant(merchant1);
//
//        Throwable e = assertThrows(IllegalStateException.class, () -> {
//            merchantService.saveMerchant(merchant2);
//        });
//
//        assertEquals("이미 가입된 회원입니다.", e.getMessage());
//
//    }
//}