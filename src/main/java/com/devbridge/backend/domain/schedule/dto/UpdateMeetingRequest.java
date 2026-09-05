package com.devbridge.backend.domain.schedule.dto;

public record UpdateMeetingRequest(
        String title,

        String purpose,

        String agenda,

        String location
) {
}
