package com.devbridge.backend.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private String id;
    private String sessionId;
    private String senderType;
    private String content;
    private Integer promptTokens;
    private Integer completionTokens;
}
