package me.swudam.jangbo.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.StoreFormDto;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.entity.Store;
import me.swudam.jangbo.repository.MerchantRepository;
import me.swudam.jangbo.repository.StoreRepository;
import me.swudam.jangbo.service.StoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("api/stores")
@RestController
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService; // 상인 로직
    private final MerchantRepository merchantRepository; // 상인 조회용
    private final StoreRepository storeRepository;

    // 1. 상점 등록 API (C)
    // POST - /api/stores
    @PostMapping
    public ResponseEntity<?> createStore(@Valid @ModelAttribute StoreFormDto storeFormDto,
                                         @RequestParam(value="storeImage", required = false) MultipartFile storeImage,
                                         HttpSession httpSession, Authentication authentication){
        // 1-1. 세션에 저장된 '가입 직후' 여부 확인 (justRegisteredMerchant)
        Boolean justRegistered = (Boolean) httpSession.getAttribute("justRegisteredMerchant");
        String email = null;

        // 1-2. 로그인 정보에서 이메일 추출 (로그인 상태인 경우)
        if(authentication != null){
            Object principal = authentication.getPrincipal();
            if(principal instanceof User user){
                email = user.getUsername(); // UserDetails 타입에서 username(=email) 획득
            } else if (principal instanceof String s){
                email = s; // 간혹 String 타입인 경우를 대비하여
            }
        }

        // 1-3. 인증 정보 없으면,
        // 세션에 가입 직후 이메일이 있으면 그것으로 대체
        if (email == null && Boolean.TRUE.equals(justRegistered)){
            email = (String) httpSession.getAttribute("justRegisteredMerchantEmail");
        }
        // 1-4. 이메일 없으면 인증 실패 401 반환
        if (email == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "created", false,
                            "message", "로그인이 필요합니다."
                    ));
        }
        // 1-5. 이미지가 아닌 파일인지
        if (storeImage != null && !storeImage.isEmpty()) {
            String contentType = storeImage.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "created", false,
                        "message", "이미지 파일만 업로드 가능합니다."
                ));
            }
        }
        try{
            // 1-6. 이메일로 상인 조회 (유효한 로그인 정보인지 확인)
            Merchant merchant = merchantRepository.findByEmail(email);
            if(merchant == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "created", false,
                                "message", "유효하지 않은 로그인 정보입니다."
                        ));
            }

            // 1-7. 상점 저장 로직
            Long storeId = storeService.saveStore(storeFormDto, merchant, storeImage);

            // 1-8. 가입 직후 세션 값 제거 (1회성)
            httpSession.removeAttribute("justRegisteredMerchant");
            httpSession.removeAttribute("justRegisteredMerchantEmail");

            // 1-9. 성공 응답 데이터
            Map<String, Object> body = new HashMap<>();
            body.put("created", true);
            body.put("storeId", storeId);

            // 1-10. 201 Created
            return ResponseEntity.status(HttpStatus.CREATED).body(body);

        } catch (IllegalStateException e){ // 1-11. 400 Bad Request
            return ResponseEntity.badRequest().body(Map.of(
                    "created", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e){ // 1-12. 500 서버 에러
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "created", false,
                    "message", "이미지 업로드 또는 저장 중 오류가 발생했습니다.",
                    "error", e.getMessage()
            ));
        }
    }

    // 2. 특정 상점 조회 API (R)
    // GET - /api/stores{id} (단건 조회)
    @GetMapping("/{storeId}")
    public ResponseEntity<?> getStore(@PathVariable Long storeId) {
        try{
            // 2-1. 상점 조회
            StoreFormDto storeFormDto = storeService.getStoreById(storeId);
            // 2-2. 성공 응답
            return ResponseEntity.ok(Map.of(
                    "found", true,
                    "store", storeFormDto
            ));
        } catch (EntityNotFoundException e){
            // 2-3. 상점 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "found", false,
                    "message", e.getMessage())
            );
        }
    }

    // 2. 전체 상점 조회 API (R)
    // GET - /api/stores (목록 조회)
    @GetMapping
    public ResponseEntity<?> getAllStores() {
        try{
            List<StoreFormDto> stores = storeRepository.findAll().stream()
                    .map(StoreFormDto::of)
                    .toList();
            return ResponseEntity.ok(Map.of(
                    "found", true,
                    "stores", stores
            ));
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "found", false,
                    "message", "상점 목록 조회 중 오류가 발생했습니다.",
                    "error", e.getMessage()
            ));
        }
    }

    // 3. 상점 수정 API (U)
    // PATCH - /api/stores{id}
    @PatchMapping("/{storeId}")
    public ResponseEntity<?> updateStore(@PathVariable Long storeId,
                                         @Valid @ModelAttribute StoreFormDto storeFormdto,
                                         @RequestParam(value="storeImage", required = false) MultipartFile storeImage,
                                         HttpSession session){
        try{
            // 3-1. 인증된 사용자 이메일 가져오기
            String email = getAuthenticatedMerchantEmail(session);

            // 3-2. 이메일 없으면 인증 실패 401
            if (email == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "updated", false,
                        "message", "로그인이 필요합니다."
                ));
            }

            // 3-3. 로그인한 상인 확인 401
            Merchant merchant = merchantRepository.findByEmail(email);
            if(merchant == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "updated", false,
                        "message", "유효하지 않은 로그인 정보입니다."
                ));
            }

            // 3-4. 상점 조회 404
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "ID에 해당하는 상점을 찾을 수 없습니다. " + storeId
                    ));

            // 3-5. 소유권 확인 403
            if (!store.getMerchant().getId().equals(merchant.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "updated", false,
                        "message", "상점 소유자가 아니므로 수정할 수 없습니다."
                ));
            }

            // 3-6. 상점 수정
            storeService.updateStore(storeId, storeFormdto, storeImage);

            // 3-7. 성공 응답 200
            return ResponseEntity.ok(Map.of(
                    "updated", true,
                    "storeId", storeId
            ));
        } catch (ResponseStatusException e){
            // 3-8. 404 및 기타 상태 코드
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "updated", false,
                    "message", e.getReason() != null ? e.getReason() : "이유 없음"
            ));
        } catch (HttpClientErrorException e){
            // 3-9. 잘못된 요청 400
            return ResponseEntity.badRequest().body(Map.of(
                    "updated", false,
                    "storeId", storeId
            ));
        } catch (Exception e){
            // 3-10. 서버 오류 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "updated", false,
                    "message", "상점 수정 중 오류가 발생했습니다",
                    "error", e.getMessage()
            ));
        }
    }

    // 4. 상점 삭제 API (D)
    // DELETE - /api/stores/{storeId}
    @DeleteMapping("/{storeId}")
    public ResponseEntity<?> deleteStore(@PathVariable Long storeId,
                                         HttpSession session) {
        try{
            // 4-1. 인증된 사용자 이메일 가져오기
            String email = getAuthenticatedMerchantEmail(session);

            // 4-2. 이메일 없으면 인증 실패
            if (email == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "deleted", false,
                        "message", "로그인이 필요합니다."
                ));
            }

            // 4-3. 로그인한 상인 확인
            Merchant merchant = merchantRepository.findByEmail(email);
            if(merchant == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "deleted", false,
                        "message", "유효하지 않은 로그인 정보입니다."
                ));
            }

            // 4-4. 상점 조회
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "ID에 해당하는 상점을 찾을 수 없습니다. " + storeId
                    ));

            // 4-5. 소유권 확인
            if (!store.getMerchant().getId().equals(merchant.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "deleted", false,
                        "message", "상점 소유자가 아니므로 삭제할 수 없습니다."
                ));
            }

            // 4-6. 상점 삭제
            storeService.deleteStore(storeId);

            // 4-7. 성공 응답
            return ResponseEntity.ok(Map.of(
                    "deleted", true,
                    "storeId", storeId
            ));
        } catch (ResponseStatusException e){
            // 4-8. 404 및 기타 상태 코드
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "deleted", false,
                    "message", e.getReason() != null ? e.getReason() : "이유 없음"
            ));
        } catch (HttpClientErrorException e){
            // 4-9. 잘못된 요청
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "deleted", false,
                    "message", e.getStatusText()
            ));
        } catch (Exception e){
            // 4-10. 서버 오류
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "deleted", false,
                    "message", "상점 삭제 중 오류가 발생했습니다."
            ));
        }
    }

    // Helper: 현재 로그인한 상인 이메일 가져오기
    private String getAuthenticatedMerchantEmail(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = null;

        // 1. SecurityContext에서 인증 정보 가져오기
        if (auth != null && auth.isAuthenticated() && !isAnonymous(auth)) {
            Object principal = auth.getPrincipal();
            email = (principal instanceof UserDetails userDetails)
                    ? userDetails.getUsername()
                    : principal.toString();
        }

        // 2. SecurityContext가 비어있으면 일반 로그인 세션 확인
        if (email == null) {
            email = (String) session.getAttribute("merchantEmail");
        }

        // 3. 그래도 없으면 1회성 가입 직후 세션 확인
        if (email == null) {
            Boolean justRegistered = (Boolean) session.getAttribute("justRegisteredMerchant");
            if (Boolean.TRUE.equals(justRegistered)) {
                email = (String) session.getAttribute("justRegisteredMerchantEmail");
            }
        }

        return email; // null이면 인증 실패
    }

    private boolean isAnonymous(Authentication auth) {
        String name = auth.getName();
        return name == null || "anonymousUser".equals(name);
    }
}