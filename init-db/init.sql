CREATE DATABASE IF NOT EXISTS devbridge;

-- Spring 계정: devbridge 전체 권한
CREATE USER IF NOT EXISTS 'ssafy_be'@'%' IDENTIFIED BY '1234';
GRANT ALL PRIVILEGES ON devbridge.* TO 'ssafy_be'@'%';

-- FastAPI 계정: 나머지 READ ONLY + AI 도메인 테이블만 전체 권한
CREATE USER IF NOT EXISTS 'ssafy_ai'@'%' IDENTIFIED BY '1234';

-- 1. 전체 READ ONLY (Spring 테이블 포함)
GRANT SELECT ON devbridge.* TO 'ssafy_ai'@'%';

-- 2. Alembic 마이그레이션용 (새 테이블 생성 필요)
GRANT CREATE ON devbridge.* TO 'ssafy_ai'@'%';

-- 3. AI 도메인 테이블만 전체 권한
GRANT ALL PRIVILEGES ON devbridge.KNOWLEDGE_DOCUMENTS TO 'ssafy_ai'@'%';
GRANT ALL PRIVILEGES ON devbridge.DATA_SOURCES TO 'ssafy_ai'@'%';
GRANT ALL PRIVILEGES ON devbridge.DATABASE_SCHEMAS TO 'ssafy_ai'@'%';
GRANT ALL PRIVILEGES ON devbridge.GIT_COMMITS TO 'ssafy_ai'@'%';
GRANT ALL PRIVILEGES ON devbridge.document_chunks TO 'ssafy_ai'@'%';
GRANT ALL PRIVILEGES ON devbridge.usage_logs TO 'ssafy_ai'@'%';
GRANT ALL PRIVILEGES ON devbridge.alembic_version TO 'ssafy_ai'@'%';

FLUSH PRIVILEGES;