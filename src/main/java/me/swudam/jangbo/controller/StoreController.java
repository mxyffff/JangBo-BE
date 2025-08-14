package me.swudam.jangbo.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.StoreFormDto;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.repository.MerchantRepository;
import me.swudam.jangbo.repository.StoreRepository;
import me.swudam.jangbo.service.StoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
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
                                         HttpSession session, Authentication authentication){
        // 1-1. 세션/Authentication 확인 (로그인 또는 1회성 가입)
        String email = getAuthenticatedMerchantEmail(session, authentication);
        if(email == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "created", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        // 1-2. 업로드 파일 타입 검사
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
            // 1-3. 이메일로 Merchant 조회
            Merchant merchant = merchantRepository.findByEmail(email);
            if(merchant == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "created", false,
                        "message", "유효하지 않은 로그인 정보입니다."
                ));
            }

            // 1-4. 상점 저장 로직 호출
            Long storeId = storeService.saveStore(storeFormDto, merchant, storeImage);

            // 1-5. 1회성 가입 세션 제거
            session.removeAttribute("justRegisteredMerchant");
            session.removeAttribute("justRegisteredMerchantEmail");

            // 1-6. 성공 응답
            Map<String, Object> body = new HashMap<>();
            body.put("created", true);
            body.put("storeId", storeId);

            return ResponseEntity.status(HttpStatus.CREATED).body(body);

        } catch (IllegalStateException e){
            // 1-7. 400 Bad Request
            return ResponseEntity.badRequest().body(Map.of(
                    "created", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e){
            // 1-8. 500 Internal Server Error
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
            return ResponseEntity.ok(Map.of(
                    "found", true,
                    "store", storeFormDto
            ));
        } catch (ResponseStatusException e){
            // 2-2. 상점 없으면 404
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "found", false,
                    "message", e.getReason()
            ));
        }
    }

    // 3. 전체 상점 조회 API (R)
    // GET - /api/stores
    @GetMapping
    public ResponseEntity<?> getAllStores() {
        try{
            // 3-1. 전체 상점 조회
            return ResponseEntity.ok(Map.of(
                    "found", true,
                    "stores", storeService.getStores()
            ));
        } catch (Exception e){
            // 3-2. 서버 오류
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
                                         @Valid @ModelAttribute StoreFormDto storeFormDto,
                                         @RequestParam(value="storeImage", required = false) MultipartFile storeImage,
                                         HttpSession session){
        try{
            // 4-1. 인증된 사용자 이메일 가져오기
            String email = getAuthenticatedMerchantEmail(session, null);
            if(email == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "updated", false,
                        "message", "로그인이 필요합니다."
                ));
            }

            // 4-2. 로그인한 상인 조회
            Merchant merchant = merchantRepository.findByEmail(email);
            if(merchant == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "updated", false,
                        "message", "유효하지 않은 로그인 정보입니다."
                ));
            }

            // 4-3. 상점 수정 호출
            storeService.updateStore(storeId, storeFormDto, storeImage, merchant);

            return ResponseEntity.ok(Map.of(
                    "updated", true,
                    "storeId", storeId
            ));

        } catch (ResponseStatusException e){
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "updated", false,
                    "message", e.getReason()
            ));
        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "updated", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e){
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
    public ResponseEntity<?> deleteStore(@PathVariable Long storeId, HttpSession session) {
        try{
            // 5-1. 인증된 사용자 이메일 가져오기
            String email = getAuthenticatedMerchantEmail(session, null);
            if(email == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "deleted", false,
                        "message", "로그인이 필요합니다."
                ));
            }

            // 5-2. 로그인한 상인 조회
            Merchant merchant = merchantRepository.findByEmail(email);
            if(merchant == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "deleted", false,
                        "message", "유효하지 않은 로그인 정보입니다."
                ));
            }

            // 5-3. 상점 삭제 호출
            storeService.deleteStore(storeId, merchant);

            return ResponseEntity.ok(Map.of(
                    "deleted", true,
                    "storeId", storeId
            ));

        } catch (ResponseStatusException e){
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "deleted", false,
                    "message", e.getReason()
            ));
        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "deleted", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "deleted", false,
                    "message", "상점 삭제 중 오류가 발생했습니다"
            ));
        }
    }

    // Helper: 현재 로그인한 상인 이메일 가져오기
    private String getAuthenticatedMerchantEmail(HttpSession session, Authentication authentication) {
        String email = null;

        // SecurityContext 확인
        if(authentication == null){
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        if(authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())){
            Object principal = authentication.getPrincipal();
            email = (principal instanceof UserDetails userDetails) ? userDetails.getUsername() : principal.toString();
        }

        // 세션 확인
        if(email == null){
            email = (String) session.getAttribute("merchantEmail");
        }

        // 1회성 가입 세션 확인
        if(email == null && Boolean.TRUE.equals(session.getAttribute("justRegisteredMerchant"))){
            email = (String) session.getAttribute("justRegisteredMerchantEmail");
        }

        return email;
    }
}