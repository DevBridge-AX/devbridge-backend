package com.devbridge.backend.domain.datasource.repository;

import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {

    List<KnowledgeDocument> findTop5ByDataSource_Workspace_IdOrderByCreatedAtDesc(String workspaceId);
}