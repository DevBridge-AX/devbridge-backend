package com.devbridge.backend.domain.chat.entity;

import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CHAT_MESSAGES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id; // 메시지 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session; // 세션 ID

    @Column(name = "sender_type", nullable = false, length = 50)
    private String senderType; // USER, AI, OWNER

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // 메시지 본문

    @Builder.Default
    @Column(name = "prompt_tokens")
    private Integer promptTokens = 0; // AI 사용량(요청)

    @Builder.Default
    @Column(name = "completion_tokens")
    private Integer completionTokens = 0; // AI 사용량(답변)

    @Column(name = "is_groundable")
    private Boolean isGroundable;

    @Column(name = "confidence_score", precision = 4, scale = 3)
    private java.math.BigDecimal confidenceScore;

    @Builder.Default
    @Column(name = "context_truncated", nullable = false)
    private Boolean contextTruncated = false;

    @Column(name = "estimated_cost", precision = 10, scale = 4)
    private java.math.BigDecimal estimatedCost;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Builder.Default
    @Column(name = "feedback", columnDefinition = "ENUM('positive', 'negative', 'none') DEFAULT 'none'")
    private FeedbackType feedback = FeedbackType.NONE;

    @Column(name = "rewrite_model", length = 100)
    private String rewriteModel;

    @Column(name = "rewrite_prompt_tokens")
    private Integer rewritePromptTokens;

    @Column(name = "rewrite_completion_tokens")
    private Integer rewriteCompletionTokens;
}
