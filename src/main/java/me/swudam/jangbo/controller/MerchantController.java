package me.swudam.jangbo.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.MerchantFormDto;
import me.swudam.jangbo.entity.Category;
import me.swudam.jangbo.entity.Merchant;
import me.swudam.jangbo.service.MerchantService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.validation.Valid;

// [온보딩] 상인
@RequestMapping("/merchants")
@Controller
@RequiredArgsConstructor
public class MerchantController {
    private final MerchantService merchantService;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    // Get
    @GetMapping(value="/new") // 요청 시 회원가입 폼 페이지로 이동
    public String merchantForm(Model model){
        model.addAttribute("merchantFormDto", new MerchantFormDto());
        model.addAttribute("categories", Category.values()); // enum 배열 전달
        return "merchant/merchantForm";
    }

    // Post
    @PostMapping(value="/new") // submit 버튼 누르면 호출되어 회원 생성
    public String merchantForm(@Valid MerchantFormDto merchantFormDto, BindingResult bindingResult,
                               Model model, HttpSession session){
        // DTO 유효성 검사 실패 시
        if(bindingResult.hasErrors()){
            model.addAttribute("categories", Category.values());
            return "merchant/merchantForm";
        }

        // 비밀번호와 비밀번호 재입력 불일치 체크
        if (!merchantFormDto.getPassword().equals(merchantFormDto.getPasswordConfirm())) {
            model.addAttribute("categories", Category.values());
            bindingResult.rejectValue("passwordConfirm", "error.passwordConfirm", "비밀번호가 일치하지 않습니다.");
            return "merchant/merchantForm";
        }

        try{
            Merchant merchant = Merchant.createMerchant(merchantFormDto, passwordEncoder);
            merchantService.saveMerchant(merchant);

            // 회원가입 성공 시 세션 플래그 설정 - 상점 등록 페이지 접근 허용
            session.setAttribute("justRegisteredMerchant", true);
            // 상점 등록할 때 authenticatino이 없으면 세션에서 이메일을 꺼내 쓰도록
            session.setAttribute("justRegisteredMerchantEmail", merchant.getEmail()); // 이메일도 같이 저장

        } catch(IllegalStateException e){ // 중복 회원 가입 예외 발생 시 예러 메시지를 뷰로 전달
            model.addAttribute("categories", Category.values());
            model.addAttribute("errorMessage", e.getMessage());
            return "merchant/merchantForm";
        }

        System.out.println("[회원가입 성공] justRegisteredMerchant 플래그 세션에 저장: " + session.getAttribute("justRegisteredMerchant"));
        return "redirect:/stores/new"; // 회원가입 성공 시 상점 등록으로 이동
    }

    // 로그인
    // Get
    @GetMapping("/login") // 요청 시 로그인 폼 페이지로 이동
    public String loginMerchant(Model model){
        return "merchant/merchantLoginForm";
    }

    // Get
    @GetMapping("/login/error") // 로그인 실패한 경우
    public String loginMerchantError(Model model){
        model.addAttribute("errorMessage", "이메일 또는 비밀번호를 확인해주세요.");
        return "merchant/merchantLoginForm"; // 실패 시 다시 로그인 창으로 이동
    }
}
