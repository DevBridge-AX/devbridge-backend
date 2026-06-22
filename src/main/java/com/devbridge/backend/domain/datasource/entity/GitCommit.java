package com.devbridge.backend.domain.datasource.entity;

import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "GIT_COMMITS", indexes = {
        @Index(name = "idx_git_commits_workspace_id", columnList = "workspace_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_git_commits_workspace_commit_hash", columnNames = {"workspace_id", "commit_hash"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GitCommit extends BaseEntity {

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
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "author_name", length = 100)
    private String authorName;

    @Column(name = "author_email", length = 255)
    private String authorEmail;

    @Column(name = "commit_hash", nullable = false, length = 100)
    private String commitHash;

    @Column(name = "short_hash", length = 20)
    private String shortHash;

    @Column(name = "commit_message", nullable = false, columnDefinition = "TEXT")
    private String commitMessage;

    @Column(name = "branch_name", length = 255)
    private String branchName;

    @Column(name = "pushed_at", nullable = false)
    private LocalDateTime pushedAt;
}