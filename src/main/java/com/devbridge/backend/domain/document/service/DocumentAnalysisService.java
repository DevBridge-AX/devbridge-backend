package com.devbridge.backend.domain.document.service;

import com.devbridge.backend.domain.datasource.dto.DocumentResponse;
import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.repository.KnowledgeDocumentRepository;
import com.devbridge.backend.domain.document.dto.DocumentAnalysisRequest;
import com.devbridge.backend.domain.task.entity.Task;
import com.devbridge.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAnalysisService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final DocumentAnalysisClient documentAnalysisClient;

    @Transactional
    public DocumentResponse analyzeDocument(String documentId) {
        KnowledgeDocument document = findDocumentById(documentId);

        document.markAnalysisProcessing();

        try {
            documentAnalysisClient.analyze(buildAnalysisRequest(document));
        } catch (RuntimeException e) {
            document.markAnalysisFailed();
            log.warn("Document analysis request failed. documentId={}", documentId, e);
        }

        return toDocumentResponse(document);
    }

    private DocumentAnalysisRequest buildAnalysisRequest(KnowledgeDocument document) {
        Task task = document.getTask();
        String dataSourceId = document.getDataSource().getId();

        return DocumentAnalysisRequest.builder()
                .documentId(document.getId())
                .workspaceId(document.getDataSource().getWorkspace().getId())
                .sourceId(dataSourceId)
                .dataSourceId(dataSourceId)
                .taskId(task != null ? task.getId() : null)
                .title(document.getTitle())
                .filePath(document.getFilePath())
                .docType(document.getDocumentType())
                .build();
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
        Task task = document.getTask();
        String documentId = document.getId();

        return DocumentResponse.builder()
                .id(documentId)
                .workspaceId(document.getDataSource().getWorkspace().getId())
                .workspaceName(document.getDataSource().getWorkspace().getName())
                .dataSourceId(document.getDataSource().getId())
                .sourceName(document.getDataSource().getSourceName())
                .sourceType(document.getDataSource().getSourceType())
                .sourceStatus(document.getDataSource().getStatus())
                .taskId(task != null ? task.getId() : null)
                .taskTitle(task != null ? task.getTitle() : null)
                .title(document.getTitle())
                .documentType(document.getDocumentType())
                .description(document.getDescription())
                .vectorId(document.getVectorId())
                .summary(document.getSummary())
                .keywords(document.getKeywords())
                .riskLevel(document.getRiskLevel())
                .nextAction(document.getNextAction())
                .analysisStatus(document.getAnalysisStatus())
                .analysisModel(document.getAnalysisModel())
                .analysisMode(document.getAnalysisMode())
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