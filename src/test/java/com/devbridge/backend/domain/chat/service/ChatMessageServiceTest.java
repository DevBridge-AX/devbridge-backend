package com.devbridge.backend.domain.chat.service;

import com.devbridge.backend.domain.chat.dto.ConversationContext;
import com.devbridge.backend.domain.chat.dto.fastapi.FastApiDoneEvent;
import com.devbridge.backend.domain.chat.entity.ChatMessage;
import com.devbridge.backend.domain.chat.entity.ChatSession;
import com.devbridge.backend.domain.chat.entity.CitationSourceType;
import com.devbridge.backend.domain.chat.entity.MessageCitation;
import com.devbridge.backend.domain.chat.repository.ChatMessageRepository;
import com.devbridge.backend.domain.chat.repository.ChatSessionRepository;
import com.devbridge.backend.domain.chat.repository.MessageCitationRepository;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.global.common.exception.ForbiddenException;
import com.devbridge.backend.global.config.fastapi.FastApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ChatMessageService} 특성 테스트(characterization test).
 *
 * <p>"올바른 동작"이 아니라 <b>현재 동작</b>을 그대로 고정하는 것이 목적이다.
 *
 * <p>이 클래스는 AI 엔진(FastAPI)의 `done` 이벤트를 DB로 변환하는 지점이라
 * <b>레포 간 계약이 코드로 드러나는 자리</b>다. 응답 필드 구조가 바뀌면 여기서 먼저 깨진다.
 * 특히 토큰 사용량 기반 비용 계산은 값이 조용히 틀려도 아무도 모르는 종류의 로직이라 수치를 고정한다.
 */
