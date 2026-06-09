package com.devbridge.backend.domain.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DashboardGitCommitItemResponse {

    private String commitId;
    private String commitHash;
    private String commitMessage;
    private String authorName;
    private LocalDateTime pushedAt;
}