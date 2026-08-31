package com.devbridge.backend.global.common.exception;

import lombok.Getter;

/**
 * {@link ErrorCode}를 담는 범용 비즈니스 예외.
 *
 * <p>메시지에 동적 값(ID 등)이 들어가야 하는 경우 {@link #BusinessException(ErrorCode, String)}로
 * 오버라이드한다 — {@code errorCode}는 응답의 {@code code} 필드와 HTTP 상태코드 매핑에만 쓰이고,
 * 실제로 클라이언트에 노출되는 {@code message}는 항상 이 생성자에 전달된 값이다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
