package com.devbridge.backend.domain.schedule.dto;

import java.time.LocalDateTime;

public record CandidateTimeSlot(
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
