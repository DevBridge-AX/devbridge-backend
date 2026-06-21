package com.devbridge.backend.domain.chat.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record ChatSessionResponse(
    String id,
    String workspaceId,
    String employeeId,
    String sessionTitle,
    LocalDateTime lastMessageAt
) {}
