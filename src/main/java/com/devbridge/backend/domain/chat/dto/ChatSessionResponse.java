package com.devbridge.backend.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionResponse {
    private String id;
    private String workspaceId;
    private String employeeId;
    private String sessionTitle;
}
