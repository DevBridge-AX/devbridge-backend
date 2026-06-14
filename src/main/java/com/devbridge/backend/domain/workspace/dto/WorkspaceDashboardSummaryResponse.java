package com.devbridge.backend.domain.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WorkspaceDashboardSummaryResponse {

    private String workspaceId;
    private String workspaceName;

    private long totalTaskCount;
    private long assignedTaskCount;
    private long inProgressTaskCount;
    private long doneTaskCount;
    private long delayedTaskCount;

    private int progressRate;
    private long memberCount;
}