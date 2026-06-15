package com.devbridge.backend.domain.schedule.dto;

import java.time.LocalDateTime;

public record ConfirmedScheduleResponse(
        String meetingId,
        String title,
        LocalDateTime confirmedStartTime,
        LocalDateTime confirmedEndTime
) {
}
