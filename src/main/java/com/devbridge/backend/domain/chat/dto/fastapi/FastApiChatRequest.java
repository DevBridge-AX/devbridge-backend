package com.devbridge.backend.domain.chat.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FastApiChatRequest {

    @JsonProperty("session_id")
    private String sessionId;

    private String content;

    @JsonProperty("conversation_history")
    private List<ConversationMessage> conversationHistory;

    @JsonProperty("workspace_id")
    private String workspaceId;

    @JsonProperty("user_id")
    private String userId;

    private String role;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversationMessage {
        private String role;
        private String content;
    }
}
