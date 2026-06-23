package com.devbridge.backend.domain.chat.entity;

import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.task.entity.Task;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "OWNER_CONFIRMATIONS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OwnerConfirmation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id; // 확인 요청 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace; // 워크스페이스 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task; // 업무 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_message_id")
    private ChatMessage questionMessage; // 파생된 질문 (채팅 경로)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_document_id")
    private KnowledgeDocument relatedDocument; // 관련 문서 (문서 화면 경로)

    @Column(name = "question_content", columnDefinition = "TEXT")
    private String questionContent; // 자유 텍스트 질문 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_owner_id", nullable = false)
    private User assignedOwner; // 답변자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private User requester; // 질문 요청자

    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, ANSWERED

    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String answerContent; // 답변 내용

    @Builder.Default
    @Column(name = "is_faq")
    private Boolean isFaq = false; // FAQ 등록 여부

    @Column(name = "target_role", length = 100)
    private String targetRole; // FAQ 타겟 직무

    @Column(name = "answered_at")
    private LocalDateTime answeredAt; // 답변일

    public void submitAnswer(String answerContent) {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("이미 답변이 완료된 요청입니다.");
        }
        this.answerContent = answerContent;
        this.status = "ANSWERED";
        this.answeredAt = LocalDateTime.now();
    }
}
