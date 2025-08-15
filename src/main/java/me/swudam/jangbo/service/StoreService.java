package me.swudam.jangbo.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.StoreFormDto;
import me.swudam.jangbo.entity.DayOff;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.entity.Store;
import me.swudam.jangbo.repository.MerchantRepository;
import me.swudam.jangbo.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

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

        // 휴무 요일 규칙 검증
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

    // 전체 상점 조회
    @Transactional(readOnly = true)
    public List<StoreFormDto> getStores(){
        List<Store> stores = storeRepository.findAll();
        List<StoreFormDto> storeFormDtos = new ArrayList<>();
        stores.forEach(s -> storeFormDtos.add(StoreFormDto.of(s)));
        return storeFormDtos;
    }

    // ID로 특정 상점 찾기
    @Transactional(readOnly = true)
    public StoreFormDto getStoreById(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "ID에 해당하는 상점을 찾을 수 없습니다. " + storeId));
        return StoreFormDto.of(store);
    }

    // 상점 수정
    public void updateStore(Long storeId, StoreFormDto storeFormDto,
                            MultipartFile storeImage) throws Exception{
        // 휴무 요일 규칙 검증
        validateDayOff(storeFormDto);

        Optional<Store> optionalStore = storeRepository.findById(storeId);

        if (optionalStore.isPresent()) {
            Store store = optionalStore.get();

            if(storeFormDto.getStoreName() != null){
                store.setStoreName(storeFormDto.getStoreName());
            }
            if(storeFormDto.getStoreAddress() != null){
                store.setStoreAddress(storeFormDto.getStoreAddress());
            }
            if(storeFormDto.getOpenTime() != null){
                store.setOpenTime(storeFormDto.getOpenTime());
            }
            if(storeFormDto.getCloseTime() != null){
                store.setCloseTime(storeFormDto.getCloseTime());
            }
            if(storeFormDto.getDayOff() != null && !storeFormDto.getDayOff().isEmpty()){
                store.setDayOff(new HashSet<>(storeFormDto.getDayOff()));
            }
            if(storeFormDto.getStorePhoneNumber() != null){
                store.setStorePhoneNumber(storeFormDto.getStorePhoneNumber());
            }
            if(storeFormDto.getCategory() != null){
                store.setCategory(storeFormDto.getCategory());
            }

            // 이미지 수정
            if (storeImage != null && !storeImage.isEmpty()) {
                // 이미지 교체 시에는 기존 파일 삭제
                if (store.getStoreImgPath() != null) {
                    File oldFile = new File(store.getStoreImgPath());
                    if (oldFile.exists()) {
                        boolean deleted = oldFile.delete();
                        if (!deleted) {
                            System.err.println("기존 이미지 삭제 실패: " + oldFile.getAbsolutePath());
                        }
                    }
                }
                // 새 파일 저장
                UUID uuid = UUID.randomUUID();
                String fileName = uuid + "-" + storeImage.getOriginalFilename();
                File storeImgFile = new File(uploadPath, fileName);
                storeImage.transferTo(storeImgFile);

                store.setStoreImg(fileName);
                store.setStoreImgPath(uploadPath + "/" + fileName);
            }

            storeRepository.save(store);
        } else {
            throw new HttpClientErrorException(
                    HttpStatus.NOT_FOUND,
                    "ID에 해당하는 상점을 찾을 수 없습니다. " + storeId
            );
        }
    }

    // 상점 삭제
    public void deleteStore(Long storeId){
        Optional<Store> optionalStore = storeRepository.findById(storeId);

        if(optionalStore.isPresent()) {
            storeRepository.delete(optionalStore.get());
        } else{
            throw new HttpClientErrorException(
                    HttpStatus.NOT_FOUND,
                    "ID에 해당하는 상점을 찾을 수 없습니다. " + storeId
            );
        }
    }

}