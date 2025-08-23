package me.swudam.jangbo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.ai.AiIngredientRequestDto;
import me.swudam.jangbo.dto.ai.AiRecommendationRequestDto;
import me.swudam.jangbo.dto.ai.AiRecommendationResponseDto;
import me.swudam.jangbo.service.AiRecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 2차 처리 컨트롤러
@RestController
@RequestMapping("/api/ai/recommendations")
@RequiredArgsConstructor
public class AiRecommendationController {

    private final AiRecommendationService recommendationService;

    @PostMapping
    public ResponseEntity<AiRecommendationResponseDto> recommend(
            @RequestBody @Valid AiRecommendationRequestDto requestDto
    ) {
        return ResponseEntity.ok(recommendationService.recommend(requestDto));
    }
}
