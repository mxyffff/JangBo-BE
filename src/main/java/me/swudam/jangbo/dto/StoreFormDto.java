package me.swudam.jangbo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import me.swudam.jangbo.entity.Category;
import me.swudam.jangbo.entity.DayOff;
import me.swudam.jangbo.entity.Store;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// [상인] 상점 등록
@Getter
@Setter
public class StoreFormDto {

    @NotBlank(message = "상점명은 필수 입력 값입니다.")
    private String storeName;

    @NotBlank(message = "도로명주소는 필수 입력 값입니다.")
    private String storeAddress;

    @NotNull(message = "오픈 시간은 필수 입력 값입니다.")
    private LocalTime openTime;
    @NotNull(message = "마감 시간은 필수 입력 값입니다.")
    private LocalTime closeTime;

    @NotEmpty(message = "휴무 요일은 필수 입력 값입니다.")
    private List<DayOff> dayOff;

    @NotBlank(message = "전화번호는 필수 입력 값입니다.")
    private String storePhoneNumber;

    @NotNull(message = "카테고리는 한 가지를 필수 선택해야 합니다.")
    private Category category; // 단일 선택

    // 조회용 이미지 URL
    private String storeImgPath;

    // 정적 팩토리
    public static StoreFormDto of(Store store) {
        StoreFormDto dto = new StoreFormDto();
        dto.setStoreName(store.getStoreName().trim()); // 상점 이름 등록 시 줄바꿈 trim으로 제거
        dto.setStoreAddress(store.getStoreAddress());
        dto.setOpenTime(store.getOpenTime());
        dto.setCloseTime(store.getCloseTime());
        dto.setDayOff(new ArrayList<>(store.getDayOff())); // Set → List 변환
        dto.setStorePhoneNumber(store.getStorePhoneNumber());
        dto.setCategory(store.getCategory());
        dto.setStoreImgPath(store.getStoreImgPath());
        return dto;
    }
}