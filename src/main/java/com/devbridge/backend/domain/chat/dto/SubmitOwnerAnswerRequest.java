package com.devbridge.backend.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitOwnerAnswerRequest(
        @NotBlank(message = "답변 내용은 필수입니다.")
        String answerContent
) {}
