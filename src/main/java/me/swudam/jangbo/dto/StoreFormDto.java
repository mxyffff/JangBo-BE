package me.swudam.jangbo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import me.swudam.jangbo.entity.Category;
import me.swudam.jangbo.entity.DayOff;
import me.swudam.jangbo.entity.Store;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// [상인] 상점 등록
@Getter
@Setter
public class StoreFormDto {

    /* 한 줄 소개 최대 길이(엔티티와 맞춤) */
    public static final int TAGLINE_MAX = 80;

    @NotBlank(message = "상점명은 필수 입력 값입니다.")
    private String storeName;

    // 요청시 업로드된 파일 받기용
    private MultipartFile storeImgFile;

    // 응답시 클라이언트에 전달할 저장된 이미지 경로 (String)
    private String storeImgPath;

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

    // 추가: 한 줄 소개
    @NotNull(message = "한 줄 소개는 null 일 수 없습니다.")
    @Size(max = TAGLINE_MAX, message = "한 줄 소개는 최대 " + TAGLINE_MAX + "자까지 입력할 수 있습니다.")
    private String tagline = "";  // null 금지. 빈 문자열 기본값.

    // ---- 응답용 식별자 필드 (요청시에는 무시) ----
    private Long storeId;     // 선택: 상점 아이디도 같이 쓰고 싶으면
    private Long merchantId;  // ★ 프론트 요청 -> 상인 id도 함께 반환

    // --- Added mapper for response usage ---
    public static StoreFormDto of(Store s) {
        StoreFormDto dto = new StoreFormDto();

        dto.setStoreId(s.getId()); // 상점 id 추가 (선택)
        dto.setMerchantId(s.getMerchant() != null ? s.getMerchant().getId() : null); // 상인 id 추가
        dto.setStoreName(s.getStoreName());
        dto.setStoreImgPath(s.getStoreImgPath()); // 이미지 경로 추가
        dto.setStoreAddress(s.getStoreAddress());
        dto.setOpenTime(s.getOpenTime());
        dto.setCloseTime(s.getCloseTime());
        dto.setDayOff(new ArrayList<>(s.getDayOff()));
        dto.setStorePhoneNumber(s.getStorePhoneNumber());
        dto.setCategory(s.getCategory());
        // storeImage is request-only; not set on response
        // 엔티티에서 null 방어
        dto.setTagline(Objects.requireNonNullElse(s.getTagline(), ""));

        return dto;
    } // 단일 선택

    /* 저장/수정 전에 한 번 호출해서 문자열을 안전하게 정리 */
    public void normalize() {
        this.storeName        = trimOrNull(this.storeName);
        this.storeAddress     = trimOrNull(this.storeAddress);
        this.storePhoneNumber = trimOrNull(this.storePhoneNumber);
        this.tagline          = normalizeTagline(this.tagline);
    }

    /* 내부 유틸 */
    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String normalizeTagline(String s) {
        if (s == null) return "";
        String t = s.trim().replaceAll("\\s+", " ");
        if (t.length() > TAGLINE_MAX) t = t.substring(0, TAGLINE_MAX);
        return t;
    }
}