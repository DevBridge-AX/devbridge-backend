-- ============================================================
-- Migration: 직무(jobRole)와 워크스페이스 권한(permission) 분리
-- Date: 2026-06-18
-- ============================================================

-- 1. USERS 테이블에 job_role 컬럼 추가 (ENUM 값: PLANNER, DEVELOPER, QA, DESIGNER, OPERATOR, NEWCOMER)
ALTER TABLE USERS
    ADD COLUMN job_role VARCHAR(50) NULL AFTER position;

-- 2. WORKSPACE_MEMBERS 테이블의 member_role 컬럼을 permission으로 리네임
ALTER TABLE WORKSPACE_MEMBERS
    RENAME COLUMN member_role TO permission;
