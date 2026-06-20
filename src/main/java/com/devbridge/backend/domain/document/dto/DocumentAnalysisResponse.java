package com.devbridge.backend.domain.document.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentAnalysisResponse {

    @JsonProperty("document_id")
    private String documentId;

    @JsonProperty("workspace_id")
    private String workspaceId;

    @JsonProperty("task_id")
    private String taskId;

    private String summary;

    private String keywords;

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("next_action")
    private String nextAction;

    private String model;

    private String mode;
}