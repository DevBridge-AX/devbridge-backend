package com.devbridge.backend.domain.document.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentAnalysisResponse {

    @JsonProperty("knowledge_document_id")
    private String knowledgeDocumentId;

    @JsonProperty("document_id")
    private String documentId;

    private String status;

    private String message;
}