@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    private static final String SESSION_ID = "SESSION-001";
    private static final String MESSAGE_ID = "MSG-001";
    private static final String MODEL = "gpt-4o-mini";
    private static final String REWRITE_MODEL = "gpt-4o-mini-rewrite";

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private MessageCitationRepository messageCitationRepository;

    @Mock
    private FastApiProperties fastApiProperties;

    private ChatMessageService chatMessageService;

    private ChatSession session;

    @BeforeEach
    void setUp() {
        chatMessageService = new ChatMessageService(
                chatMessageRepository, chatSessionRepository, messageCitationRepository, fastApiProperties);

        session = ChatSession.builder()
                .id(SESSION_ID)
                .workspace(Workspace.builder().id("WS-001").name("워크스페이스").build())
                .user(User.builder().id("USER-001").employeeId("EMP001").build())
                .build();
    }

    private FastApiDoneEvent.TokenUsageDetail usage(String model, int prompt, int completion) {
        return new FastApiDoneEvent.TokenUsageDetail(model, prompt, completion);
    }

    private FastApiDoneEvent doneEvent(
            List<FastApiDoneEvent.CitationData> citations,
            Boolean groundable,
            String suggestedOwnerId,
            FastApiDoneEvent.TokenUsage tokenUsage
    ) {
        return new FastApiDoneEvent(citations, groundable, 0.87, suggestedOwnerId, "v1.2", tokenUsage);
    }

    private void givenCreditRates(Map<String, FastApiProperties.CreditRate> rates) {
        when(fastApiProperties.getCreditRates()).thenReturn(rates);
    }

    private FastApiProperties.CreditRate rate(String input, String output) {
        FastApiProperties.CreditRate creditRate = new FastApiProperties.CreditRate();
        creditRate.setInput(new BigDecimal(input));
        creditRate.setOutput(new BigDecimal(output));
        return creditRate;
    }

    @Nested
    @DisplayName("saveUserMessage")
    class SaveUserMessage {

        @Test
        @DisplayName("saveUserMessage_withValidSession_savesMessageWithUserSenderType")
        void saveUserMessage_withValidSession_savesMessageWithUserSenderType() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(chatMessageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            chatMessageService.saveUserMessage(SESSION_ID, "안녕하세요");

            ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
            verify(chatMessageRepository).save(captor.capture());
            assertThat(captor.getValue().getSenderType()).isEqualTo("USER");
            assertThat(captor.getValue().getContent()).isEqualTo("안녕하세요");
        }

        @Test
        @DisplayName("saveUserMessage_withUnknownSession_throwsIllegalArgumentException")
        void saveUserMessage_withUnknownSession_throwsIllegalArgumentException() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatMessageService.saveUserMessage(SESSION_ID, "안녕하세요"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("세션을 찾을 수 없습니다: " + SESSION_ID);

            verify(chatMessageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("saveAiMessage")
    class SaveAiMessage {

        @Test
        @DisplayName("saveAiMessage_withDoneEvent_mapsFastApiFieldsToEntity")
        void saveAiMessage_withDoneEvent_mapsFastApiFieldsToEntity() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(chatMessageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            givenCreditRates(Map.of(MODEL, rate("1.0", "2.0")));

            FastApiDoneEvent.TokenUsage tokenUsage =
                    new FastApiDoneEvent.TokenUsage(usage(MODEL, 1000, 500), null, true);

            chatMessageService.saveAiMessage(SESSION_ID, MESSAGE_ID, "AI 응답 본문",
                    doneEvent(null, true, null, tokenUsage));

            ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
            verify(chatMessageRepository).save(captor.capture());

            ChatMessage saved = captor.getValue();
            // messageId는 스트리밍 중 프론트에 먼저 전달된 값이므로 그대로 PK가 되어야 한다.
            assertThat(saved.getId()).isEqualTo(MESSAGE_ID);
            assertThat(saved.getSenderType()).isEqualTo("AI");
            assertThat(saved.getContent()).isEqualTo("AI 응답 본문");
            assertThat(saved.getIsGroundable()).isTrue();
            assertThat(saved.getConfidenceScore()).isEqualByComparingTo(BigDecimal.valueOf(0.87));
            assertThat(saved.getContextTruncated()).isTrue();
            assertThat(saved.getPromptVersion()).isEqualTo("v1.2");
            assertThat(saved.getPromptTokens()).isEqualTo(1000);
            assertThat(saved.getCompletionTokens()).isEqualTo(500);
        }

        @Test
        @DisplayName("saveAiMessage_withoutRewriteUsage_leavesRewriteFieldsNull")
        void saveAiMessage_withoutRewriteUsage_leavesRewriteFieldsNull() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(chatMessageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            givenCreditRates(Map.of(MODEL, rate("1.0", "2.0")));

            FastApiDoneEvent.TokenUsage tokenUsage =
                    new FastApiDoneEvent.TokenUsage(usage(MODEL, 100, 50), null, false);

            chatMessageService.saveAiMessage(SESSION_ID, MESSAGE_ID, "본문",
                    doneEvent(null, true, null, tokenUsage));

            ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
            verify(chatMessageRepository).save(captor.capture());

            // turn1(rewriter 미사용)에서는 rewrite 관련 필드가 전부 null이다.
            assertThat(captor.getValue().getRewriteModel()).isNull();
            assertThat(captor.getValue().getRewritePromptTokens()).isNull();
            assertThat(captor.getValue().getRewriteCompletionTokens()).isNull();
        }

        @Test
        @DisplayName("saveAiMessage_withRewriteUsage_sumsCostOfBothModels")
        void saveAiMessage_withRewriteUsage_sumsCostOfBothModels() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(chatMessageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            givenCreditRates(Map.of(
                    MODEL, rate("1.0", "2.0"),
                    REWRITE_MODEL, rate("0.5", "1.0")));

            FastApiDoneEvent.TokenUsage tokenUsage = new FastApiDoneEvent.TokenUsage(
                    usage(MODEL, 1000, 500), usage(REWRITE_MODEL, 200, 100), false);

            chatMessageService.saveAiMessage(SESSION_ID, MESSAGE_ID, "본문",
                    doneEvent(null, true, null, tokenUsage));

            ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
            verify(chatMessageRepository).save(captor.capture());

            // 단가는 1000토큰 단위다.
            // main:    1.0*1000/1000 + 2.0*500/1000  = 1.0 + 1.0  = 2.0
            // rewrite: 0.5*200/1000  + 1.0*100/1000  = 0.1 + 0.1  = 0.2
            assertThat(captor.getValue().getEstimatedCost()).isEqualByComparingTo(new BigDecimal("2.2"));
            assertThat(captor.getValue().getRewriteModel()).isEqualTo(REWRITE_MODEL);
            assertThat(captor.getValue().getRewritePromptTokens()).isEqualTo(200);
        }

        @Test
        @DisplayName("saveAiMessage_withUnknownModel_fallsBackToZeroCost")
        void saveAiMessage_withUnknownModel_fallsBackToZeroCost() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(chatMessageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            givenCreditRates(Map.of("other-model", rate("1.0", "2.0")));

            FastApiDoneEvent.TokenUsage tokenUsage =
                    new FastApiDoneEvent.TokenUsage(usage(MODEL, 1000, 500), null, false);

            chatMessageService.saveAiMessage(SESSION_ID, MESSAGE_ID, "본문",
                    doneEvent(null, true, null, tokenUsage));

            ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
            verify(chatMessageRepository).save(captor.capture());

            // 단가표에 없는 모델은 경고 로그만 남기고 비용 0으로 처리한다(요청은 실패하지 않는다).
            assertThat(captor.getValue().getEstimatedCost()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("saveAiMessage_withNullCreditRates_fallsBackToZeroCost")
        void saveAiMessage_withNullCreditRates_fallsBackToZeroCost() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(chatMessageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            givenCreditRates(null);

            FastApiDoneEvent.TokenUsage tokenUsage =
                    new FastApiDoneEvent.TokenUsage(usage(MODEL, 1000, 500), null, false);

            chatMessageService.saveAiMessage(SESSION_ID, MESSAGE_ID, "본문",
                    doneEvent(null, true, null, tokenUsage));

            ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
            verify(chatMessageRepository).save(captor.capture());
            assertThat(captor.getValue().getEstimatedCost()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("saveAiMessage_withCitations_savesEachCitationWithUpperCasedSourceType")
        void saveAiMessage_withCitations_savesEachCitationWithUpperCasedSourceType() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(chatMessageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            givenCreditRates(Map.of(MODEL, rate("1.0", "2.0")));

            FastApiDoneEvent.TokenUsage tokenUsage =
                    new FastApiDoneEvent.TokenUsage(usage(MODEL, 10, 10), null, false);
            // AI 엔진은 소문자로 보내고 백엔드가 대문자 enum으로 변환한다.
            var citation = new FastApiDoneEvent.CitationData(
                    "document", "DOC-001", 3, "요구사항 정의서", 0.91);

            chatMessageService.saveAiMessage(SESSION_ID, MESSAGE_ID, "본문",
                    doneEvent(List.of(citation), true, null, tokenUsage));

            ArgumentCaptor<MessageCitation> captor = ArgumentCaptor.forClass(MessageCitation.class);
            verify(messageCitationRepository).save(captor.capture());

            MessageCitation saved = captor.getValue();
            assertThat(saved.getSourceType()).isEqualTo(CitationSourceType.DOCUMENT);
            assertThat(saved.getSourceId()).isEqualTo("DOC-001");
            assertThat(saved.getVectorChunkId()).isEqualTo(3);
            assertThat(saved.getTitle()).isEqualTo("요구사항 정의서");
            assertThat(saved.getSimilarityScore()).isEqualByComparingTo(BigDecimal.valueOf(0.91));
        }

        @Test
        @DisplayName("saveAiMessage_withEmptyCitations_savesNoCitation")
        void saveAiMessage_withEmptyCitations_savesNoCitation() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(chatMessageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            givenCreditRates(Map.of(MODEL, rate("1.0", "2.0")));

            FastApiDoneEvent.TokenUsage tokenUsage =
                    new FastApiDoneEvent.TokenUsage(usage(MODEL, 10, 10), null, false);

            chatMessageService.saveAiMessage(SESSION_ID, MESSAGE_ID, "본문",
                    doneEvent(List.of(), true, null, tokenUsage));

            verify(messageCitationRepository, never()).save(any());
        }

        @Test
        @DisplayName("saveAiMessage_withUnknownCitationSourceType_throwsIllegalArgumentException")
        void saveAiMessage_withUnknownCitationSourceType_throwsIllegalArgumentException() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(chatMessageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            givenCreditRates(Map.of(MODEL, rate("1.0", "2.0")));

            FastApiDoneEvent.TokenUsage tokenUsage =
                    new FastApiDoneEvent.TokenUsage(usage(MODEL, 10, 10), null, false);
            var citation = new FastApiDoneEvent.CitationData(
                    "unknown_source", "SRC-001", 1, "제목", 0.5);

            // AI 엔진이 CitationSourceType에 없는 값을 보내면 enum 변환에서 실패한다.
            // 메시지는 이미 저장된 뒤이므로 인용만 유실되는 것이 아니라 트랜잭션이 롤백된다.
            assertThatThrownBy(() -> chatMessageService.saveAiMessage(
                    SESSION_ID, MESSAGE_ID, "본문", doneEvent(List.of(citation), true, null, tokenUsage)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("saveAiMessage_withUnknownSession_throwsIllegalArgumentException")
        void saveAiMessage_withUnknownSession_throwsIllegalArgumentException() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            FastApiDoneEvent.TokenUsage tokenUsage =
                    new FastApiDoneEvent.TokenUsage(usage(MODEL, 10, 10), null, false);

            assertThatThrownBy(() -> chatMessageService.saveAiMessage(
                    SESSION_ID, MESSAGE_ID, "본문", doneEvent(null, true, null, tokenUsage)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("세션을 찾을 수 없습니다: " + SESSION_ID);
        }
    }

    @Nested
    @DisplayName("getConversationContext")
    class GetConversationContext {

        @Test
        @DisplayName("getConversationContext_withMixedSenderTypes_mapsUserAndAssistantRoles")
        void getConversationContext_withMixedSenderTypes_mapsUserAndAssistantRoles() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(chatMessageRepository.findBySession_IdOrderByCreatedAtAsc(SESSION_ID)).thenReturn(List.of(
                    ChatMessage.builder().session(session).senderType("USER").content("질문").build(),
                    ChatMessage.builder().session(session).senderType("AI").content("답변").build(),
                    ChatMessage.builder().session(session).senderType("OWNER").content("담당자 답변").build()
            ));

            ConversationContext context = chatMessageService.getConversationContext(SESSION_ID);

            assertThat(context.getWorkspaceId()).isEqualTo("WS-001");
            assertThat(context.getUserId()).isEqualTo("USER-001");
            // USER 외의 senderType은 전부 assistant로 매핑된다. OWNER도 assistant가 된다.
            assertThat(context.getConversationHistory())
                    .extracting("role")
                    .containsExactly("user", "assistant", "assistant");
        }

        @Test
        @DisplayName("getConversationContext_withUnknownSession_throwsIllegalArgumentException")
        void getConversationContext_withUnknownSession_throwsIllegalArgumentException() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatMessageService.getConversationContext(SESSION_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("세션을 찾을 수 없습니다: " + SESSION_ID);
        }
    }

    @Nested
    @DisplayName("getMessages")
    class GetMessages {

        @Test
        @DisplayName("getMessages_withSessionOwner_mapsToResponseInCreatedOrder")
        void getMessages_withSessionOwner_mapsToResponseInCreatedOrder() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(chatMessageRepository.findBySession_IdOrderByCreatedAtAsc(SESSION_ID)).thenReturn(List.of(
                    ChatMessage.builder().id("M1").session(session).senderType("USER").content("질문")
                            .promptTokens(10).completionTokens(0).build(),
                    ChatMessage.builder().id("M2").session(session).senderType("AI").content("답변")
                            .promptTokens(100).completionTokens(50).build()
            ));

            var responses = chatMessageService.getMessages(SESSION_ID, "EMP001");

            assertThat(responses).hasSize(2);
            assertThat(responses.getFirst().id()).isEqualTo("M1");
            assertThat(responses.getFirst().sessionId()).isEqualTo(SESSION_ID);
            assertThat(responses.get(1).senderType()).isEqualTo("AI");
            assertThat(responses.get(1).completionTokens()).isEqualTo(50);
        }

        @Test
        @DisplayName("getMessages_withNonOwner_throwsForbiddenException")
        void getMessages_withNonOwner_throwsForbiddenException() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            // sessionId만 알면 남의 대화 내용을 읽을 수 있었던 IDOR 경로다.
            assertThatThrownBy(() -> chatMessageService.getMessages(SESSION_ID, "EMP999"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("해당 채팅방에 대한 접근 권한이 없습니다.");

            verify(chatMessageRepository, never()).findBySession_IdOrderByCreatedAtAsc(anyString());
        }

        @Test
        @DisplayName("getMessages_withUnknownSession_throwsIllegalArgumentException")
        void getMessages_withUnknownSession_throwsIllegalArgumentException() {
            when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatMessageService.getMessages(SESSION_ID, "EMP001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("세션을 찾을 수 없습니다: " + SESSION_ID);
        }
    }
}
