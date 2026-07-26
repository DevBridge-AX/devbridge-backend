package com.devbridge.backend.domain.chat.service;

import com.devbridge.backend.domain.chat.dto.ConversationContext;
import com.devbridge.backend.domain.chat.dto.fastapi.FastApiChatRequest;
import com.devbridge.backend.domain.chat.dto.fastapi.FastApiDoneEvent;
import com.devbridge.backend.domain.chat.entity.ChatMessage;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.global.config.fastapi.FastApiClient;
import com.devbridge.backend.global.config.websocket.WebSocketContract;
import com.devbridge.backend.global.config.websocket.WebSocketSessionRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ChatWebSocketHandler} 특성 테스트(characterization test).
 *
 * <p>이 핸들러가 프론트엔드로 내보내는 메시지의 `type` 값과 필드명은
 * <b>프론트엔드 레포에 사본이 존재하는 계약</b>이다. 백엔드만 바꾸면 컴파일은 통과하고
 * 화면만 조용히 깨진다 — 2026-06-25 WebSocket 장애와 같은 구조다.
 * 따라서 기대값을 리터럴로 직접 적어 고정한다.
 *
 * <p>AI 엔진(FastAPI)에서 수신하는 SSE 이벤트명(`token`/`done`/`error`) 역시 AI 엔진 레포와의 계약이다.
 */
