package com.devbridge.backend.domain.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DashboardDocumentItemResponse {

    private String documentId;
    private String title;
    private String sourceName;
    private LocalDateTime createdAt;
}