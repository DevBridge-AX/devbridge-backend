-- ============================================================
-- Migration: OWNER_CONFIRMATIONS 확장 (문서 화면 직접 요청 지원)
-- Date: 2026-06-22
-- ============================================================

-- 1. question_message_id NOT NULL 제약 제거 (문서 화면 요청 시 ChatMessage 없음)
ALTER TABLE OWNER_CONFIRMATIONS
    MODIFY COLUMN question_message_id VARCHAR(36) NULL;

-- 2. related_document_id FK 컬럼 추가 (nullable)
ALTER TABLE OWNER_CONFIRMATIONS
    ADD COLUMN related_document_id VARCHAR(36) NULL,
    ADD CONSTRAINT fk_owner_confirmations_document
        FOREIGN KEY (related_document_id) REFERENCES KNOWLEDGE_DOCUMENTS(id);

-- 3. question_content TEXT 컬럼 추가 (nullable, 자유 텍스트 질문용)
ALTER TABLE OWNER_CONFIRMATIONS
    ADD COLUMN question_content TEXT NULL;
