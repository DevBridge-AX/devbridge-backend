package com.devbridge.backend.domain.chat.service;

import com.devbridge.backend.domain.chat.dto.fastapi.FastApiChatRequest;
import com.devbridge.backend.domain.chat.dto.fastapi.FastApiDoneEvent;
import com.devbridge.backend.domain.chat.entity.ChatMessage;
import com.devbridge.backend.domain.user.entity.JobRole;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.global.config.fastapi.FastApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final FastApiClient fastApiClient;
    private final ChatMessageService chatMessageService;
    private final OwnerConfirmationService ownerConfirmationService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("WebSocket 연결 수립: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("WebSocket 연결 종료: {} ({})", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = payload.path("type").asText();

            if ("chat_message".equals(type)) {
                handleChatMessage(session, payload);
            }
        } catch (Exception e) {
            log.error("WebSocket 메시지 처리 오류: {}", e.getMessage(), e);
            sendMessage(session, createErrorMessage(e.getMessage()));
        }
    }

    private void handleChatMessage(WebSocketSession session, JsonNode payload) {
        String sessionId = payload.path("session_id").asText();
        String content = payload.path("content").asText();

        String employeeId = (String) session.getAttributes().get("employeeId");
        String role = resolveRole(employeeId);

        chatMessageService.saveUserMessage(sessionId, content);

        sendMessage(session, createSearchingMessage());

        List<ChatMessage> history = chatMessageService.getConversationHistory(sessionId);
        List<FastApiChatRequest.ConversationMessage> conversationHistory = buildConversationHistory(history);

        ChatMessage latestUserMsg = history.getLast();
        String workspaceId = latestUserMsg.getSession().getWorkspace().getId();
        String userId = latestUserMsg.getSession().getUser().getId();

        String messageId = java.util.UUID.randomUUID().toString();

        FastApiChatRequest request = FastApiChatRequest.builder()
                .sessionId(sessionId)
                .content(content)
                .conversationHistory(conversationHistory)
                .workspaceId(workspaceId)
                .userId(userId)
                .role(role)
                .build();

        StringBuilder fullContent = new StringBuilder();

        fastApiClient.streamChat(request)
                .doOnError(e -> {
                    log.error("FastAPI SSE 스트림 오류: {}", e.getMessage(), e);
                    sendMessage(session, createErrorMessage("AI 서비스 연결 오류가 발생했습니다."));
                })
                .subscribe(sse -> handleSseEvent(session, sse, sessionId, messageId, fullContent));
    }

    private void handleSseEvent(WebSocketSession session, ServerSentEvent<String> sse,
                                String sessionId, String messageId, StringBuilder fullContent) {
        String event = sse.event();
        String data = sse.data();

        if (event == null || data == null) {
            return;
        }

        switch (event) {
            case "token" -> handleTokenEvent(session, data, messageId, fullContent);
            case "done" -> handleDoneEvent(session, data, sessionId, messageId, fullContent);
            case "error" -> handleErrorEvent(session, data);
        }
    }

    private void handleTokenEvent(WebSocketSession session, String data,
                                  String messageId, StringBuilder fullContent) {
        try {
            JsonNode tokenData = objectMapper.readTree(data);
            String text = tokenData.path("text").asText();
            fullContent.append(text);

            String tokenMessage = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
                put("type", "token");
                put("message_id", messageId);
                put("text", text);
            }});
            sendMessage(session, tokenMessage);
        } catch (JsonProcessingException e) {
            log.error("token 이벤트 파싱 오류: {}", e.getMessage());
        }
    }

    private void handleDoneEvent(WebSocketSession session, String data,
                                 String sessionId, String messageId, StringBuilder fullContent) {
        try {
            FastApiDoneEvent doneEvent = objectMapper.readValue(data, FastApiDoneEvent.class);
            ChatMessage savedMessage = chatMessageService.saveAiMessage(
                    sessionId, messageId, fullContent.toString(), doneEvent);

            String doneMessage = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
                put("type", "done");
                put("message_id", savedMessage.getId());
                put("citations", doneEvent.getCitations());
                put("needs_owner_confirmation", !Boolean.TRUE.equals(doneEvent.getIsGroundable()));
            }});
            sendMessage(session, doneMessage);

            if (!Boolean.TRUE.equals(doneEvent.getIsGroundable())
                    && doneEvent.getSuggestedOwnerId() != null) {
                ownerConfirmationService.triggerOwnerConfirmation(
                        savedMessage.getId(), doneEvent.getSuggestedOwnerId())
                        .ifPresent(ownerName -> {
                            try {
                                String confirmMsg = objectMapper.writeValueAsString(
                                        new java.util.LinkedHashMap<>() {{
                                            put("type", "owner_confirmation_suggested");
                                            put("message_id", savedMessage.getId());
                                            put("suggested_owner_id", doneEvent.getSuggestedOwnerId());
                                            put("suggested_owner_name", ownerName);
                                        }});
                                sendMessage(session, confirmMsg);
                            } catch (JsonProcessingException e) {
                                log.error("owner_confirmation_suggested 메시지 생성 오류: {}", e.getMessage());
                            }
                        });
            }
        } catch (Exception e) {
            log.error("done 이벤트 처리 오류: {}", e.getMessage(), e);
            sendMessage(session, createErrorMessage("응답 처리 중 오류가 발생했습니다."));
        }
    }

    private void handleErrorEvent(WebSocketSession session, String data) {
        try {
            JsonNode errorData = objectMapper.readTree(data);
            String errorMsg = errorData.path("message").asText("알 수 없는 오류");
            sendMessage(session, createErrorMessage(errorMsg));
        } catch (JsonProcessingException e) {
            sendMessage(session, createErrorMessage("AI 서비스 오류가 발생했습니다."));
        }
    }

    private String resolveRole(String employeeId) {
        if (employeeId == null) {
            return JobRole.NEWCOMER.name().toLowerCase();
        }
        return userRepository.findByEmployeeId(employeeId)
                .map(User::getJobRole)
                .map(jobRole -> jobRole.name().toLowerCase())
                .orElse(JobRole.NEWCOMER.name().toLowerCase());
    }

    private List<FastApiChatRequest.ConversationMessage> buildConversationHistory(
            List<ChatMessage> messages) {
        List<FastApiChatRequest.ConversationMessage> history = new ArrayList<>();
        for (ChatMessage msg : messages) {
            String role = "USER".equals(msg.getSenderType()) ? "user" : "assistant";
            history.add(FastApiChatRequest.ConversationMessage.builder()
                    .role(role)
                    .content(msg.getContent())
                    .build());
        }
        return history;
    }

    private String createSearchingMessage() {
        try {
            return objectMapper.writeValueAsString(
                    java.util.Map.of("type", "searching"));
        } catch (JsonProcessingException e) {
            return "{\"type\":\"searching\"}";
        }
    }

    private String createErrorMessage(String message) {
        try {
            return objectMapper.writeValueAsString(
                    java.util.Map.of("type", "error", "message", message));
        } catch (JsonProcessingException e) {
            return "{\"type\":\"error\",\"message\":\"" + message + "\"}";
        }
    }

    void sendMessage(WebSocketSession session, String message) {
        if (session.isOpen()) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                log.error("WebSocket 메시지 전송 실패: {}", e.getMessage());
            }
        }
    }
}
