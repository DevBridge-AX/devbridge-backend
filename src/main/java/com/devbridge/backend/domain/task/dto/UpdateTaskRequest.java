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
public class UpdateTaskRequest {
    private String assigneeId;
    private String title;
    private String description;
    private LocalDateTime dueDate;
}