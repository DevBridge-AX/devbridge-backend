package com.devbridge.backend.domain.schedule.dto;

import com.devbridge.backend.domain.schedule.entity.ParticipantRole;
import com.devbridge.backend.domain.schedule.entity.ParticipantStatus;

public record MeetingParticipantResponse(
        String employeeId,
        ParticipantRole role,
        ParticipantStatus status
) {
}
