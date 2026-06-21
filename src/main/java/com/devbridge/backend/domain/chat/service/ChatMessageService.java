package com.devbridge.backend.domain.chat.service;

import com.devbridge.backend.domain.chat.dto.fastapi.FastApiDoneEvent;
import com.devbridge.backend.domain.chat.entity.ChatMessage;
import com.devbridge.backend.domain.chat.entity.ChatSession;
import com.devbridge.backend.domain.chat.entity.CitationSourceType;
import com.devbridge.backend.domain.chat.entity.MessageCitation;
import com.devbridge.backend.domain.chat.repository.ChatMessageRepository;
import com.devbridge.backend.domain.chat.repository.ChatSessionRepository;
import com.devbridge.backend.domain.chat.repository.MessageCitationRepository;
import com.devbridge.backend.global.config.fastapi.FastApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.devbridge.backend.domain.chat.dto.ChatMessageResponse;
import com.devbridge.backend.domain.chat.dto.ConversationContext;
import com.devbridge.backend.domain.chat.dto.fastapi.FastApiChatRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final MessageCitationRepository messageCitationRepository;
    private final FastApiProperties fastApiProperties;

    @Transactional
    public ChatMessage saveUserMessage(String sessionId, String content) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));

        ChatMessage userMessage = ChatMessage.builder()
                .session(session)
                .senderType("USER")
                .content(content)
                .build();

        return chatMessageRepository.save(userMessage);
    }

    @Transactional
    public ChatMessage saveAiMessage(String sessionId,
                                     String fullContent, FastApiDoneEvent doneEvent) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));

        FastApiDoneEvent.TokenUsage tokenUsage = doneEvent.getTokenUsage();
        FastApiDoneEvent.TokenUsageDetail main = tokenUsage.getMain();
        FastApiDoneEvent.TokenUsageDetail rewrite = tokenUsage.getRewrite();

        BigDecimal estimatedCost = calculateEstimatedCost(main, rewrite);

        ChatMessage aiMessage = ChatMessage.builder()
                .session(session)
                .senderType("AI")
                .content(fullContent)
                .isGroundable(doneEvent.getIsGroundable())
                .confidenceScore(BigDecimal.valueOf(doneEvent.getConfidence()))
                .contextTruncated(tokenUsage.getContextTruncated())
                .promptVersion(doneEvent.getPromptVersion())
                .promptTokens(main.getPromptTokens())
                .completionTokens(main.getCompletionTokens())
                .rewriteModel(rewrite != null ? rewrite.getModel() : null)
                .rewritePromptTokens(rewrite != null ? rewrite.getPromptTokens() : null)
                .rewriteCompletionTokens(rewrite != null ? rewrite.getCompletionTokens() : null)
                .estimatedCost(estimatedCost)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(aiMessage);

        saveCitations(savedMessage, doneEvent.getCitations());

        return savedMessage;
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getConversationHistory(String sessionId) {
        return chatMessageRepository.findBySession_IdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySession_IdOrderByCreatedAtAsc(sessionId);
        return messages.stream()
                .map(msg -> ChatMessageResponse.builder()
                        .id(msg.getId())
                        .sessionId(msg.getSession().getId())
                        .senderType(msg.getSenderType())
                        .content(msg.getContent())
                        .promptTokens(msg.getPromptTokens())
                        .completionTokens(msg.getCompletionTokens())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversationContext getConversationContext(String sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));

        String workspaceId = session.getWorkspace().getId();
        String userId = session.getUser().getId();

        List<ChatMessage> messages = chatMessageRepository.findBySession_IdOrderByCreatedAtAsc(sessionId);
        List<FastApiChatRequest.ConversationMessage> history = new ArrayList<>();
        for (ChatMessage msg : messages) {
            String role = "USER".equals(msg.getSenderType()) ? "user" : "assistant";
            history.add(FastApiChatRequest.ConversationMessage.builder()
                    .role(role)
                    .content(msg.getContent())
                    .build());
        }

        return ConversationContext.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .conversationHistory(history)
                .build();
    }

    private void saveCitations(ChatMessage message, List<FastApiDoneEvent.CitationData> citations) {
        if (citations == null || citations.isEmpty()) {
            return;
        }

        for (FastApiDoneEvent.CitationData citation : citations) {
            CitationSourceType sourceType = CitationSourceType.valueOf(
                    citation.getSourceType().toUpperCase());

            MessageCitation entity = MessageCitation.builder()
                    .message(message)
                    .sourceType(sourceType)
                    .sourceId(citation.getSourceId())
                    .vectorChunkId(citation.getChunkId())
                    .title(citation.getTitle())
                    .similarityScore(BigDecimal.valueOf(citation.getSimilarityScore()))
                    .build();

            messageCitationRepository.save(entity);
        }
    }

    private BigDecimal calculateEstimatedCost(
            FastApiDoneEvent.TokenUsageDetail main,
            FastApiDoneEvent.TokenUsageDetail rewrite) {

        BigDecimal cost = calculateModelCost(main);

        if (rewrite != null) {
            cost = cost.add(calculateModelCost(rewrite));
        }

        return cost;
    }

    private BigDecimal calculateModelCost(FastApiDoneEvent.TokenUsageDetail usage) {
        var creditRates = fastApiProperties.getCreditRates();
        if (creditRates == null) {
            return BigDecimal.ZERO;
        }

        FastApiProperties.CreditRate rate = creditRates.get(usage.getModel());
        if (rate == null) {
            log.warn("단가표에 모델이 없습니다: {}", usage.getModel());
            return BigDecimal.ZERO;
        }

        BigDecimal inputCost = rate.getInput()
                .multiply(BigDecimal.valueOf(usage.getPromptTokens()))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);

        BigDecimal outputCost = rate.getOutput()
                .multiply(BigDecimal.valueOf(usage.getCompletionTokens()))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);

        return inputCost.add(outputCost);
    }
}
