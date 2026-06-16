package com.devbridge.backend.api.document;

import com.devbridge.backend.domain.datasource.dto.DocumentResponse;
import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.service.DocumentService;
import com.devbridge.backend.domain.document.dto.UpdateDocumentRequest;
import com.devbridge.backend.domain.document.service.DocumentFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocumentController implements DocumentAPI {

    private final DocumentService documentService;
    private final DocumentFileService documentFileService;

    @Override
    public ResponseEntity<List<DocumentResponse>> getDocumentsByWorkspace(String workspaceId) {
        return ResponseEntity.ok(documentService.getDocumentsByWorkspace(workspaceId));
    }

    @Override
    public ResponseEntity<DocumentResponse> getDocumentDetail(String id) {
        return ResponseEntity.ok(documentService.getDocumentDetail(id));
    }

    @Override
public ResponseEntity<DocumentResponse> updateDocument(
        String id,
        UpdateDocumentRequest request
) {
    return ResponseEntity.ok(documentFileService.updateDocument(id, request));
}

    @Override
    public ResponseEntity<DocumentResponse> uploadDocument(
            String workspaceId,
            String dataSourceId,
            String uploadedById,
            String documentType,
            String description,
            MultipartFile file
    ) {
        return ResponseEntity.ok(
                documentFileService.uploadDocument(
                        workspaceId,
                        dataSourceId,
                        uploadedById,
                        documentType,
                        description,
                        file
                )
        );
    }

    @Override
    public ResponseEntity<Resource> downloadDocument(String id) {
        KnowledgeDocument document = documentFileService.getDocumentEntity(id);
        Resource resource = documentFileService.loadDocumentFile(id);

        String fileName = document.getOriginalFileName() != null
                ? document.getOriginalFileName()
                : "document";

        MediaType mediaType = resolveMediaType(document.getContentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName, StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(resource);
    }

    @Override
    public ResponseEntity<Resource> previewDocument(String id) {
        KnowledgeDocument document = documentFileService.getDocumentEntity(id);
        Resource resource = documentFileService.loadDocumentFile(id);

        MediaType mediaType = resolveMediaType(document.getContentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(
                                        document.getOriginalFileName() != null
                                                ? document.getOriginalFileName()
                                                : "document",
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                )
                .body(resource);
    }

    @Override
    public ResponseEntity<Void> deleteDocument(String id) {
        documentFileService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}