package com.devbridge.backend.domain.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    private String id;
    private String workspaceId;
    private String requesterId;
    private String assigneeId;
    private String title;
    private String description;
    private String status;
    private LocalDateTime dueDate;
}
