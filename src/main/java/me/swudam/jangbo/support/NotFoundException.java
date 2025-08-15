package me.swudam.jangbo.support;

// 도메인 리소스를 찾지 못했을 때 (조회 실패 시) 사용하는 커스텀 런타임 예외
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
