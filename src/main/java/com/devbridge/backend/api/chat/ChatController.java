package com.devbridge.backend.api.chat;

import com.devbridge.backend.domain.chat.dto.ChatMessageResponse;
import com.devbridge.backend.domain.chat.dto.ChatSessionResponse;
import com.devbridge.backend.domain.chat.dto.CreateChatSessionRequest;
import com.devbridge.backend.domain.chat.dto.SendMessageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController implements ChatAPI {

    @Override
    public ResponseEntity<ChatSessionResponse> createChatSession(CreateChatSessionRequest request) {
        ChatSessionResponse response = ChatSessionResponse.builder()
                .id("dummy-session-id")
                .workspaceId(request.getWorkspaceId())
                .userId(request.getUserId())
                .sessionTitle(request.getSessionTitle())
                .build();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ChatMessageResponse> sendMessage(String sessionId, SendMessageRequest request) {
        ChatMessageResponse response = ChatMessageResponse.builder()
                .id("dummy-message-id")
                .sessionId(sessionId)
                .senderType("AI")
                .content("이것은 AI의 모의 답변입니다. 질문해주신 '" + request.getContent() + "' 에 대한 응답입니다.")
                .promptTokens(10)
                .completionTokens(45)
                .build();
        return ResponseEntity.ok(response);
    }
}
