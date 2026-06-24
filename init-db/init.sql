CREATE DATABASE IF NOT EXISTS devbridge;

-- ============================================================
-- 1. 계정 생성
-- ============================================================

-- Spring 계정: DDL + DML 전체 권한 (테이블 생성/수정의 유일한 주체)
CREATE USER IF NOT EXISTS 'ssafy_be'@'%' IDENTIFIED BY '1234';
GRANT ALL PRIVILEGES ON devbridge.* TO 'ssafy_be'@'%' WITH GRANT OPTION;

-- FastAPI 계정: SELECT + DML (스키마 레벨)
-- 개발 단계에서는 스키마 레벨로 부여하고, 프로덕션 배포 시 테이블별 세분화
CREATE USER IF NOT EXISTS 'ssafy_ai'@'%' IDENTIFIED BY '1234';
GRANT SELECT, INSERT, UPDATE, DELETE ON devbridge.* TO 'ssafy_ai'@'%';

-- ============================================================
-- 2. AI 전용 테이블 (Spring 엔티티 미존재, Alembic 비활성화)
-- ============================================================

USE devbridge;

CREATE TABLE IF NOT EXISTS document_chunks (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id            VARCHAR(36)   NOT NULL,
    source_type             VARCHAR(30)   NOT NULL,
    source_id               VARCHAR(36)   NOT NULL,
    content                 TEXT          NOT NULL,
    chunk_metadata          JSON          NULL,
    embedding_model         VARCHAR(100)  NOT NULL,
    embedding_model_version VARCHAR(50)   NOT NULL,
    vector_id               VARCHAR(255)  NOT NULL,
    created_at              DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX ix_document_chunks_workspace_id (workspace_id),
    INDEX ix_document_chunks_vector_id (vector_id),
    INDEX ix_document_chunks_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usage_logs (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id     VARCHAR(36)   NOT NULL,
    date             DATE          NOT NULL,
    embedding_model  VARCHAR(100)  NOT NULL,
    embedding_tokens DECIMAL(12,1) NOT NULL DEFAULT 0.0,
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX ix_usage_logs_workspace_id (workspace_id),
    UNIQUE KEY uq_usage_logs_workspace_date_model (workspace_id, date, embedding_model)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

FLUSH PRIVILEGES;
