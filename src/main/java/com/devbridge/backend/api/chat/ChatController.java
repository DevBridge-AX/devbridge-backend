package com.devbridge.backend.api.chat;

import com.devbridge.backend.domain.chat.dto.ChatMessageResponse;
import com.devbridge.backend.domain.chat.dto.ChatSessionResponse;
import com.devbridge.backend.domain.chat.dto.CreateChatSessionRequest;
import com.devbridge.backend.domain.chat.dto.SendMessageRequest;
import com.devbridge.backend.domain.chat.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController implements ChatAPI {

    private final ChatSessionService chatSessionService;

    @Override
    public ResponseEntity<ChatSessionResponse> createChatSession(String workspaceId, String employeeId, CreateChatSessionRequest request) {
        return ResponseEntity.ok(chatSessionService.createSession(workspaceId, employeeId, request));
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
