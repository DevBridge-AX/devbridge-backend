package com.devbridge.backend.domain.task.entity;

import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "TASKS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE TASKS SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Task extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "ASSIGNED";

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    public void updateTask(User assignee, String title, String description, LocalDateTime dueDate) {
        this.assignee = assignee;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
    }

    public void updateStatus(String status) {
        this.status = status;
    }
}