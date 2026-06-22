package com.devbridge.backend.domain.datasource.entity;

import com.devbridge.backend.domain.task.entity.Task;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "KNOWLEDGE_DOCUMENTS", indexes = {
        @Index(name = "idx_knowledge_documents_workspace_id", columnList = "workspace_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE KNOWLEDGE_DOCUMENTS SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class KnowledgeDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private DataSource dataSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "analysis_status", nullable = false, length = 50)
    private String analysisStatus = "PENDING";

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "risk_level", length = 50)
    private String riskLevel;

    @Column(name = "next_action", columnDefinition = "TEXT")
    private String nextAction;

    @Column(name = "analysis_model", length = 100)
    private String analysisModel;

    @Column(name = "analysis_mode", length = 50)
    private String analysisMode;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(name = "vector_id", length = 255)
    private String vectorId;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "stored_file_name", length = 255)
    private String storedFileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    public void updateAnalysisResult(
            String summary,
            String keywords,
            String riskLevel,
            String nextAction,
            String analysisModel,
            String analysisMode
    ) {
        this.summary = summary;
        this.keywords = keywords;
        this.riskLevel = riskLevel;
        this.nextAction = nextAction;
        this.analysisModel = analysisModel;
        this.analysisMode = analysisMode;
        this.analysisStatus = "COMPLETED";
        this.analyzedAt = LocalDateTime.now();
    }

    public void markAnalysisProcessing() {
        this.analysisStatus = "PROCESSING";
    }

    public void markAnalysisFailed() {
        this.analysisStatus = "FAILED";
        this.analyzedAt = LocalDateTime.now();
    }

    public void updateDocumentInfo(String title, String documentType, String description) {
        if (title != null && !title.isBlank()) {
            this.title = title.trim();
        }

        if (documentType != null && !documentType.isBlank()) {
            this.documentType = documentType.trim().toUpperCase();
        }

        this.description = description != null && !description.isBlank()
                ? description.trim()
                : null;
    }
}