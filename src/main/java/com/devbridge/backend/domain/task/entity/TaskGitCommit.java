package com.devbridge.backend.domain.task.entity;

import com.devbridge.backend.domain.datasource.entity.GitCommit;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "TASK_GIT_COMMITS",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_task_git_commit",
                        columnNames = {"task_id", "commit_id"}
                )
        },
        indexes = {
                @Index(name = "idx_task_git_commits_task_id", columnList = "task_id"),
                @Index(name = "idx_task_git_commits_commit_id", columnList = "commit_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TaskGitCommit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commit_id", nullable = false)
    private GitCommit gitCommit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_by")
    private User linkedBy;

    @Column(name = "link_type", nullable = false, length = 50)
    private String linkType;
}