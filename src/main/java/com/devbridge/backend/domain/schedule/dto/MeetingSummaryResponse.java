package com.devbridge.backend.domain.schedule.dto;

import com.devbridge.backend.domain.schedule.entity.MeetingStatus;

import java.time.LocalDateTime;

public record MeetingSummaryResponse(
        String meetingId,
        String title,
        MeetingStatus status,
        Integer durationMinutes,
        LocalDateTime confirmedStartTime,
        LocalDateTime confirmedEndTime
) {
}
