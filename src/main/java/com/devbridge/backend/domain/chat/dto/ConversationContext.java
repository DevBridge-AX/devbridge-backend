package com.devbridge.backend.domain.chat.dto;

import com.devbridge.backend.domain.chat.dto.fastapi.FastApiChatRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class ConversationContext {
    private final String workspaceId;
    private final String userId;
    private final List<FastApiChatRequest.ConversationMessage> conversationHistory;
}
