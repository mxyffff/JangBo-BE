package me.swudam.jangbo.dto;

import lombok.Getter;
import lombok.Setter;

// 상인 주문 수락 요청 DTO
@Getter
@Setter
public class MerchantOrderAcceptRequestDto {
    private int prepareTimeMinutes; // 선택한 준비시간 (5, 10, 20, 30)
}