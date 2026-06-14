package com.devbridge.backend.domain.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DashboardTaskItemResponse {

    private String taskId;
    private String title;
    private String status;
    private String assigneeName;
    private LocalDateTime dueDate;
}