@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTest {

    private static final String EMPLOYEE_ID = "EMP001";
    private static final String SESSION_ID = "SESSION-001";

    @Mock
    private FastApiClient fastApiClient;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebSocketSessionRegistry webSocketSessionRegistry;

    @Mock
    private WebSocketSession session;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ChatWebSocketHandler handler;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        handler = new ChatWebSocketHandler(
                fastApiClient, chatMessageService, userRepository, objectMapper, webSocketSessionRegistry);
        attributes = new HashMap<>();
        lenient().when(session.getAttributes()).thenReturn(attributes);
        lenient().when(session.isOpen()).thenReturn(true);
        lenient().when(session.getId()).thenReturn("WS-SESSION-1");
    }

    /** 세션으로 전송된 모든 메시지의 payload를 순서대로 반환한다. */
    private List<String> sentMessages() throws Exception {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        return captor.getAllValues().stream().map(TextMessage::getPayload).toList();
    }

    private JsonNode parse(String payload) throws Exception {
        return objectMapper.readTree(payload);
    }

    private FastApiDoneEvent doneEvent(Boolean groundable, String suggestedOwnerId) {
        return new FastApiDoneEvent(
                List.of(), groundable, 0.9, suggestedOwnerId, "v1",
                new FastApiDoneEvent.TokenUsage(
                        new FastApiDoneEvent.TokenUsageDetail("model", 10, 10), null, false));
    }

    @Nested
    @DisplayName("연결 수립 및 인증")
    class Connection {

        @Test
        @DisplayName("afterConnectionEstablished_withAuthenticatedSession_registersSession")
        void afterConnectionEstablished_withAuthenticatedSession_registersSession() throws Exception {
            attributes.put("employeeId", EMPLOYEE_ID);

            handler.afterConnectionEstablished(session);

            verify(webSocketSessionRegistry).register(EMPLOYEE_ID, session);
            verify(session, never()).close(any(CloseStatus.class));
        }

        @Test
        @DisplayName("afterConnectionEstablished_withoutEmployeeIdAttribute_closesWith4401")
        void afterConnectionEstablished_withoutEmployeeIdAttribute_closesWith4401() throws Exception {
            // JwtHandshakeInterceptor가 인증에 실패하면 attribute를 넣지 않고 핸드셰이크만 성립시킨다.
            // 실제 차단은 여기서 이뤄지는 2단 구조다.
            handler.afterConnectionEstablished(session);

            ArgumentCaptor<CloseStatus> captor = ArgumentCaptor.forClass(CloseStatus.class);
            verify(session).close(captor.capture());

            assertThat(captor.getValue().getCode()).isEqualTo(4401);
            assertThat(captor.getValue().getReason()).isEqualTo("Authentication required");
            assertThat(captor.getValue().getCode()).isEqualTo(WebSocketContract.CLOSE_CODE_AUTH_FAILED);
            verify(webSocketSessionRegistry, never()).register(anyString(), any());
        }

        @Test
        @DisplayName("afterConnectionClosed_always_unregistersSession")
        void afterConnectionClosed_always_unregistersSession() {
            handler.afterConnectionClosed(session, CloseStatus.NORMAL);

            verify(webSocketSessionRegistry).unregister(session);
        }

        @Test
        @DisplayName("handleTextMessage_withoutEmployeeIdAttribute_closesWith4401")
        void handleTextMessage_withoutEmployeeIdAttribute_closesWith4401() throws Exception {
            handler.handleMessage(session, new TextMessage("{\"type\":\"chat_message\"}"));

            ArgumentCaptor<CloseStatus> captor = ArgumentCaptor.forClass(CloseStatus.class);
            verify(session).close(captor.capture());
            assertThat(captor.getValue().getCode()).isEqualTo(4401);
            // 인증되지 않은 세션의 메시지는 처리되지 않는다.
            verify(chatMessageService, never()).saveUserMessage(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("메시지 수신 처리")
    class InboundMessage {

        @BeforeEach
        void authenticate() {
            attributes.put("employeeId", EMPLOYEE_ID);
            attributes.put("jobRole", "developer");
        }

        @Test
        @DisplayName("handleTextMessage_withUnknownType_isIgnoredSilently")
        void handleTextMessage_withUnknownType_isIgnoredSilently() throws Exception {
            handler.handleMessage(session, new TextMessage("{\"type\":\"ping\"}"));

            // chat_message 외의 type은 무시된다. 에러 응답도 보내지 않는다.
            verify(chatMessageService, never()).saveUserMessage(anyString(), anyString());
            verify(session, never()).sendMessage(any());
        }

        @Test
        @DisplayName("handleTextMessage_withMalformedJson_sendsErrorMessage")
        void handleTextMessage_withMalformedJson_sendsErrorMessage() throws Exception {
            handler.handleMessage(session, new TextMessage("not-json"));

            JsonNode error = parse(sentMessages().getFirst());
            assertThat(error.path("type").asText()).isEqualTo("error");
            assertThat(error.path("message").asText()).isNotBlank();
        }

        @Test
        @DisplayName("handleTextMessage_withChatMessage_sendsSearchingThenCallsFastApi")
        void handleTextMessage_withChatMessage_sendsSearchingThenCallsFastApi() throws Exception {
            when(chatMessageService.getConversationContext(SESSION_ID)).thenReturn(
                    ConversationContext.builder()
                            .workspaceId("WS-001").userId("USER-001").conversationHistory(List.of()).build());
            when(fastApiClient.streamChat(any(FastApiChatRequest.class))).thenReturn(Flux.empty());

            handler.handleMessage(session, new TextMessage(
                    "{\"type\":\"chat_message\",\"session_id\":\"" + SESSION_ID + "\",\"content\":\"질문입니다\"}"));

            verify(chatMessageService).saveUserMessage(SESSION_ID, "질문입니다");

            // 프론트는 이 'searching' 메시지를 받아 로딩 상태를 표시한다.
            JsonNode searching = parse(sentMessages().getFirst());
            assertThat(searching.path("type").asText()).isEqualTo("searching");

            // 핸드셰이크에서 저장한 jobRole이 AI 엔진 요청의 role로 전달된다.
            ArgumentCaptor<FastApiChatRequest> captor = ArgumentCaptor.forClass(FastApiChatRequest.class);
            verify(fastApiClient).streamChat(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo("developer");
            assertThat(captor.getValue().getWorkspaceId()).isEqualTo("WS-001");
            assertThat(captor.getValue().getUserId()).isEqualTo("USER-001");
            assertThat(captor.getValue().getContent()).isEqualTo("질문입니다");
        }
    }

    @Nested
    @DisplayName("SSE 이벤트 → 프론트 메시지 변환 (계약)")
    class SseTranslation {

        @BeforeEach
        void authenticate() {
            attributes.put("employeeId", EMPLOYEE_ID);
            attributes.put("jobRole", "developer");
            lenient().when(chatMessageService.getConversationContext(SESSION_ID)).thenReturn(
                    ConversationContext.builder()
                            .workspaceId("WS-001").userId("USER-001").conversationHistory(List.of()).build());
        }

        private void receive(ServerSentEvent<String>... events) throws Exception {
            when(fastApiClient.streamChat(any(FastApiChatRequest.class))).thenReturn(Flux.just(events));
            handler.handleMessage(session, new TextMessage(
                    "{\"type\":\"chat_message\",\"session_id\":\"" + SESSION_ID + "\",\"content\":\"질문\"}"));
        }

        private ServerSentEvent<String> sse(String event, String data) {
            return ServerSentEvent.<String>builder().event(event).data(data).build();
        }

        @Test
        @DisplayName("handleSseEvent_withTokenEvent_forwardsTextWithSameMessageId")
        void handleSseEvent_withTokenEvent_forwardsTextWithSameMessageId() throws Exception {
            receive(sse("token", "{\"text\":\"안녕\"}"), sse("token", "{\"text\":\"하세요\"}"));

            List<String> messages = sentMessages();
            JsonNode first = parse(messages.get(1));
            JsonNode second = parse(messages.get(2));

            assertThat(first.path("type").asText()).isEqualTo("token");
            assertThat(first.path("text").asText()).isEqualTo("안녕");
            assertThat(second.path("text").asText()).isEqualTo("하세요");
            // 스트리밍 중 message_id는 동일해야 프론트가 같은 말풍선에 이어붙일 수 있다.
            assertThat(first.path("message_id").asText())
                    .isNotBlank()
                    .isEqualTo(second.path("message_id").asText());
        }

        @Test
        @DisplayName("handleSseEvent_withDoneEvent_sendsDoneWithPersistedMessageId")
        void handleSseEvent_withDoneEvent_sendsDoneWithPersistedMessageId() throws Exception {
            when(chatMessageService.saveAiMessage(anyString(), anyString(), anyString(), any()))
                    .thenReturn(ChatMessage.builder().id("SAVED-ID").build());

            receive(sse("token", "{\"text\":\"안녕\"}"),
                    sse("done", "{\"is_groundable\":true,\"confidence\":0.9,"
                            + "\"token_usage\":{\"main\":{\"model\":\"m\",\"prompt_tokens\":1,\"completion_tokens\":1}}}"));

            JsonNode done = parse(sentMessages().getLast());
            assertThat(done.path("type").asText()).isEqualTo("done");
            // done에서는 DB에 저장된 ID를 내려준다(스트리밍 중 임시 ID와 다를 수 있다).
            assertThat(done.path("message_id").asText()).isEqualTo("SAVED-ID");
            assertThat(done.path("needs_owner_confirmation").asBoolean()).isFalse();
            assertThat(done.has("citations")).isTrue();
        }

        @Test
        @DisplayName("handleSseEvent_withNotGroundableDone_setsNeedsOwnerConfirmationTrue")
        void handleSseEvent_withNotGroundableDone_setsNeedsOwnerConfirmationTrue() throws Exception {
            when(chatMessageService.saveAiMessage(anyString(), anyString(), anyString(), any()))
                    .thenReturn(ChatMessage.builder().id("SAVED-ID").build());

            receive(sse("done", "{\"is_groundable\":false,\"confidence\":0.2,"
                    + "\"token_usage\":{\"main\":{\"model\":\"m\",\"prompt_tokens\":1,\"completion_tokens\":1}}}"));

            JsonNode done = parse(sentMessages().getLast());
            // is_groundable이 true가 아니면 담당자 확인이 필요하다고 프론트에 알린다.
            assertThat(done.path("needs_owner_confirmation").asBoolean()).isTrue();
        }

        @Test
        @DisplayName("handleSseEvent_withSuggestedOwner_sendsOwnerConfirmationSuggestedMessage")
        void handleSseEvent_withSuggestedOwner_sendsOwnerConfirmationSuggestedMessage() throws Exception {
            when(chatMessageService.saveAiMessage(anyString(), anyString(), anyString(), any()))
                    .thenReturn(ChatMessage.builder().id("SAVED-ID").build());
            when(userRepository.findById("OWNER-001")).thenReturn(
                    Optional.of(User.builder().id("OWNER-001").name("담당자").build()));

            receive(sse("done", "{\"is_groundable\":false,\"confidence\":0.2,"
                    + "\"suggested_owner_id\":\"OWNER-001\","
                    + "\"token_usage\":{\"main\":{\"model\":\"m\",\"prompt_tokens\":1,\"completion_tokens\":1}}}"));

            JsonNode suggested = parse(sentMessages().getLast());
            assertThat(suggested.path("type").asText()).isEqualTo("owner_confirmation_suggested");
            assertThat(suggested.path("suggested_owner_id").asText()).isEqualTo("OWNER-001");
            assertThat(suggested.path("suggested_owner_name").asText()).isEqualTo("담당자");
            assertThat(suggested.path("message_id").asText()).isEqualTo("SAVED-ID");
        }

        @Test
        @DisplayName("handleSseEvent_withGroundableDoneAndSuggestedOwner_doesNotSuggestOwner")
        void handleSseEvent_withGroundableDoneAndSuggestedOwner_doesNotSuggestOwner() throws Exception {
            when(chatMessageService.saveAiMessage(anyString(), anyString(), anyString(), any()))
                    .thenReturn(ChatMessage.builder().id("SAVED-ID").build());

            receive(sse("done", "{\"is_groundable\":true,\"confidence\":0.9,"
                    + "\"suggested_owner_id\":\"OWNER-001\","
                    + "\"token_usage\":{\"main\":{\"model\":\"m\",\"prompt_tokens\":1,\"completion_tokens\":1}}}"));

            // 근거가 충분하면 담당자 추천 메시지를 보내지 않는다(조회조차 하지 않는다).
            assertThat(parse(sentMessages().getLast()).path("type").asText()).isEqualTo("done");
            verify(userRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("handleSseEvent_withErrorEvent_forwardsErrorMessage")
        void handleSseEvent_withErrorEvent_forwardsErrorMessage() throws Exception {
            receive(sse("error", "{\"message\":\"AI 처리 실패\"}"));

            JsonNode error = parse(sentMessages().getLast());
            assertThat(error.path("type").asText()).isEqualTo("error");
            assertThat(error.path("message").asText()).isEqualTo("AI 처리 실패");
        }

        @Test
        @DisplayName("handleSseEvent_withErrorEventMissingMessage_usesDefaultText")
        void handleSseEvent_withErrorEventMissingMessage_usesDefaultText() throws Exception {
            receive(sse("error", "{}"));

            assertThat(parse(sentMessages().getLast()).path("message").asText())
                    .isEqualTo("알 수 없는 오류");
        }

        @Test
        @DisplayName("handleSseEvent_withUnknownEventName_isIgnored")
        void handleSseEvent_withUnknownEventName_isIgnored() throws Exception {
            receive(sse("heartbeat", "{}"));

            // token/done/error 외의 이벤트는 무시된다. searching 메시지만 전송된 상태다.
            List<String> messages = sentMessages();
            assertThat(messages).hasSize(1);
            assertThat(parse(messages.getFirst()).path("type").asText()).isEqualTo("searching");
        }

        @Test
        @DisplayName("handleSseEvent_withMalformedDoneData_sendsErrorMessage")
        void handleSseEvent_withMalformedDoneData_sendsErrorMessage() throws Exception {
            receive(sse("done", "not-json"));

            JsonNode error = parse(sentMessages().getLast());
            assertThat(error.path("type").asText()).isEqualTo("error");
            assertThat(error.path("message").asText()).isEqualTo("응답 처리 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("handleSseEvent_withMalformedTokenData_isSkippedWithoutErrorMessage")
        void handleSseEvent_withMalformedTokenData_isSkippedWithoutErrorMessage() throws Exception {
            receive(sse("token", "not-json"), sse("token", "{\"text\":\"정상\"}"));

            // token 파싱 실패는 로그만 남기고 넘어간다. done과 달리 error 메시지를 보내지 않으므로
            // 프론트는 일부 토큰이 유실된 사실을 알 수 없다.
            List<String> messages = sentMessages();
            assertThat(messages).hasSize(2);
            assertThat(parse(messages.getFirst()).path("type").asText()).isEqualTo("searching");
            assertThat(parse(messages.get(1)).path("text").asText()).isEqualTo("정상");
        }

        @Test
        @DisplayName("handleSseEvent_withNullEventName_isIgnored")
        void handleSseEvent_withNullEventName_isIgnored() throws Exception {
            receive(ServerSentEvent.<String>builder().data("{\"text\":\"본문\"}").build());

            // event나 data가 null이면 즉시 반환한다(주석 이벤트·keep-alive 대응).
            assertThat(sentMessages()).hasSize(1);
        }

        @Test
        @DisplayName("streamChat_whenFastApiStreamFails_sendsConnectionErrorMessage")
        void streamChat_whenFastApiStreamFails_sendsConnectionErrorMessage() throws Exception {
            when(fastApiClient.streamChat(any(FastApiChatRequest.class)))
                    .thenReturn(Flux.error(new RuntimeException("connection refused")));

            handler.handleMessage(session, new TextMessage(
                    "{\"type\":\"chat_message\",\"session_id\":\"" + SESSION_ID + "\",\"content\":\"질문\"}"));

            // AI 엔진이 죽어 있을 때 프론트가 받는 유일한 신호다. 문구가 바뀌면 화면 처리도 함께 확인해야 한다.
            JsonNode error = parse(sentMessages().getLast());
            assertThat(error.path("type").asText()).isEqualTo("error");
            assertThat(error.path("message").asText()).isEqualTo("AI 서비스 연결 오류가 발생했습니다.");
            // 사용자 메시지는 이미 저장된 뒤이므로 질문은 남고 답변만 없는 상태가 된다.
            verify(chatMessageService).saveUserMessage(SESSION_ID, "질문");
            verify(chatMessageService, never()).saveAiMessage(anyString(), anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("sendMessage")
    class SendMessage {

        @Test
        @DisplayName("sendMessage_withClosedSession_doesNotSend")
        void sendMessage_withClosedSession_doesNotSend() throws Exception {
            when(session.isOpen()).thenReturn(false);

            handler.sendMessage(session, "{\"type\":\"token\"}");

            verify(session, never()).sendMessage(any());
        }
    }
}
