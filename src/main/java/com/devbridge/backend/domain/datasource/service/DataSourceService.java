package com.devbridge.backend.domain.datasource.service;

import com.devbridge.backend.domain.datasource.dto.ConnectDataSourceRequest;
import com.devbridge.backend.domain.datasource.dto.DataSourceResponse;
import com.devbridge.backend.domain.datasource.entity.DataSource;
import com.devbridge.backend.domain.datasource.repository.DataSourceRepository;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import com.devbridge.backend.global.config.fastapi.FastApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    private final WorkspaceRepository workspaceRepository;
    private final FastApiClient fastApiClient;

    @Transactional
    public DataSourceResponse connectDataSource(ConnectDataSourceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("DataSource request is required.");
        }

        if (request.getWorkspaceId() == null || request.getWorkspaceId().isBlank()) {
            throw new IllegalArgumentException("Workspace ID is required.");
        }

        if (request.getSourceType() == null || request.getSourceType().isBlank()) {
            throw new IllegalArgumentException("Source type is required.");
        }

        if (request.getSourceName() == null || request.getSourceName().isBlank()) {
            throw new IllegalArgumentException("Source name is required.");
        }

        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + request.getWorkspaceId()));

        DataSource dataSource = DataSource.builder()
                .workspace(workspace)
                .sourceType(request.getSourceType().trim().toUpperCase())
                .sourceName(request.getSourceName().trim())
                .status("CONNECTED")
                .build();

        DataSource savedDataSource = dataSourceRepository.save(dataSource);

        triggerIngestion(savedDataSource);

        return toDataSourceResponse(savedDataSource);
    }

    @Transactional(readOnly = true)
    public DataSourceResponse getDataSource(String dataSourceId) {
        if (dataSourceId == null || dataSourceId.isBlank()) {
            throw new IllegalArgumentException("DataSource ID is required.");
        }

        DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + dataSourceId));

        return toDataSourceResponse(dataSource);
    }

    private void triggerIngestion(DataSource dataSource) {
        String sourceType = dataSource.getSourceType();
        String workspaceId = dataSource.getWorkspace().getId();
        String dataSourceId = dataSource.getId();

        if ("GIT".equals(sourceType)) {
            Map<String, Object> request = Map.of(
                    "workspace_id", workspaceId,
                    "data_source_id", dataSourceId,
                    "commits", java.util.List.of()
            );
            fastApiClient.ingestGit(request)
                    .subscribe(
                            unused -> {},
                            e -> log.error("Git 인덱싱 요청 실패: {}", e.getMessage())
                    );
        }
    }

    private DataSourceResponse toDataSourceResponse(DataSource dataSource) {
        return DataSourceResponse.builder()
                .id(dataSource.getId())
                .workspaceId(dataSource.getWorkspace().getId())
                .sourceType(dataSource.getSourceType())
                .sourceName(dataSource.getSourceName())
                .status(dataSource.getStatus())
                .build();
    }
}