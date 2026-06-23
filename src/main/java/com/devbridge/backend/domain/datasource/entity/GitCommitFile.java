package com.devbridge.backend.domain.datasource.entity;

import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "GIT_COMMIT_FILES", indexes = {
        @Index(name = "idx_git_commit_files_commit_id", columnList = "commit_id"),
        @Index(name = "idx_git_commit_files_file_path", columnList = "file_path")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GitCommitFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commit_id", nullable = false)
    private GitCommit gitCommit;

    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    @Column(name = "change_type", length = 30)
    private String changeType;

    @Column(name = "additions")
    private Integer additions;

    @Column(name = "deletions")
    private Integer deletions;

    @Column(name = "patch", columnDefinition = "LONGTEXT")
    private String patch;

    @Column(name = "diff_summary", columnDefinition = "TEXT")
    private String diffSummary;
}