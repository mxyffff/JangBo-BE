package me.swudam.jangbo.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.StoreFormDto;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.entity.Store;
import me.swudam.jangbo.repository.MerchantRepository;
import me.swudam.jangbo.repository.StoreRepository;
import me.swudam.jangbo.service.StoreService;
import me.swudam.jangbo.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/api/stores")
@RestController
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService; // 상인 로직
    private final MerchantRepository merchantRepository; // 상인 조회용
    private final StoreRepository storeRepository;

    // 1. 상점 등록 (C)
    @PostMapping
    public ResponseEntity<?> createStore(@Valid @ModelAttribute StoreFormDto storeFormDto,
                                         @RequestParam(value = "storeImage", required = false) MultipartFile storeImage,
                                         HttpSession session, Authentication authentication) {
        String email = extractEmail(authentication, session);
        // 이메일 없으면 인증 실패
        if (email == null) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }
        // 1-5. 이미지가 아닌 파일인지
        if (storeImage != null && !storeImage.isEmpty()) {
            String contentType = storeImage.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
            }
        }

        // 1-6. 이메일로 상인 조회 (유효한 로그인 정보인지 확인)
        Merchant merchant = merchantRepository.findByEmail(email);
        if (merchant == null) {
            throw new AuthenticationCredentialsNotFoundException("유효하지 않은 로그인 정보입니다.");
        }

        // 1-7. 상점 저장 로직
        Long storeId = storeService.saveStore(storeFormDto, merchant, storeImage);
        // 1-8. 가입 직후 세션 값 제거 (1회성)
        session.removeAttribute("justRegisteredMerchant");
        session.removeAttribute("justRegisteredMerchantEmail");

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "created", true,
                "storeId", storeId
        ));
    }

    // 2. 특정 상점 조회 (R)
    // GET - /api/stores{id} (단건 조회)
    @GetMapping("/{storeId}")
    public ResponseEntity<?> getStore(@PathVariable Long storeId) {
        // 2-1. 상점 조회
        // 서비스가 NotFoundException 던짐 → 전역 핸들러가 404로 응답
        StoreFormDto store = storeService.getStoreById(storeId);
        // 2-2. 성공 응답
        return ResponseEntity.ok(Map.of("found", true, "store", store));
    }

    // 3. 전체 상점 조회 (R)
    // GET - /api/stores (목록 조회)
    // 목록 조회용 API
    // sort 파라미터(optional): 정렬 기준
    // recent → 최신순 (기본값)
    @GetMapping
    public ResponseEntity<?> getAllStores(@RequestParam(required = false, defaultValue = "recent") String sort) {
        List<StoreFormDto> stores;

        if ("recent".equals(sort)) {
            stores = storeService.getStoreSortedByRecent(); // 최신순
        } else if ("popular".equals(sort)) {
            // 나중에 인기순 구현
            stores = storeService.getStoreSortedByRecent(); // 임시: 최신순
        } else {
            // 잘못된 sort 값 → 기본 최신순
            stores = storeService.getStoreSortedByRecent();
        }
        return ResponseEntity.ok(Map.of("found", true, "stores", stores));
    }

    // 4. 상점 수정 (U)
    // PATCH - /api/stores{id}
    @PatchMapping("/{storeId}")
    public ResponseEntity<?> updateStore(@PathVariable Long storeId,
                                         @Valid @ModelAttribute StoreFormDto storeFormDto,
                                         @RequestParam(value = "storeImage", required = false) MultipartFile storeImage,
                                         HttpSession session) {
        // 3-1. 인증된 사용자 이메일 가져오기
        String email = extractEmail(SecurityContextHolder.getContext().getAuthentication(), session);
        // 3-2. 이메일 없으면 인증 실패
        if (email == null) throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");

        // 3-3. 로그인한 상인 확인
        Merchant merchant = merchantRepository.findByEmail(email);
        if (merchant == null) throw new AuthenticationCredentialsNotFoundException("유효하지 않은 로그인 정보입니다.");

        // 3-4. 상점 조회 404
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("상점을 찾을 수 없거나 접근 권한이 없습니다."));

        // 3-5. 소유권 확인
        if (!store.getMerchant().getId().equals(merchant.getId())) {
            // 존재/권한을 감추기 위해 404 반환 정책
            throw new NotFoundException("상점을 찾을 수 없거나 접근 권한이 없습니다.");
        }

        // 3-6. 상점 수정
        storeService.updateStore(storeId, storeFormDto, storeImage);
        // 3-7. 성공 응답 200
        return ResponseEntity.ok(Map.of("updated", true, "storeId", storeId));
    }

    // 5. 상점 삭제 (D)
    // DELETE - /api/stores/{storeId}
    @DeleteMapping("/{storeId}")
    public ResponseEntity<?> deleteStore(@PathVariable Long storeId, HttpSession session) {
        // 4-1. 인증된 사용자 이메일 가져오기
        String email = extractEmail(SecurityContextHolder.getContext().getAuthentication(), session);
        // 4-2. 이메일 없으면 인증 실패
        if (email == null) throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");

        // 4-3. 로그인한 상인 확인
        Merchant merchant = merchantRepository.findByEmail(email);
        if (merchant == null) throw new AuthenticationCredentialsNotFoundException("유효하지 않은 로그인 정보입니다.");

        // 4-4. 상점 조회
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("상점을 찾을 수 없거나 접근 권한이 없습니다."));

        // 4-5. 소유권 확인
        if (!store.getMerchant().getId().equals(merchant.getId())) {
            throw new NotFoundException("상점을 찾을 수 없거나 접근 권한이 없습니다.");
        }

        // 4-6. 상점 삭제
        storeService.deleteStore(storeId);
        // 4-7. 성공 응답
        return ResponseEntity.ok(Map.of("deleted", true, "storeId", storeId));
    }

    /* 6. [AI] 한 줄 소개 추천
        요청 JSON body:
        {
            "storeName": "태릉정육",
             "category": "정육/계란",     // 옵션
             "keywords": ["신선", "국내산"] // 옵션 (최대 8개 권장)
        }
        응답 JSON:
        {
            "success": true,
             "candidates": ["문장1","문장2","문장3"]
        }
    */
    @PostMapping("/tagline/suggest")
    public ResponseEntity<?> suggestTagline(@RequestBody @Valid TaglineSuggestRequest req) {
        var candidates = storeService.suggestTaglines(
                req.storeName(),
                req.category(),    // null 가능: 서비스에서 정리
                req.keywords()     // null 가능: 서비스에서 정리
        );
        return ResponseEntity.ok(Map.of(
                "success", true,
                "candidates", candidates
        ));
    }

    // 내부 유틸: 이메일 추출 메서드
    private String extractEmail(Authentication authentication, HttpSession session) {
        String email = null;
        // 1-2. 로그인 정보에서 이메일 추출 (로그인 상태인 경우)
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user) {
                email = user.getUsername();
            } else if (principal instanceof String s) {
                email = s;
            } else if (principal instanceof UserDetails ud) {
                email = ud.getUsername();
            }
        }
        // 1-3. 인증 정보 없으면,
        // 세션에 가입 직후 이메일이 있으면 그것으로 대체
        if (email == null) email = (String) session.getAttribute("merchantEmail");
        if (email == null) {
            // 1-1. 세션에 저장된 '가입 직후' 여부 확인 (justRegisteredMerchant)
            Boolean justRegistered = (Boolean) session.getAttribute("justRegisteredMerchant");
            if (Boolean.TRUE.equals(justRegistered)) {
                email = (String) session.getAttribute("justRegisteredMerchantEmail");
            }
        }
        return email;
    }

    /* 요청/응답 DTO */
    // AI 한 줄 소개 추천 요청
    public record TaglineSuggestRequest(
            @NotBlank(message = "상점명은 필수입니다.")
            String storeName,

            // 선택값: 없으면 모델이 상점명만 보고 생성
            @Size(max = 30, message = "카테고리는 30자를 넘을 수 없습니다.")
            String category,

            // 선택값: 공백/중복은 서비스에서 정리, 과도한 입력 방지를 위해 개수 제한
            @Size(max = 8, message = "키워드는 최대 8개까지 입력할 수 있습니다.")
            List<@Size(max = 12, message = "키워드는 12자 이내여야 합니다.") String> keywords
    ) {
    }

}

/*
전체 흐름 요약
상점 등록
   - 세션에서 '가입 직후' 상태 확인
   - 로그인 상태라면 Authentication에서 이메일 획득
   - 인증 정보 없으면 세션 '가입 직후' 이메일 사용
   - 이메일 없으면 401 반환
   - 이메일로 상인 조회
   - StoreService로 상점 저장
   - 세션 '가입 직후' 정보 제거 (1회성)
   - 성공/실패 응답 반환
*/
