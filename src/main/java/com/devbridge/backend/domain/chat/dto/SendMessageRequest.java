package com.devbridge.backend.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SendMessageRequest(
    String senderType,
    @NotBlank(message = "메시지 내용은 필수입니다.")
    String content
) {}
