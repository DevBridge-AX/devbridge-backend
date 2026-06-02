package com.devbridge.backend.api.datasource;

import com.devbridge.backend.domain.datasource.dto.ConnectDataSourceRequest;
import com.devbridge.backend.domain.datasource.dto.DataSourceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Data Source", description = "데이터 소스 연동 API 명세")
@RequestMapping("/api/datasources")
public interface DataSourceAPI {

    @Operation(summary = "데이터 소스 연동", description = "외부 Git, DB, 문서 등의 데이터 소스를 연동합니다.")
    @PostMapping
    ResponseEntity<DataSourceResponse> connectDataSource(@RequestBody ConnectDataSourceRequest request);

    @Operation(summary = "데이터 소스 상세 조회", description = "ID에 해당하는 데이터 소스 정보를 조회합니다.")
    @GetMapping("/{id}")
    ResponseEntity<DataSourceResponse> getDataSource(@PathVariable("id") String id);
}
