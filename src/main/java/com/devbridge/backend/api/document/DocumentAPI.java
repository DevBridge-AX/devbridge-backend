package com.devbridge.backend.api.document;

import com.devbridge.backend.domain.datasource.dto.DocumentResponse;
import com.devbridge.backend.domain.document.dto.UpdateDocumentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @Operation(summary = "Document 수정", description = "Document ID 기준으로 문서 제목과 문서 타입을 수정합니다.")
    @PutMapping("/{id}")
    ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable("id") String id,
            @RequestBody UpdateDocumentRequest request
    );

    @Operation(summary = "Document AI 분석 실행", description = "Document ID 기준으로 AI Engine 분석을 실행하고 결과를 저장합니다.")
    @PostMapping("/{id}/analyze")
    ResponseEntity<DocumentResponse> analyzeDocument(@PathVariable("id") String id);

    @Operation(summary = "Document 파일 업로드", description = "문서 파일을 업로드하고 문서 메타데이터를 저장합니다.")
    @PostMapping("/upload")
    ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("workspaceId") String workspaceId,
            @RequestParam("dataSourceId") String dataSourceId,
            @RequestParam(value = "uploadedById", required = false) String uploadedById,
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart("file") MultipartFile file
    );

    @Operation(summary = "Document 파일 다운로드", description = "Document ID 기준으로 저장된 원본 파일을 다운로드합니다.")
    @GetMapping("/{id}/download")
    ResponseEntity<Resource> downloadDocument(@PathVariable("id") String id);

    @Operation(summary = "Document 파일 미리보기", description = "Document ID 기준으로 저장된 파일을 브라우저에서 미리보기합니다.")
    @GetMapping("/{id}/preview")
    ResponseEntity<Resource> previewDocument(@PathVariable("id") String id);

    @Operation(summary = "Document 삭제", description = "Document ID 기준으로 문서를 삭제합니다.")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteDocument(@PathVariable("id") String id);
}