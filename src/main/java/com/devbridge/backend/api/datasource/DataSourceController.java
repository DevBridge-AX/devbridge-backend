package com.devbridge.backend.api.datasource;

import com.devbridge.backend.domain.datasource.dto.ConnectDataSourceRequest;
import com.devbridge.backend.domain.datasource.dto.DataSourceResponse;
import com.devbridge.backend.domain.datasource.service.DataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DataSourceController implements DataSourceAPI {

    private final DataSourceService dataSourceService;

    @Override
    public ResponseEntity<DataSourceResponse> connectDataSource(ConnectDataSourceRequest request) {
        return ResponseEntity.ok(dataSourceService.connectDataSource(request));
    }

    @Override
    public ResponseEntity<DataSourceResponse> getDataSource(String id) {
        return ResponseEntity.ok(dataSourceService.getDataSource(id));
    }
}