package com.devbridge.backend.domain.schedule.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddParticipantsRequest(
        @NotEmpty(message = "추가할 참석자 목록은 비어있을 수 없습니다.")
        List<String> employeeIds
) {
}
