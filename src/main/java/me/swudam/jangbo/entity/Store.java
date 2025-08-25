package me.swudam.jangbo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.swudam.jangbo.dto.StoreFormDto;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ToString(exclude = {"merchant", "products"})
@Entity
@Table(name = "store",
        // 추가
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_merchant", columnNames = "merchant_id")
        },
        indexes = {
                @Index(name = "idx_store_merchant_id", columnList = "merchant_id"),
                @Index(name = "idx_store_category", columnList = "category"),
                @Index(name = "idx_store_store_name", columnList = "store_name")
        })
@Getter
@Setter
public class Store extends BaseTimeEntity {

    // ※ 한 줄 소개 글 길이 제한(프론트 디자인 고려)
    public static final int MAX_TAGLINE_LENGTH = 80;

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

    private LocalTime openTime; // 오픈 시간
    private LocalTime closeTime; // 마감 시간

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

    // (신규) 한 줄 소개 — 직접 입력/AI 추천 결과가 들어오는 곳
    // - 길이 제한: DB는 80자까지, 서비스/도메인에서 동일하게 제한
    @Column(name = "tagline", length = MAX_TAGLINE_LENGTH)
    private String tagline;

    // 수정
    // Merchant와의 관계 (FK)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    // 추가
    // 상품 영속성
    @OneToMany(mappedBy = "store", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false)
    private List<Product> products = new ArrayList<>();

    public static Store createStore(StoreFormDto storeFormDto, Merchant merchant) {
        Store store = new Store();

        store.setStoreName(storeFormDto.getStoreName());
        store.setStoreImgPath(String.valueOf(storeFormDto.getStoreImgPath())); // 이미지 경로 추가
        store.setStoreAddress(storeFormDto.getStoreAddress());
        store.setOpenTime(storeFormDto.getOpenTime());
        store.setCloseTime(storeFormDto.getCloseTime());
        store.setDayOff(new HashSet<>(storeFormDto.getDayOff()));
        store.setStorePhoneNumber(storeFormDto.getStorePhoneNumber());
        store.setCategory(storeFormDto.getCategory());
        store.setMerchant(merchant);

        // null 들어와도 빈 문자열로 정리해서 NOT NULL 보장
        store.updateTagline(storeFormDto.getTagline());

        // 이미지 저장 로직은 Service에서 처리
        return store;
    }

    /** 한 줄 소개 업데이트(항상 NOT NULL 유지) */
    public void updateTagline(String newTagline) {
        // null -> "" 로 변환, 양끝/연속 공백 정리, 80자 컷
        String t = (newTagline == null) ? "" : newTagline.trim().replaceAll("\\s+", " ");
        if (t.length() > MAX_TAGLINE_LENGTH) t = t.substring(0, MAX_TAGLINE_LENGTH);
        this.tagline = t;
    }
}
