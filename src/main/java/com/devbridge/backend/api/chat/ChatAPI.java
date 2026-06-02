package com.devbridge.backend.api.chat;

import com.devbridge.backend.domain.chat.dto.ChatMessageResponse;
import com.devbridge.backend.domain.chat.dto.ChatSessionResponse;
import com.devbridge.backend.domain.chat.dto.CreateChatSessionRequest;
import com.devbridge.backend.domain.chat.dto.SendMessageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat", description = "AI 채팅방 및 메시지 API 명세")
@RequestMapping("/api/chats")
public interface ChatAPI {

    @Operation(summary = "채팅방 생성", description = "AI와의 대화를 위한 새로운 채팅 세션을 생성합니다.")
    @PostMapping("/sessions")
    ResponseEntity<ChatSessionResponse> createChatSession(@RequestBody CreateChatSessionRequest request);

    @Operation(summary = "메시지 전송", description = "특정 채팅방에 메시지를 전송하고 AI 답변과 토큰 사용 정보를 받습니다.")
    @PostMapping("/sessions/{sessionId}/messages")
    ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable("sessionId") String sessionId,
            @RequestBody SendMessageRequest request);
}
