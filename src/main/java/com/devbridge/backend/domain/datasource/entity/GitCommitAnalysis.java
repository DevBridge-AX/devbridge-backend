package com.devbridge.backend.domain.datasource.entity;

import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "GIT_COMMIT_ANALYSIS", indexes = {
        @Index(name = "idx_git_commit_analysis_workspace_id", columnList = "workspace_id"),
        @Index(name = "idx_git_commit_analysis_index_status", columnList = "index_status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_git_commit_analysis_commit", columnNames = {"commit_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GitCommitAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commit_id", nullable = false)
    private GitCommit gitCommit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "impact_area", length = 255)
    private String impactArea;

    @Column(name = "risk_level", length = 50)
    private String riskLevel;

    @Column(name = "next_action", columnDefinition = "TEXT")
    private String nextAction;

    @Column(name = "vector_id", length = 255)
    private String vectorId;

    @Builder.Default
    @Column(name = "index_status", nullable = false, length = 50)
    private String indexStatus = "PENDING";

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;
}