package com.devbridge.backend.domain.chat.dto;

import lombok.Builder;

@Builder
public record ChatMessageResponse(
    String id,
    String sessionId,
    String senderType,
    String content,
    Integer promptTokens,
    Integer completionTokens
) {}
