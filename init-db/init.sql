CREATE DATABASE IF NOT EXISTS devbridge;

-- Spring 계정: DDL + DML 전체 권한 (테이블 생성/수정의 유일한 주체)
CREATE USER IF NOT EXISTS 'ssafy_be'@'%' IDENTIFIED BY '1234';
GRANT ALL PRIVILEGES ON devbridge.* TO 'ssafy_be'@'%';

-- FastAPI 계정: Spring 테이블은 SELECT만, AI 도메인 테이블은 DML만
CREATE USER IF NOT EXISTS 'ssafy_ai'@'%' IDENTIFIED BY '1234';

-- 1. 전체 READ ONLY (Spring 비즈니스 테이블 포함)
GRANT SELECT ON devbridge.* TO 'ssafy_ai'@'%';

-- 2. 공유 테이블: DML만 (DDL 제외 — ALTER, DROP 불가)
GRANT INSERT, UPDATE, DELETE ON devbridge.KNOWLEDGE_DOCUMENTS TO 'ssafy_ai'@'%';
GRANT INSERT, UPDATE, DELETE ON devbridge.DATA_SOURCES TO 'ssafy_ai'@'%';
GRANT INSERT, UPDATE, DELETE ON devbridge.DATABASE_SCHEMAS TO 'ssafy_ai'@'%';
GRANT INSERT, UPDATE, DELETE ON devbridge.GIT_COMMITS TO 'ssafy_ai'@'%';
GRANT INSERT, UPDATE, DELETE ON devbridge.GIT_COMMIT_ANALYSIS TO 'ssafy_ai'@'%';

-- 3. AI 전용 테이블: DML + DDL (Alembic 마이그레이션 포함)
GRANT ALL PRIVILEGES ON devbridge.document_chunks TO 'ssafy_ai'@'%';
GRANT ALL PRIVILEGES ON devbridge.usage_logs TO 'ssafy_ai'@'%';
GRANT ALL PRIVILEGES ON devbridge.alembic_version TO 'ssafy_ai'@'%';

FLUSH PRIVILEGES;