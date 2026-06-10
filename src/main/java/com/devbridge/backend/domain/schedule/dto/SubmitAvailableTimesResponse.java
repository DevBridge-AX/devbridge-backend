package com.devbridge.backend.domain.schedule.dto;

import com.devbridge.backend.domain.schedule.entity.ParticipantStatus;

public record SubmitAvailableTimesResponse(
        String meetingId,
        String employeeId,
        ParticipantStatus status,
        boolean allParticipantsResponded
) {
}
