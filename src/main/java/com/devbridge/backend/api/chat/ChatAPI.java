package com.devbridge.backend.api.chat;

import com.devbridge.backend.domain.chat.dto.ChatMessageResponse;
import com.devbridge.backend.domain.chat.dto.ChatSessionResponse;
import com.devbridge.backend.domain.chat.dto.CreateChatSessionRequest;
import com.devbridge.backend.domain.chat.dto.CreateOwnerConfirmationRequest;
import com.devbridge.backend.domain.chat.dto.SendMessageRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat", description = "AI 채팅방 및 메시지 API 명세")
@RequestMapping("/api/chats")
public interface ChatAPI {

    @Operation(summary = "채팅방 생성", description = "JWT 인증된 사용자 기준으로 새로운 채팅 세션을 생성합니다.")
    @PostMapping("/sessions")
    ResponseEntity<ChatSessionResponse> createChatSession(
            @RequestHeader("X-Workspace-Id") String workspaceId,
            @AuthenticationPrincipal String employeeId,
            @RequestBody CreateChatSessionRequest request);

    @Operation(summary = "메시지 전송", description = "특정 채팅방에 메시지를 전송하고 AI 답변과 토큰 사용 정보를 받습니다.")
    @PostMapping("/sessions/{sessionId}/messages")
    ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable("sessionId") String sessionId,
            @RequestBody SendMessageRequest request);

    @Operation(summary = "채팅방 목록 조회", description = "최근 메시지 시간 기준 내림차순으로 채팅방 목록을 조회합니다.")
    @GetMapping("/sessions")
    ResponseEntity<List<ChatSessionResponse>> getChatSessions(
            @RequestHeader("X-Workspace-Id") String workspaceId,
            @AuthenticationPrincipal String employeeId);

    @Operation(summary = "채팅 메시지 목록 조회", description = "특정 채팅방의 메시지 내역을 조회합니다.")
    @GetMapping("/sessions/{sessionId}/messages")
    ResponseEntity<List<ChatMessageResponse>> getChatMessages(
            @PathVariable("sessionId") String sessionId,
            @AuthenticationPrincipal String employeeId);

    @Operation(summary = "채팅방 삭제", description = "특정 채팅방을 삭제합니다.")
    @DeleteMapping("/sessions/{sessionId}")
    ResponseEntity<Void> deleteChatSession(
            @PathVariable("sessionId") String sessionId,
            @AuthenticationPrincipal String employeeId);

    @Operation(summary = "담당자 확인 요청", description = "RAG 근거 부족 시 사용자가 선택한 담당자에게 확인 요청을 생성합니다.")
    @PostMapping("/messages/{messageId}/owner-confirmation")
    ResponseEntity<Void> createOwnerConfirmation(
            @PathVariable("messageId") String messageId,
            @AuthenticationPrincipal String employeeId,
            @Valid @RequestBody CreateOwnerConfirmationRequest request);
}
