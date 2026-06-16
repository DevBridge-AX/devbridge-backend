package com.devbridge.backend.domain.datasource.service;

import com.devbridge.backend.domain.datasource.dto.DocumentResponse;
import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.repository.KnowledgeDocumentRepository;
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
        return DocumentResponse.builder()
                .id(document.getId())
                .workspaceId(document.getDataSource().getWorkspace().getId())
                .workspaceName(document.getDataSource().getWorkspace().getName())
                .dataSourceId(document.getDataSource().getId())
                .sourceName(document.getDataSource().getSourceName())
                .sourceType(document.getDataSource().getSourceType())
                .sourceStatus(document.getDataSource().getStatus())
                .title(document.getTitle())
                .vectorId(document.getVectorId())
                .summary(null)
                .analysisStatus(null)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}