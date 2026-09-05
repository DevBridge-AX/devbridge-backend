package com.devbridge.backend.global.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 도메인 전체가 공유하는 예외 코드 자산.
 *
 * <p>도메인 접두사(AUTH_, CHAT_, SCHEDULE_, SETTING_ ...)로 구분한다. HTTP 상태코드는
 * 기존 raw throw(IllegalArgumentException→400, IllegalStateException→409)가 실제로
 * 매핑되던 상태코드를 그대로 보존한 것이며, 새로 정한 값이 아니다.
 *
 * <p>{@code COMMON_*}은 아직 {@link BusinessException}으로 옮기지 않은 raw throw를 위한
 * fallback 코드다. {@link GlobalExceptionHandler}가 이 코드들을 사용해, 마이그레이션
 * 진행률과 무관하게 응답에 {@code code} 필드가 항상 존재하도록 한다.
 */
@Getter
public enum ErrorCode {

    // Common (미마이그레이션 raw throw용 fallback)
    COMMON_BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    COMMON_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    COMMON_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    COMMON_CONFLICT(HttpStatus.CONFLICT, "요청을 처리할 수 없는 상태입니다."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // auth
    AUTH_HR_NOT_FOUND(HttpStatus.BAD_REQUEST, "일치하는 사원 정보를 찾을 수 없습니다."),
    AUTH_INACTIVE_EMPLOYEE(HttpStatus.BAD_REQUEST, "비활성화된 사원입니다."),
    AUTH_EMPLOYEE_EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "사번과 등록된 이메일 정보가 일치하지 않습니다."),
    // 상태코드가 부적절하다는 게 이미 알려져 있으나(프론트 합의 전까지 500 유지) 그대로 보존한다.
    AUTH_EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송 중 오류가 발생했습니다."),
    AUTH_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 불일치하거나 만료되었습니다."),
    AUTH_EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),
    AUTH_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "인증 실패"),
    AUTH_CREDENTIAL_MISMATCH(HttpStatus.BAD_REQUEST, "사번 또는 비밀번호가 일치하지 않습니다."),

    // chat
    CHAT_USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 사용자가 존재하지 않습니다."),
    CHAT_SESSION_NOT_FOUND(HttpStatus.BAD_REQUEST, "세션을 찾을 수 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 채팅 메시지가 존재하지 않습니다."),
    CHAT_ANSWER_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 답변이 완료된 요청입니다."),
    CHAT_OWNER_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "이미 담당자가 배정된 질문입니다."),
    CHAT_CANNOT_QUESTION_SELF(HttpStatus.BAD_REQUEST, "본인에게는 질문을 보낼 수 없습니다."),
    CHAT_DOCUMENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 문서가 존재하지 않습니다."),
    CHAT_DOCUMENT_UPLOADER_MISSING(HttpStatus.BAD_REQUEST, "등록자 정보가 없는 문서입니다. 담당자를 지정할 수 없습니다."),
    CHAT_CANNOT_QUESTION_OWN_DOCUMENT(HttpStatus.BAD_REQUEST, "본인이 등록한 문서입니다."),
    CHAT_CONFIRMATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 확인 요청이 존재하지 않습니다."),

    // schedule
    SCHEDULE_USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 사용자가 존재하지 않습니다."),
    SCHEDULE_NOT_PARTICIPANT(HttpStatus.BAD_REQUEST, "해당 회의의 참석자가 아닙니다."),
    SCHEDULE_MEETING_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 회의가 존재하지 않습니다."),
    SCHEDULE_REFERENCE_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 회의의 첨부파일이 아닙니다."),
    SCHEDULE_DOCUMENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 문서가 존재하지 않습니다."),
    SCHEDULE_NOT_HOST(HttpStatus.BAD_REQUEST, "회의 주최자만 회의 정보를 수정할 수 있습니다."),
    SCHEDULE_INVALID_TITLE(HttpStatus.BAD_REQUEST, "회의 제목은 빈 값일 수 없습니다."),
    SCHEDULE_CANDIDATE_SERIALIZE_FAILED(HttpStatus.CONFLICT, "후보 시간 목록 직렬화에 실패했습니다."),
    SCHEDULE_CANDIDATE_DESERIALIZE_FAILED(HttpStatus.CONFLICT, "후보 시간 목록 역직렬화에 실패했습니다."),
    SCHEDULE_ALREADY_CANCELED(HttpStatus.CONFLICT, "이미 취소된 회의입니다."),
    SCHEDULE_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확정된 회의입니다."),
    SCHEDULE_INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "종료 시간은 시작 시간보다 이후여야 합니다."),
    SCHEDULE_INVALID_STATUS_FOR_REOPEN(HttpStatus.CONFLICT, "재조율은 후보 시간 선정(SELECTING) 상태에서만 가능합니다."),
    SCHEDULE_PARTICIPANT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 참석자로 등록된 사용자입니다."),
    SCHEDULE_PARTICIPANT_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 참석자를 찾을 수 없습니다."),
    SCHEDULE_CANNOT_REMOVE_HOST(HttpStatus.BAD_REQUEST, "회의 주최자는 제외할 수 없습니다."),

    // setting
    SETTING_USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "사용자를 찾을 수 없습니다."),
    SETTING_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
