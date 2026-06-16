package com.devbridge.backend.domain.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {
    private String id;

    private String workspaceId;
    private String workspaceName;

    private String dataSourceId;
    private String sourceName;
    private String sourceType;
    private String sourceStatus;

    private String title;
    private String vectorId;

    private String summary;
    private String analysisStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}