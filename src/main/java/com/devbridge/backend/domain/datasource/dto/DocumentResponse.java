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

    private String taskId;
    private String taskTitle;

    private String title;
    private String documentType;
    private String description;

    private String vectorId;

    private String summary;
    private String keywords;
    private String riskLevel;
    private String nextAction;
    private String analysisStatus;
    private String analysisModel;
    private String analysisMode;
    private LocalDateTime analyzedAt;

    private String originalFileName;
    private String storedFileName;
    private String fileUrl;
    private String previewUrl;
    private String downloadUrl;
    private String contentType;
    private Long fileSize;

    private String uploadedById;
    private String uploadedByName;
    private String uploadedByEmail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}