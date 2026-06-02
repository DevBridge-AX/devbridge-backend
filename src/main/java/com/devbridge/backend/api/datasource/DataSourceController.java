package com.devbridge.backend.api.datasource;

import com.devbridge.backend.domain.datasource.dto.ConnectDataSourceRequest;
import com.devbridge.backend.domain.datasource.dto.DataSourceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataSourceController implements DataSourceAPI {

    @Override
    public ResponseEntity<DataSourceResponse> connectDataSource(ConnectDataSourceRequest request) {
        DataSourceResponse response = DataSourceResponse.builder()
                .id("dummy-source-id")
                .workspaceId(request.getWorkspaceId())
                .sourceType(request.getSourceType())
                .sourceName(request.getSourceName())
                .status("CONNECTED")
                .build();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DataSourceResponse> getDataSource(String id) {
        DataSourceResponse response = DataSourceResponse.builder()
                .id(id)
                .workspaceId("dummy-workspace-id")
                .sourceType("GIT")
                .sourceName("devbridge-repo")
                .status("CONNECTED")
                .build();
        return ResponseEntity.ok(response);
    }
}
