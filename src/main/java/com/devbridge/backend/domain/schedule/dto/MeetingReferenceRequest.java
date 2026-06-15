package com.devbridge.backend.domain.schedule.dto;

import com.devbridge.backend.domain.schedule.entity.ReferenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MeetingReferenceRequest(
        @NotNull(message = "참조 타입은 필수입니다.")
        ReferenceType referenceType,

        String documentId,

        String fileUrl,

        @NotBlank(message = "제목은 필수입니다.")
        String title
) {
}
