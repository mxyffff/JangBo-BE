package me.swudam.jangbo.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import me.swudam.jangbo.entity.Category;
import me.swudam.jangbo.entity.DayOff;
import me.swudam.jangbo.entity.Store;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// [상인] 상점 등록
@Getter
@Setter
public class StoreFormDto {

    @NotBlank(message = "상점명은 필수 입력 값입니다.")
    private String storeName;

    private MultipartFile storeImage;

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
    private Category category;

    // --- Added mapper for response usage ---
    public static StoreFormDto of(Store s) {
        StoreFormDto dto = new StoreFormDto();
        dto.setStoreName(s.getStoreName());
        dto.setStoreAddress(s.getStoreAddress());
        dto.setOpenTime(s.getOpenTime());
        dto.setCloseTime(s.getCloseTime());
        dto.setDayOff(new ArrayList<>(s.getDayOff()));
        dto.setStorePhoneNumber(s.getStorePhoneNumber());
        dto.setCategory(s.getCategory());
        // storeImage is request-only; not set on response
        return dto;
    } // 단일 선택
}