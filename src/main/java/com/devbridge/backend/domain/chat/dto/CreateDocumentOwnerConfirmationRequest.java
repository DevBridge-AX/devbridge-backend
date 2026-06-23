package com.devbridge.backend.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDocumentOwnerConfirmationRequest(
        @NotBlank(message = "질문 내용은 필수입니다.")
        String questionContent
) {
}
