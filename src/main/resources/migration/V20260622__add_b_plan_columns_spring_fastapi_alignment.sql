-- ============================================================
-- Migration: B항목 — Spring-FastAPI 엔티티 통일 잔여 컬럼 반영
-- Date: 2026-06-22
-- ============================================================

-- 1. DATA_SOURCES: config (nullable JSON, 민감 정보 저장 금지)
ALTER TABLE DATA_SOURCES
    ADD COLUMN config JSON NULL;

-- 2-1. DATABASE_SCHEMAS: workspace_id FK (NOT NULL)
ALTER TABLE DATABASE_SCHEMAS
    ADD COLUMN workspace_id VARCHAR(36) NOT NULL,
    ADD CONSTRAINT fk_database_schemas_workspace
        FOREIGN KEY (workspace_id) REFERENCES WORKSPACES(id),
    ADD INDEX idx_database_schemas_workspace_id (workspace_id);

-- 2-2. GIT_COMMITS: workspace_id FK (NOT NULL) + unique(workspace_id, commit_hash)
ALTER TABLE GIT_COMMITS
    ADD COLUMN workspace_id VARCHAR(36) NOT NULL,
    ADD CONSTRAINT fk_git_commits_workspace
        FOREIGN KEY (workspace_id) REFERENCES WORKSPACES(id),
    ADD INDEX idx_git_commits_workspace_id (workspace_id),
    ADD CONSTRAINT uk_git_commits_workspace_commit_hash
        UNIQUE (workspace_id, commit_hash);

-- 2-3. KNOWLEDGE_DOCUMENTS: workspace_id FK (NOT NULL)
ALTER TABLE KNOWLEDGE_DOCUMENTS
    ADD COLUMN workspace_id VARCHAR(36) NOT NULL,
    ADD CONSTRAINT fk_knowledge_documents_workspace
        FOREIGN KEY (workspace_id) REFERENCES WORKSPACES(id),
    ADD INDEX idx_knowledge_documents_workspace_id (workspace_id);

-- 3. DATABASE_SCHEMAS: schema_name, table_name, description (전부 nullable)
ALTER TABLE DATABASE_SCHEMAS
    ADD COLUMN schema_name VARCHAR(100) NULL,
    ADD COLUMN table_name VARCHAR(100) NULL,
    ADD COLUMN description TEXT NULL;

-- 4. GIT_COMMITS: author_name, author_email (nullable)
ALTER TABLE GIT_COMMITS
    ADD COLUMN author_name VARCHAR(100) NULL,
    ADD COLUMN author_email VARCHAR(255) NULL;
