package com.devbridge.backend.domain.chat.dto;

import lombok.Builder;

@Builder
public record CreateChatSessionRequest(
    String sessionTitle
) {}
