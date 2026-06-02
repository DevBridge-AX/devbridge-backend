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
}
