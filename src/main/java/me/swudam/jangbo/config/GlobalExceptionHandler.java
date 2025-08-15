package me.swudam.jangbo.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.swudam.jangbo.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// 전역 예외 처리기
// 목표: 모든 오류 응답의 JSON 포맷을 일관되게 유지
// 성공 응답은 각 API에서 자유롭게 정의하되, 오류는 여기서 단일 포맷으로 응답
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* [유효성 검증 실패] @Valid 바인딩 단계에서 발생 (DTO 필드 에러) */
    // 예: @NotBlank, @Email 위반 등
    // => 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return build(
                request, HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "입력값을 확인해주세요.",
                Map.of("errors", errors)
        );
    }

    /* [리소스 없음] 도메인 조회 실패 시 사용하는 커스텀 예외 */
    // => 404 Not Found
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request
    ) {
        return build(request, HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null);
    }

    /* [잘못된 요청] 비즈니스 규칙 위반 및 단순 파라미터 오류 */
    // 예: 이메일 미인증, 재고/가격 제약 위반 등
    // => 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return build(request, HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), null);
    }

    /* [그 외 예기치 못한 오류] */
    // => 500 Internal Server Error
    // 운영 시 메시지는 일반화하고, 상세 내용은 로그로 남기는 것이 안전
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception", ex);
        return build(request, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", null);
    }

    // @ModelAttribute 바인딩/유효성 오류 → 400 (예: StoreFormDto)
    @ExceptionHandler(org.springframework.validation.BindException.class)
    public ResponseEntity<Map<String,Object>> handleBind(
            org.springframework.validation.BindException ex, HttpServletRequest request) {
        Map<String, Object> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return build(request, HttpStatus.BAD_REQUEST, "BINDING_ERROR",
                "입력값을 확인해주세요.", Map.of("errors", errors));
    }
    // JSON 파싱 실패/날짜 포맷 오류 → 400
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String,Object>> handleNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST, "MESSAGE_NOT_READABLE",
                "요청 본문을 읽을 수 없습니다. 형식을 확인해주세요.", null);
    }
    // 파라미터 타입 불일치 → 400
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String,Object>> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST, "TYPE_MISMATCH",
                "요청 파라미터 타입이 올바르지 않습니다.", Map.of("param", ex.getName()));
    }
    // 파라미터 제약 위반 → 400
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String,Object>> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION",
                "요청 파라미터를 확인해주세요.", Map.of("errors", ex.getConstraintViolations()));
    }
    // 필수 파라미터 누락 → 400
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String,Object>> handleMissingParam(
            org.springframework.web.bind.MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "필수 파라미터가 누락되었습니다.", Map.of("param", ex.getParameterName()));
    }

    // 업로드 용량 초과 → 413
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String,Object>> handleMaxUpload(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return build(request, HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE",
                "업로드 가능한 파일 크기를 초과했습니다.", null);
    }

    // 메서드 미지원 → 405
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String,Object>> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(request, HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "지원하지 않는 요청 메서드입니다.", null);
    }

    // DB 제약 위반(유니크 등) → 409
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String,Object>> handleDataIntegrity(
            org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        return build(request, HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "데이터 무결성 제약을 위반했습니다.", null);
    }

    // 인증 실패(로그인) → 401
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<Map<String,Object>> handleAuth(
            org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {
        return build(request, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                "인증에 실패했습니다. 이메일 또는 비밀번호를 확인해주세요.", null);
    }

    /* 공통 에러 바디 구성 유틸 */
    // success: 항상 false
    // status: HTTP 상태코드 숫자
    // code: 프론트/로그 용 내부 식별자 (문자열)
    // message: 사용자 표시 메시지 (국문)
    // path: 요청 경로
    // timestamp: UTC 기준 시간(ISO-8601)
    // +extra: 선택 필드 (필드 오류 등)
    private ResponseEntity<Map<String, Object>> build(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message,
            Map<String, Object> extra // 선택
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("status", status.value());
        body.put("code", code);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        body.put("timestamp", java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString());
        if (extra != null) body.putAll(extra);

        return ResponseEntity.status(status).body(body);
    }
}
