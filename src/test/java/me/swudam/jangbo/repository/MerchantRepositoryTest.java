package me.swudam.jangbo.repository;

import me.swudam.jangbo.entity.Category;
import me.swudam.jangbo.entity.Merchant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(locations="classpath:application.properties")
class MerchantRepositoryTest {
    @Autowired
    MerchantRepository merchantRepository;

    // 상인-2 - [온보딩] 상인 상점 등록
    @Test
    @DisplayName("회원가입 테스트")
    public void createMerchantTest(){
        // 멤버 객체 세팅
        Merchant merchant = new Merchant();

        merchant.setUsername("김슈니");
        merchant.setEmail("swu@gmail.com");
        merchant.setPassword("1234");
        merchant.setPhoneNumber("010-1234-5678");
        merchant.setStoreName("명이칼국수");
        merchant.setStoreAddress("서울 노원구 동일로180길 53");
        merchant.setBusinessNumber("123-4567-8900");
        merchant.setCategory(Category.건어물);

        // memberRepository의 save 메서드 사용해서 해당 객체 저장
        Merchant savedMerchant = merchantRepository.save(merchant);

        // println() 함수로 객체 출력
        System.out.println(merchant.toString());
    }
}