package me.swudam.jangbo.service;

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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    @Value("${uploadPath}")
    private String uploadPath;

    // 1. 상점 저장
    public Long saveStore(StoreFormDto storeFormDto, Merchant merchant, MultipartFile storeImage) {
        // 1-1. 휴무 요일 제약
        if(storeFormDto.getDayOff().contains(DayOff.ALWAYS_OPEN) && storeFormDto.getDayOff().size() > 1){
            throw new IllegalStateException("연중무휴는 다른 요일과 함께 선택할 수 없습니다.");
        }

        // 1-2. Store 생성 DTO → Entity 변환
        Store store = Store.createStore(storeFormDto, merchant);

        // 1-3. 이미지 저장
        if(storeImage != null && !storeImage.isEmpty()){
            try{
                UUID uuid = UUID.randomUUID();
                String fileName = uuid + "-" + storeImage.getOriginalFilename();
                File storeImgFile = new File(uploadPath, fileName);
                storeImage.transferTo(storeImgFile);
                store.setStoreImg(fileName);
                store.setStoreImgPath(uploadPath + "/" + fileName);
            } catch(Exception e){
                throw new IllegalStateException("이미지 저장 중 오류가 발생했습니다.");
            }
        }

        // 1-4. DB 저장
        storeRepository.save(store);
        return store.getId();
    }

    // 2. 단일 상점 조회
    @Transactional(readOnly = true)
    public StoreFormDto getStoreById(Long storeId){
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "ID에 해당하는 상점을 찾을 수 없습니다. " + storeId
                ));
        return StoreFormDto.of(store);
    }

    // 3. 전체 상점 조회
    @Transactional(readOnly = true)
    public List<StoreFormDto> getStores(){
        List<Store> stores = storeRepository.findAll();
        List<StoreFormDto> dtos = new ArrayList<>();
        stores.forEach(s -> dtos.add(StoreFormDto.of(s)));
        return dtos;
    }

    // 4. 상점 수정
    public void updateStore(Long storeId, StoreFormDto storeFormDto, MultipartFile storeImage, Merchant merchant) throws Exception {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "ID에 해당하는 상점을 찾을 수 없습니다. " + storeId
                ));

        // 4-1. 소유권 확인
        if(!store.getMerchant().getId().equals(merchant.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "상점 소유자가 아니므로 수정할 수 없습니다.");
        }

        // 4-2. 상점 데이터 수정
        if(storeFormDto.getStoreName() != null) store.setStoreName(storeFormDto.getStoreName());
        if(storeFormDto.getStoreAddress() != null) store.setStoreAddress(storeFormDto.getStoreAddress());
        if(storeFormDto.getOpenTime() != null) store.setOpenTime(storeFormDto.getOpenTime());
        if(storeFormDto.getCloseTime() != null) store.setCloseTime(storeFormDto.getCloseTime());
        if(storeFormDto.getDayOff() != null && !storeFormDto.getDayOff().isEmpty())
            store.setDayOff(new HashSet<>(storeFormDto.getDayOff()));
        if(storeFormDto.getStorePhoneNumber() != null) store.setStorePhoneNumber(storeFormDto.getStorePhoneNumber());
        if(storeFormDto.getCategory() != null) store.setCategory(storeFormDto.getCategory());

        // 4-3. 이미지 수정
        if(storeImage != null && !storeImage.isEmpty()){
            if(store.getStoreImgPath() != null){
                File oldFile = new File(store.getStoreImgPath());
                if(oldFile.exists()) oldFile.delete();
            }
            UUID uuid = UUID.randomUUID();
            String fileName = uuid + "-" + storeImage.getOriginalFilename();
            File storeImgFile = new File(uploadPath, fileName);
            storeImage.transferTo(storeImgFile);
            store.setStoreImg(fileName);
            store.setStoreImgPath(uploadPath + "/" + fileName);
        }

        storeRepository.save(store);
    }

    // 5. 상점 삭제
    public void deleteStore(Long storeId, Merchant merchant){
        // 상점 조회
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "ID에 해당하는 상점을 찾을 수 없습니다. " + storeId
                ));

        // 5-1. 소유권 확인
        if(!store.getMerchant().getId().equals(merchant.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "상점 소유자가 아니므로 삭제할 수 없습니다.");
        }
        // 5-2. 이미지 삭제
        if(store.getStoreImgPath() != null){
            File oldFile = new File(store.getStoreImgPath());
            if(oldFile.exists()) oldFile.delete();
        }

        // 5-3. 상점 삭제
        storeRepository.delete(store);
    }
}