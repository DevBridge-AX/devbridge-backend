ALTER TABLE GIT_COMMITS
    ADD COLUMN short_hash VARCHAR(20) NULL,
    ADD COLUMN branch_name VARCHAR(255) NULL;

CREATE TABLE GIT_COMMIT_FILES (
    id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    commit_id VARCHAR(36) NOT NULL,
    file_path VARCHAR(1024) NOT NULL,
    change_type VARCHAR(30) NULL,
    additions INT NULL,
    deletions INT NULL,
    patch LONGTEXT NULL,
    diff_summary TEXT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_git_commit_files_commit
        FOREIGN KEY (commit_id) REFERENCES GIT_COMMITS(id),

    INDEX idx_git_commit_files_commit_id (commit_id),
    INDEX idx_git_commit_files_file_path (file_path(255))
);

CREATE TABLE GIT_COMMIT_ANALYSIS (
    id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    commit_id VARCHAR(36) NOT NULL,
    workspace_id VARCHAR(36) NOT NULL,

    summary TEXT NULL,
    impact_area VARCHAR(255) NULL,
    risk_level VARCHAR(50) NULL,
    next_action TEXT NULL,

    vector_id VARCHAR(255) NULL,
    index_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    analyzed_at DATETIME(6) NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_git_commit_analysis_commit
        FOREIGN KEY (commit_id) REFERENCES GIT_COMMITS(id),

    CONSTRAINT fk_git_commit_analysis_workspace
        FOREIGN KEY (workspace_id) REFERENCES WORKSPACES(id),

    CONSTRAINT uk_git_commit_analysis_commit
        UNIQUE (commit_id),

    INDEX idx_git_commit_analysis_workspace_id (workspace_id),
    INDEX idx_git_commit_analysis_index_status (index_status)
);