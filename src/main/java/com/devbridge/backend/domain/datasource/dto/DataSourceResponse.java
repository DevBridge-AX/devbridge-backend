package com.devbridge.backend.domain.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSourceResponse {
    private String id;
    private String workspaceId;
    private String sourceType;
    private String sourceName;
    private String status;
}
