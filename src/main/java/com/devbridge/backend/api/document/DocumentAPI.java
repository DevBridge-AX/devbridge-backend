package com.devbridge.backend.api.document;

import com.devbridge.backend.domain.datasource.dto.DocumentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Document", description = "Document API")
@RequestMapping("/api/documents")
public interface DocumentAPI {

    @Operation(summary = "Document 목록 조회", description = "Workspace ID 기준으로 문서 목록을 조회합니다.")
    @GetMapping
    ResponseEntity<List<DocumentResponse>> getDocumentsByWorkspace(
            @RequestParam("workspaceId") String workspaceId
    );

    @Operation(summary = "Document 상세 조회", description = "Document ID 기준으로 문서 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    ResponseEntity<DocumentResponse> getDocumentDetail(@PathVariable("id") String id);
}