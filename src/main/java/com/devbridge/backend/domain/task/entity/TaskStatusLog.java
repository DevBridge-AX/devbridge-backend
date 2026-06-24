package com.devbridge.backend.domain.task.entity;

import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TASK_STATUS_LOGS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TaskStatusLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "previous_status", length = 50)
    private String previousStatus;

    @Column(name = "next_status", nullable = false, length = 50)
    private String nextStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreateStatusLog() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }

        if (status == null && nextStatus != null) {
            status = nextStatus;
        }
    }
}