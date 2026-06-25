package com.devbridge.backend.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record DirectQuestionRequest(
        @NotBlank(message = "담당자 ID는 필수입니다.")
        String assignedOwnerId,

        @NotBlank(message = "질문 내용은 필수입니다.")
        String questionContent
) {
}
