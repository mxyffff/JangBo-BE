package me.swudam.jangbo.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.StoreFormDto;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.repository.MerchantRepository;
import me.swudam.jangbo.service.StoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("api/stores")
@RestController
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService; // 상인 로직
    private final MerchantRepository merchantRepository; // 상인 조회용

    // 1. 상점 등록 API
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
