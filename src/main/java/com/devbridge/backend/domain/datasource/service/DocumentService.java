package com.devbridge.backend.domain.datasource.service;

import com.devbridge.backend.domain.datasource.dto.DocumentResponse;
import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.repository.KnowledgeDocumentRepository;
import com.devbridge.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByWorkspace(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("Workspace ID is required.");
        }

        return knowledgeDocumentRepository.findByDataSource_Workspace_IdOrderByCreatedAtDesc(workspaceId)
                .stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentDetail(String documentId) {
        KnowledgeDocument document = findDocumentById(documentId);
        return toDocumentResponse(document);
    }

    private KnowledgeDocument findDocumentById(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("Document ID is required.");
        }

        return knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    }

    private DocumentResponse toDocumentResponse(KnowledgeDocument document) {
        User uploadedBy = document.getUploadedBy();

        String documentId = document.getId();

        return DocumentResponse.builder()
                .id(documentId)
                .workspaceId(document.getDataSource().getWorkspace().getId())
                .workspaceName(document.getDataSource().getWorkspace().getName())
                .dataSourceId(document.getDataSource().getId())
                .sourceName(document.getDataSource().getSourceName())
                .sourceType(document.getDataSource().getSourceType())
                .sourceStatus(document.getDataSource().getStatus())
                .title(document.getTitle())
                .documentType(document.getDocumentType())
                .description(document.getDescription())
                .vectorId(document.getVectorId())
                .summary(document.getSummary())
                .keywords(document.getKeywords())
                .analysisStatus(document.getAnalysisStatus())
                .analyzedAt(document.getAnalyzedAt())
                .originalFileName(document.getOriginalFileName())
                .storedFileName(document.getStoredFileName())
                .fileUrl("/api/documents/" + documentId + "/preview")
                .previewUrl("/api/documents/" + documentId + "/preview")
                .downloadUrl("/api/documents/" + documentId + "/download")
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .uploadedById(uploadedBy != null ? uploadedBy.getId() : null)
                .uploadedByName(uploadedBy != null ? uploadedBy.getName() : null)
                .uploadedByEmail(uploadedBy != null ? uploadedBy.getEmail() : null)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}