package me.swudam.jangbo.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.StoreFormDto;
import me.swudam.jangbo.entity.Category;
import me.swudam.jangbo.entity.DayOff;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.repository.MerchantRepository;
import me.swudam.jangbo.repository.StoreRepository;
import me.swudam.jangbo.service.StoreService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/stores")
@Controller
@RequiredArgsConstructor
public class StoreController {
    private final StoreRepository storeRepository;
    private final StoreService storeService;
    private final MerchantRepository merchantRepository;

    // 상점등록
    // Get
    @GetMapping("/new")
    public String storeForm(Model model, HttpSession session) {
        // 세션 플래그 로직
        // 회원가입 직후 한 번만 접근 가능하고, 새로고침이나 직접 URl 접근은 불가능!
        Boolean justRegistered = (Boolean) session.getAttribute("justRegisteredMerchant");
        if (justRegistered == null || !justRegistered) {
            return "redirect:/";
        }

        model.addAttribute("storeFormDto", new StoreFormDto());
        model.addAttribute("categories", Category.values()); // enum 배열 전달
        model.addAttribute("dayOffOptions", DayOff.values()); // 휴무요일 배열 전달
        return "store/storeForm";
    }

    // Post
    @PostMapping("/new")
    public String storeForm(@Valid StoreFormDto storeFormDto, BindingResult bindingResult,
                            Model model, Authentication authentication,
                            @RequestParam("storeImage") MultipartFile storeImage,
                            HttpSession session) {

        // 테스트용
        System.out.println("===== [상점 등록 시작] =====");
        System.out.println("현재 세션 ID: " + session.getId());
        System.out.println("폼 입력값: " + storeFormDto);
        System.out.println("로그인 Principal: " + authentication);
        System.out.println("이메일: " + (authentication != null ? authentication.getPrincipal() : "null"));
        System.out.println("업로드 파일명: " + (storeImage != null ? storeImage.getOriginalFilename() : "null"));
        System.out.println("파일 비었는지: " + (storeImage != null && storeImage.isEmpty()));

        Boolean justRegistered = (Boolean) session.getAttribute("justRegisteredMerchant");
        String email = null;

        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                email = ((User) principal).getUsername();
            } else if (principal instanceof String) {
                email = (String) principal;
            }
        }

        // 로그인 정보 없으면 세션에서 이메일 가져오기
        if (email == null && justRegistered != null && justRegistered) {
            email = (String) session.getAttribute("justRegisteredMerchantEmail");
            System.out.println("[세션에서 가져온 이메일] " + email);
        }

        if (email == null) {
            System.out.println("이메일 정보가 없어 상점 등록 불가");
            return "redirect:/merchants/login";
        }

        try {
            Merchant merchant = merchantRepository.findByEmail(email); // DB에서 다시 조회
            System.out.println("DB에서 조회한 Merchant: " + merchant);

            if (merchant == null) throw new IllegalStateException("로그인 정보가 올바르지 않습니다.");

            Long storeId = storeService.saveStore(storeFormDto, merchant, storeImage);
            System.out.println("저장된 상점 ID: " + storeId);

            // 상점 등록 성공 시 세션에 저장한 이메일도 제거
            session.removeAttribute("justRegisteredEmail");

        } catch (IllegalStateException e) {
            System.out.println("상태 예외 발생: " + e.getMessage());
            model.addAttribute("categories", Category.values());
            model.addAttribute("dayOffOptions", DayOff.values());
            model.addAttribute("errorMessage", e.getMessage());
            return "store/storeForm";
        } catch (Exception e) {
            System.out.println("기타 예외 발생: " + e.getMessage());
            e.printStackTrace(); // 발생한 예외
            model.addAttribute("errorMessage", "이미지 업로드 중 오류가 발생했습니다.");
            model.addAttribute("categories", Category.values());
            model.addAttribute("dayOffOptions", DayOff.values());
            return "store/storeForm";
        }

        // 상점 등록 성공 후 세션 플래그와 이메일 제거 (1회성)
        if (justRegistered != null && justRegistered) {
            session.removeAttribute("justRegisteredMerchant");
            session.removeAttribute("justRegisteredMerchantEmail");
        }

        System.out.println("===== [상점 등록 완료] =====");
        return "redirect:/"; // 성공 시 메인 페이지로 리다이렉트. 추후 한 줄 소개 글 AI로 이동하도록
    }
}
