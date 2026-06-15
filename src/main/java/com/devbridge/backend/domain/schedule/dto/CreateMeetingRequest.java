package com.devbridge.backend.domain.schedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateMeetingRequest(
        @NotBlank(message = "회의 제목은 필수입니다.")
        String title,

        String purpose,

        String agenda,

        String location,

        @NotNull(message = "소요 시간은 필수입니다.")
        @Positive(message = "소요 시간은 0보다 커야 합니다.")
        Integer durationMinutes,

        @NotEmpty(message = "참석 대상자 목록은 비어있을 수 없습니다.")
        List<String> participantEmployeeIds,

        @Valid
        List<MeetingReferenceRequest> references
) {
}
