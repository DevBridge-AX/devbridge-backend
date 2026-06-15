package com.devbridge.backend.domain.schedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitAvailableTimesRequest(
        @NotEmpty(message = "가능 시간 목록은 비어있을 수 없습니다.")
        @Valid
        List<TimeSlotRequest> availableTimes
) {
}
