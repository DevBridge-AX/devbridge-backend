package com.devbridge.backend.domain.git.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitIngestionRequest {

    private String workspaceId;
    private String dataSourceId;
    private String repositoryPath;

    @Builder.Default
    private int limit = 20;
}