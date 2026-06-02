package com.devbridge.backend.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChatSessionRequest {
    private String workspaceId;
    private String userId;
    private String sessionTitle;
}
