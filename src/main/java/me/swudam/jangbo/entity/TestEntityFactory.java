package me.swudam.jangbo.entity;

/*
 * 테스트용 엔티티 객체를 간편하게 생성하기 위한 팩토리 클래스.
 * 실제 서비스 로직에 영향을 주지 않고, 테스트 환경에서 필요한
 * 더미 데이터를 손쉽게 만들 수 있도록 제공됨.
 *
 * 사용 목적:
 * - 테스트 코드에서 반복적으로 객체를 새로 생성할 때 코드 중복 최소화
 * - 엔티티 생성 시 필요한 필드 초기화 간편화
 * - 빌더 또는 Setter 접근 방식이 다른 엔티티들을 일관되게 생성 가능
 */
public class TestEntityFactory {

    /*
     * 테스트용 Customer 객체 생성
     * Customer 엔티티는 @Builder 사용
     */
    public static Customer createCustomer(String username, String email, String password) {
        return Customer.builder()
                .username(username)
                .email(email)
                .password(password)
                .build();
    }

    /*
     * 테스트용 Merchant 객체 생성
     * Merchant 엔티티는 빌더 없음 → Setter 사용
     */
    public static Merchant createMerchant(String username, String email) {
        Merchant m = new Merchant();
        m.setUsername(username);
        m.setEmail(email);
        m.setPassword("password!");
        return m;
    }

    /*
     * 테스트용 Store 객체 생성
     * Store 엔티티는 빌더 없음 → Setter 사용
     */
    public static Store createStore(String storeName, Merchant merchant) {
        Store s = new Store();
        s.setStoreName(storeName);
        s.setMerchant(merchant);
        return s;
    }

    /*
     * 테스트용 Product 객체 생성
     * Product 엔티티는 @Builder 사용
     */
    public static Product createProduct(String name, Store store, int price, int stock) {
        return Product.builder()
                .name(name)
                .store(store)
                .price(price)
                .stock(stock)
                .build();
    }
}
