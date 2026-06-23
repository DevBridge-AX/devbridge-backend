package com.devbridge.backend.domain.document.service;

import com.devbridge.backend.domain.datasource.dto.DocumentResponse;
import com.devbridge.backend.domain.datasource.entity.DataSource;
import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.repository.DataSourceRepository;
import com.devbridge.backend.domain.datasource.repository.KnowledgeDocumentRepository;
import com.devbridge.backend.domain.document.dto.UpdateDocumentRequest;
import com.devbridge.backend.domain.task.entity.Task;
import com.devbridge.backend.domain.task.repository.TaskRepository;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.global.config.fastapi.FastApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentFileService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final DataSourceRepository dataSourceRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final FastApiClient fastApiClient;

    @Value("${devbridge.file.upload-dir:uploads/documents}")
    private String uploadDir;

    @Transactional
    public DocumentResponse uploadDocument(
            String workspaceId,
            String dataSourceId,
            String uploadedById,
            String taskId,
            String documentType,
            String description,
            MultipartFile file
    ) {
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("Workspace ID is required.");
        }

        if (dataSourceId == null || dataSourceId.isBlank()) {
            throw new IllegalArgumentException("DataSource ID is required.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is required.");
        }

        DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + dataSourceId));

        if (!workspaceId.equals(dataSource.getWorkspace().getId())) {
            throw new IllegalArgumentException("DataSource does not belong to workspace: " + workspaceId);
        }

        User uploadedBy = null;
        if (uploadedById != null && !uploadedById.isBlank()) {
            uploadedBy = userRepository.findById(uploadedById)
                    .orElseThrow(() -> new IllegalArgumentException("Uploader not found: " + uploadedById));
        }

        Task task = null;
        if (taskId != null && !taskId.isBlank()) {
            task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

            if (!workspaceId.equals(task.getWorkspace().getId())) {
                throw new IllegalArgumentException("Task does not belong to workspace: " + workspaceId);
            }
        }

        String originalFileName = file.getOriginalFilename();
        String safeOriginalFileName = originalFileName != null ? originalFileName : "document";
        String storedFileName = UUID.randomUUID() + "_" + safeOriginalFileName;
        String normalizedDocumentType = normalizeDocumentType(documentType);

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            Path targetPath = uploadPath.resolve(storedFileName).normalize();
            file.transferTo(targetPath.toFile());

            KnowledgeDocument document = KnowledgeDocument.builder()
                    .dataSource(dataSource)
                    .uploadedBy(uploadedBy)
                    .task(task)
                    .title(safeOriginalFileName)
                    .documentType(normalizedDocumentType)
                    .analysisStatus("PENDING")
                    .originalFileName(safeOriginalFileName)
                    .storedFileName(storedFileName)
                    .filePath(targetPath.toString())
                    .contentType(file.getContentType())
                    .description(normalizeDescription(description))
                    .fileSize(file.getSize())
                    .build();

            KnowledgeDocument savedDocument = knowledgeDocumentRepository.save(document);

            triggerRagIngestion(savedDocument);

            return toDocumentResponse(savedDocument);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store upload file.", e);
        }
    }

    @Transactional(readOnly = true)
    public Resource loadDocumentFile(String documentId) {
        KnowledgeDocument document = findDocumentById(documentId);

        if (document.getFilePath() == null || document.getFilePath().isBlank()) {
            throw new IllegalArgumentException("Document file path is empty: " + documentId);
        }

        Path filePath = Paths.get(document.getFilePath()).toAbsolutePath().normalize();
        Resource resource = new PathResource(filePath);

        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("Document file is not readable: " + documentId);
        }

        return resource;
    }

    @Transactional(readOnly = true)
    public KnowledgeDocument getDocumentEntity(String documentId) {
        return findDocumentById(documentId);
    }

    @Transactional
    public DocumentResponse updateDocument(
            String documentId,
            UpdateDocumentRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Update document request is required.");
        }

        KnowledgeDocument document = findDocumentById(documentId);

        document.updateDocumentInfo(
                request.getTitle(),
                request.getDocumentType(),
                request.getDescription()
        );

        return toDocumentResponse(document);
    }

    @Transactional
    public void deleteDocument(String documentId) {
        KnowledgeDocument document = findDocumentById(documentId);
        knowledgeDocumentRepository.delete(document);
    }

    private KnowledgeDocument findDocumentById(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("Document ID is required.");
        }

        return knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    }

    private void triggerRagIngestion(KnowledgeDocument document) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workspace_id", document.getDataSource().getWorkspace().getId());
        request.put("source_id", document.getDataSource().getId());
        request.put("backend_document_id", document.getId());
        request.put("title", document.getTitle());
        request.put("doc_type", document.getDocumentType() != null ? document.getDocumentType() : "general");
        request.put("file_path", document.getFilePath());

        fastApiClient.ingestDocument(request)
                .subscribe(
                        unused -> {},
                        e -> log.error("문서 RAG 인덱싱 요청 실패: documentId={}, error={}",
                                document.getId(), e.getMessage())
                );
    }

    private String normalizeDocumentType(String documentType) {
        if (documentType == null || documentType.isBlank()) {
            return "ETC";
        }

        return documentType.trim().toUpperCase();
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
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