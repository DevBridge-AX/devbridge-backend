package com.devbridge.backend.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMeetingRequest(
        @NotBlank(message = "회의 제목은 필수입니다.")
        String title,

        String purpose,

        String agenda,

        String location
) {
}
