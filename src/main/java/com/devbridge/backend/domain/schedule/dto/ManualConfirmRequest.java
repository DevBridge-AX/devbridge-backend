package com.devbridge.backend.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ManualConfirmRequest(
        @NotNull(message = "확정 시작 시간은 필수입니다.")
        LocalDateTime confirmedStartTime,

        @NotNull(message = "확정 종료 시간은 필수입니다.")
        LocalDateTime confirmedEndTime
) {
}
