package me.swudam.jangbo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.swudam.jangbo.dto.StoreFormDto;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ToString(exclude = "merchant")
@Entity
@Table(name="store")
@Getter
@Setter
public class Store {
    // PK
    @Id
    @Column(name = "store_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본키 id

    private String storeName; // 상점명

    // 상점 이미지 등록
    private String storeImg;
    private String storeImgPath;

    private String storeAddress; // 도로명 주소
    private String businessHours; // 운영 시간

    @ElementCollection(targetClass = DayOff.class)
    @CollectionTable(
            name = "store_day_off",
            joinColumns = @JoinColumn(name = "store_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "day_off")
    private Set<DayOff> dayOff = new HashSet<>(); // 휴무 요일

    private String storePhoneNumber; // 상점 전화번호

    @Enumerated(EnumType.STRING)
    private Category category; // 카테고리

    // Merchant와의 관계 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    public static Store createStore(StoreFormDto storeFormDto, Merchant merchant){
        Store store = new Store();

        store.setStoreName(storeFormDto.getStoreName());
        store.setStoreAddress(storeFormDto.getStoreAddress());
        store.setBusinessHours(storeFormDto.getBusinessHours());
        store.setDayOff(new HashSet<>(storeFormDto.getDayOff()));
        store.setStorePhoneNumber(storeFormDto.getStorePhoneNumber());
        store.setCategory(storeFormDto.getCategory());
        store.setMerchant(merchant);

        // 이미지 저장 로직은 Service에서 처리
        return store;
    }
}
