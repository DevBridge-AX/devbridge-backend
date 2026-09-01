package com.devbridge.backend.global.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[{} {}] {}", errorCode.getHttpStatus().value(), errorCode.name(), e.getMessage());
        return buildResponse(errorCode.getHttpStatus(), errorCode.name(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[400 Bad Request] {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.COMMON_BAD_REQUEST.name(), e.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException e) {
        log.warn("[404 Not Found] {}", e.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ErrorCode.COMMON_NOT_FOUND.name(), e.getMessage());
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(jakarta.validation.ConstraintViolationException e) {
        log.warn("[400 Validation] {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.COMMON_VALIDATION_ERROR.name(), e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException e) {
        log.warn("[403 Forbidden] {}", e.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ErrorCode.COMMON_FORBIDDEN.name(), e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        log.warn("[409 Conflict] {}", e.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ErrorCode.COMMON_CONFLICT.name(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        log.warn("[400 Validation] {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.COMMON_VALIDATION_ERROR.name(), message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
        log.error("[500 Internal Error] {}", e.getMessage(), e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.COMMON_INTERNAL_ERROR.name(), "서버 오류가 발생했습니다.");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("code", code);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
