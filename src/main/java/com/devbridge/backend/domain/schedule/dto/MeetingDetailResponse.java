package com.devbridge.backend.domain.schedule.dto;

import com.devbridge.backend.domain.schedule.entity.MeetingStatus;

import java.time.LocalDateTime;
import java.util.List;

public record MeetingDetailResponse(
        String meetingId,
        String title,
        String purpose,
        String agenda,
        String location,
        Integer durationMinutes,
        MeetingStatus status,
        LocalDateTime confirmedStartTime,
        LocalDateTime confirmedEndTime,
        List<CandidateTimeSlot> topCandidateTimes,
        List<MeetingParticipantResponse> participants,
        List<MeetingReferenceResponse> references
) {
}
