package me.swudam.jangbo.service;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.StoreFormDto;
import me.swudam.jangbo.entity.DayOff;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.entity.Store;
import me.swudam.jangbo.repository.MerchantRepository;
import me.swudam.jangbo.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final MerchantRepository merchantRepository;

    @Value("${uploadPath}")
    private String uploadPath;

    // 휴무 요일 제약 조건(연중무휴는 단독 선택만 가능)
    public void validateDayOff(StoreFormDto storeFormDto) {
        if (storeFormDto.getDayOff().contains(DayOff.ALWAYS_OPEN)
                && storeFormDto.getDayOff().size() > 1) {
            throw new IllegalStateException("연중무휴는 다른 요일과 함께 선택할 수 없습니다.");
        }
    }

    // 이미지 포함하여 Store 저장 로직
    public Long saveStore(StoreFormDto storeFormDto, Merchant merchant,
                          MultipartFile storeImage) {
        // 상점 등록 시 Merchant 조회 + 예외
        Merchant managedMerchant = merchantRepository.findById(merchant.getId())
                .orElseThrow(() -> new IllegalStateException("상인을 찾을 수 없습니다."));

        // 규칙 검증
        validateDayOff(storeFormDto);

        // Store 생성
        Store store = Store.createStore(storeFormDto, managedMerchant);

        // 이미지 저장
        if (storeImage != null && !storeImage.isEmpty()) {
            try {
                UUID uuid = UUID.randomUUID();
                String fileName = uuid + "-" + storeImage.getOriginalFilename();
                File storeImgFile = new File(uploadPath, fileName);
                storeImage.transferTo(storeImgFile);

                store.setStoreImg(fileName);
                store.setStoreImgPath(uploadPath + "/" + fileName);
            } catch (Exception e) {
                throw new IllegalStateException("이미지 저장 중 오류가 발생했습니다.");
            }
        }

        storeRepository.save(store);
        return store.getId();
    }

    @Transactional(readOnly = true)
    public Store getStoreById(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("상점을 찾을 수 없습니다."));
    }
}