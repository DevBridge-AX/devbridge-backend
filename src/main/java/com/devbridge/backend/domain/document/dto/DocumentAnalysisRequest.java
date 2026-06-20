package com.devbridge.backend.domain.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentAnalysisRequest {

    @JsonProperty("document_id")
    private String documentId;

    @JsonProperty("workspace_id")
    private String workspaceId;

    @JsonProperty("task_id")
    private String taskId;

    private String title;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("doc_type")
    private String docType;
}