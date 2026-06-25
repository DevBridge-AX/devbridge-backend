package com.devbridge.backend.domain.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AiSummaryResponse {
    private String summary;
    private String model;
    private String mode;
}