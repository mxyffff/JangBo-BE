package me.swudam.jangbo.controller;

import lombok.RequiredArgsConstructor;
import me.swudam.jangbo.dto.PickupCounterResponseDto;
import me.swudam.jangbo.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// PUBLIC
// 모든 고객/상인 상관없이 픽업대 현황 보여줌
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/pickup-counters")
public class PickupController {

    private final OrderService orderService;

    // 1. 특정 상점별 픽업대 조회
    // GET - /api/public/pickup-counters/store/{storeId}
    @GetMapping("/store/{storeId}")
    public ResponseEntity<?> getCountersByStore(@PathVariable Long storeId){
        try {
            List<PickupCounterResponseDto> counters = orderService.getCountersByStore(storeId);
            // 성공 시 Map 형태로 반환
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "counters", counters
            ));
        } catch (IllegalArgumentException e) {
            // storeId가 잘못되었거나 조회 불가 시 BadRequest 반환
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 2. 모든 상점 픽업대 조회
    // GET - /api/public/pickup-counters/all
    @GetMapping("/all")
    public ResponseEntity<?> getAllStoresCounters() {
        Map<Long, List<PickupCounterResponseDto>> allCounters = orderService.getAllStoresCounters();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "allCounters", allCounters
        ));
    }
}