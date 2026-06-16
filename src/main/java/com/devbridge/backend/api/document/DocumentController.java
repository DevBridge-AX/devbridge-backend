package com.devbridge.backend.api.document;

import com.devbridge.backend.domain.datasource.dto.DocumentResponse;
import com.devbridge.backend.domain.datasource.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocumentController implements DocumentAPI {

    private final DocumentService documentService;

    @Override
    public ResponseEntity<List<DocumentResponse>> getDocumentsByWorkspace(String workspaceId) {
        return ResponseEntity.ok(documentService.getDocumentsByWorkspace(workspaceId));
    }

    @Override
    public ResponseEntity<DocumentResponse> getDocumentDetail(String id) {
        return ResponseEntity.ok(documentService.getDocumentDetail(id));
    }
}