package me.swudam.jangbo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MerchantOrderAcceptRequestDto {
    private int prepareTimeMinutes; // 선택한 준비시간 (5, 10, 20, 30)
}