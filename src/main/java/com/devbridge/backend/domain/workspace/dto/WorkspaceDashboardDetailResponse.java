package com.devbridge.backend.domain.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class WorkspaceDashboardDetailResponse {

    private List<DashboardTaskItemResponse> recentTasks;
    private List<DashboardTaskItemResponse> delayedTasks;
    private List<DashboardGitCommitItemResponse> recentGitCommits;
    private List<DashboardDocumentItemResponse> recentDocuments;
}