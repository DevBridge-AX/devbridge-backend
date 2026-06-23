package com.devbridge.backend.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOwnerConfirmationRequest(
        @NotBlank(message = "담당자 ID는 필수입니다.")
        String assignedOwnerId
) {
}
