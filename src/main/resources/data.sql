-- ============================================================
-- Spring Boot 자동 실행 시드 데이터 (defer-datasource-initialization: true)
-- Hibernate DDL 이후 매 기동 시 실행 — INSERT IGNORE로 중복 방지
-- ============================================================

-- 1. USERS
INSERT IGNORE INTO users (
    id, created_at, updated_at, deleted_at,
    employee_id, email, password_hash, auth_provider,
    name, department, position, job_role, system_role
) VALUES
('u001', NOW(), NOW(), NULL, 'EMP001', 'admin@company.com',    '$2a$10$uAK.crtZC9lnuTPV/i5q7.wIj7jhFZTXbvq7WmKp/63bCEw4i27LO', 'LOCAL', '김현수', '개발팀',  'Backend Developer', 'DEVELOPER', 'ADMIN'),
('u002', NOW(), NOW(), NULL, 'EMP003', 'choie000208@gmail.com','$2a$10$uAK.crtZC9lnuTPV/i5q7.wIj7jhFZTXbvq7WmKp/63bCEw4i27LO', 'LOCAL', '최형수', '기획팀',  '팀장',              'PLANNER',   'USER'),
('u003', NOW(), NOW(), NULL, 'EMP004', 'sam000208@naver.com',  '$2a$10$uAK.crtZC9lnuTPV/i5q7.wIj7jhFZTXbvq7WmKp/63bCEw4i27LO', 'LOCAL', '최펭수', '개발팀',  '대리',              'QA',        'USER'),
('u004', NOW(), NOW(), NULL, 'EMP005', 'designer@company.com', '$2b$12$4iwz6DVqmDU36V/Q8sjN8eiK.7zRjCTgShy7ZRFFYsRSr4ch7eRCS', 'LOCAL', '홍길동', '디자인팀','디자이너',           'DESIGNER',  'USER'),
('u005', NOW(), NOW(), NULL, 'EMP006', 'operator@company.com', '$2a$10$uAK.crtZC9lnuTPV/i5q7.wIj7jhFZTXbvq7WmKp/63bCEw4i27LO', 'LOCAL', '이순신', '운영팀',  '대리',              'OPERATOR',  'USER'),
('u006', NOW(), NOW(), NULL, 'EMP007', 'newcomer@company.com', '$2a$10$uAK.crtZC9lnuTPV/i5q7.wIj7jhFZTXbvq7WmKp/63bCEw4i27LO', 'LOCAL', '강감찬', '인사팀',  '사원',              'NEWCOMER',  'USER');

-- 2. WORKSPACES
INSERT IGNORE INTO workspaces (
    id, created_at, updated_at, deleted_at,
    name, description
) VALUES
('ws001', NOW(), NOW(), NULL, 'DevBridge AX',   '프로젝트 이해 지원 및 협업 자동화 시스템'),
('ws002', NOW(), NOW(), NULL, 'RestArt',        'AI 기반 전시 리플릿 서비스'),
('ws003', NOW(), NOW(), NULL, '더미 워크스페이스', '통합 테스트용 워크스페이스');

-- 3. WORKSPACE_MEMBERS
INSERT IGNORE INTO workspace_members (
    id, created_at, updated_at, deleted_at,
    workspace_id, user_id, permission, joined_at
) VALUES
('wm001', NOW(), NOW(), NULL, 'ws001', 'u001', 'OWNER',  NOW()),
('wm002', NOW(), NOW(), NULL, 'ws001', 'u002', 'MEMBER', NOW()),
('wm003', NOW(), NOW(), NULL, 'ws001', 'u003', 'MEMBER', NOW()),
('wm004', NOW(), NOW(), NULL, 'ws001', 'u004', 'MEMBER', NOW()),
('wm005', NOW(), NOW(), NULL, 'ws001', 'u005', 'MEMBER', NOW()),
('wm006', NOW(), NOW(), NULL, 'ws001', 'u006', 'MEMBER', NOW()),
('wm007', NOW(), NOW(), NULL, 'ws002', 'u001', 'MEMBER', NOW()),
('wm008', NOW(), NOW(), NULL, 'ws003', 'u001', 'OWNER',  NOW()),
('wm009', NOW(), NOW(), NULL, 'ws003', 'u002', 'MEMBER', NOW()),
('wm010', NOW(), NOW(), NULL, 'ws003', 'u003', 'MEMBER', NOW()),
('wm011', NOW(), NOW(), NULL, 'ws003', 'u004', 'MEMBER', NOW()),
('wm012', NOW(), NOW(), NULL, 'ws003', 'u005', 'MEMBER', NOW()),
('wm013', NOW(), NOW(), NULL, 'ws003', 'u006', 'MEMBER', NOW());

-- 4. TASKS
INSERT IGNORE INTO tasks (
    id, created_at, updated_at, deleted_at,
    workspace_id, requester_id, assignee_id,
    title, description, status, due_date
) VALUES
('t001', NOW(), NOW(), NULL, 'ws001', 'u002', 'u001', '대시보드 API 구현',       '프로젝트별 대시보드 데이터를 조회하는 REST API를 구현한다.',          'IN_PROGRESS', DATE_ADD(NOW(), INTERVAL 2 DAY)),
('t002', NOW(), NOW(), NULL, 'ws001', 'u002', 'u004', '대시보드 UI 배치 개선',    'Figma 와이어프레임 기준으로 대시보드 레이아웃을 재배치한다.',          'ASSIGNED',    DATE_ADD(NOW(), INTERVAL 3 DAY)),
('t003', NOW(), NOW(), NULL, 'ws001', 'u001', 'u001', 'Git 변경 이력 연동',      '최근 Git push 내역을 커밋 분석 후 대시보드에 표시한다.',             'DONE',        DATE_SUB(NOW(), INTERVAL 1 DAY)),
('t004', NOW(), NOW(), NULL, 'ws001', 'u002', 'u004', '알림 목록 UI 정리',       '대시보드 알림 목록의 읽음/안읽음 상태를 시각적으로 구분한다.',          'DELAYED',     DATE_SUB(NOW(), INTERVAL 2 DAY)),
('t005', NOW(), NOW(), NULL, 'ws001', 'u002', 'u003', 'API 응답 형식 검증',      '대시보드 API 응답이 프론트엔드 스펙과 일치하는지 검증한다.',           'ASSIGNED',    DATE_ADD(NOW(), INTERVAL 5 DAY)),
('t006', NOW(), NOW(), NULL, 'ws001', 'u001', 'u005', '운영 환경 모니터링 설정',   '서버 상태 및 에러율 모니터링 대시보드를 구성한다.',                   'IN_PROGRESS', DATE_ADD(NOW(), INTERVAL 4 DAY));

-- 5. DATA_SOURCES
INSERT IGNORE INTO data_sources (
    id, created_at, updated_at, deleted_at,
    workspace_id, source_type, source_name, status
) VALUES
('ds001', NOW(), NOW(), NULL, 'ws001', 'DOC', 'DevBridge AX 문서 저장소',        'CONNECTED'),
('ds002', NOW(), NOW(), NULL, 'ws001', 'GIT', 'devbridge-backend Git 저장소',    'CONNECTED'),
('ds003', NOW(), NOW(), NULL, 'ws001', 'GIT', 'devbridge-frontend Git 저장소',   'CONNECTED');

-- 6. KNOWLEDGE_DOCUMENTS
INSERT IGNORE INTO knowledge_documents (
    id, created_at, updated_at, deleted_at,
    source_id, workspace_id, title, document_type, vector_id, analysis_status
) VALUES
('doc001', NOW(), NOW(), NULL, 'ds001', 'ws001', '요구사항 정의서 v1.2',         'REQUIREMENTS', 'vec_req_001',   'COMPLETED'),
('doc002', NOW(), NOW(), NULL, 'ds001', 'ws001', 'API 설계 문서 v2.3',          'API_SPEC',     'vec_api_002',   'COMPLETED'),
('doc003', NOW(), NOW(), NULL, 'ds001', 'ws001', 'Figma 와이어프레임 — 대시보드', 'DESIGN',       'vec_figma_003', 'COMPLETED'),
('doc004', NOW(), NOW(), NULL, 'ds001', 'ws001', '회의록 — 6월 스프린트 킥오프',  'MEETING_NOTE', NULL,            'PENDING');

-- 7. GIT_COMMITS
INSERT IGNORE INTO git_commits (
    id, created_at, updated_at, deleted_at,
    source_id, workspace_id, author_id,
    author_name, author_email,
    commit_hash, short_hash, commit_message, branch_name, pushed_at
) VALUES
('gc001', NOW(), NOW(), NULL, 'ds002', 'ws001', 'u001',
 '김현수', 'admin@company.com',
 'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0', 'a1b2c3d',
 'feat: implement dashboard API structure', 'feature/dashboard',
 DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('gc002', NOW(), NOW(), NULL, 'ds002', 'ws001', 'u001',
 '김현수', 'admin@company.com',
 'b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1', 'b2c3d4e',
 'fix: update chatbot citation card layout', 'feature/chat',
 DATE_SUB(NOW(), INTERVAL 3 HOUR)),
('gc003', NOW(), NOW(), NULL, 'ds003', 'ws001', 'u004',
 '홍길동', 'designer@company.com',
 'c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2', 'c3d4e5f',
 'feat: add workspace member list component', 'feature/dashboard',
 DATE_SUB(NOW(), INTERVAL 5 HOUR));

-- 8. CHAT_SESSIONS
INSERT IGNORE INTO chat_sessions (
    id, created_at, updated_at, deleted_at,
    workspace_id, user_id, session_title
) VALUES
('cs001', NOW(), NOW(), NULL, 'ws001', 'u001', '대시보드 API 관련 질문'),
('cs002', NOW(), NOW(), NULL, 'ws001', 'u002', '요구사항 문서 검토 요청');

-- 9. CHAT_MESSAGES
INSERT IGNORE INTO chat_messages (
    id, created_at, updated_at, deleted_at,
    session_id, sender_type, content,
    prompt_tokens, completion_tokens, context_truncated
) VALUES
('cm001', NOW(), NOW(), NULL, 'cs001', 'USER',
 'API 응답 형식은 어디 문서를 보면 되나요?',
 12, 0, false),
('cm002', NOW(), NOW(), NULL, 'cs001', 'AI',
 'API 설계 문서 v2.3의 3장 "REST API 응답 공통 규격"을 참고하시면 됩니다. 응답은 CommonResponse 래퍼로 감싸져 있으며 data, message, timestamp 필드를 포함합니다.',
 12, 58, false),
('cm003', NOW(), NOW(), NULL, 'cs002', 'USER',
 '대시보드에 표시할 최근 커밋 수 제한이 정해져 있나요?',
 15, 0, false),
('cm004', NOW(), NOW(), NULL, 'cs002', 'AI',
 '요구사항 정의서에 따르면 대시보드 Git 섹션은 최근 5건의 커밋을 표시하도록 되어 있습니다. 해당 내용에 대해 담당자에게 확인이 필요합니다.',
 15, 42, false);

-- 10. OWNER_CONFIRMATIONS
INSERT IGNORE INTO owner_confirmations (
    id, created_at, updated_at, deleted_at,
    workspace_id, task_id, question_message_id, assigned_owner_id,
    requester_id, status, answer_content, is_faq, target_role, answered_at
) VALUES
('oc001', NOW(), NOW(), NULL, 'ws001', 't001', 'cm001', 'u001',
 'u001', 'PENDING', NULL, 0, 'Backend Developer', NULL),
('oc002', NOW(), NOW(), NULL, 'ws001', NULL, 'cm004', 'u002',
 'u002', 'ANSWERED', '최근 5건이 맞습니다. 페이지네이션은 2차 스프린트에서 추가 예정입니다.',
 1, '팀장', NOW());

-- 11. NOTIFICATIONS
INSERT IGNORE INTO notifications (
    id, created_at, updated_at, deleted_at,
    user_id, type, reference_id, title, message, is_read
) VALUES
('n001', NOW(), NOW(), NULL, 'u001', 'TASK_ASSIGNED',                  't001', '새 업무 배정',     '최형수 님이 "대시보드 API 구현" 업무를 배정했습니다.',                 0),
('n002', NOW(), NOW(), NULL, 'u001', 'OWNER_CONFIRMATION_REQUESTED',   'oc001', '담당자 확인 요청', 'API 응답 형식에 대한 확인 요청이 도착했습니다.',                       0),
('n003', NOW(), NOW(), NULL, 'u002', 'OWNER_CONFIRMATION_ANSWERED',    'oc002', '담당자 답변 완료', '최근 커밋 수 제한에 대한 답변이 등록되었습니다.',                      0),
('n004', NOW(), NOW(), NULL, 'u004', 'TASK_ASSIGNED',                  't002', '새 업무 배정',     '최형수 님이 "대시보드 UI 배치 개선" 업무를 배정했습니다.',              1),
('n005', NOW(), NOW(), NULL, 'u003', 'TASK_ASSIGNED',                  't005', '새 업무 배정',     '최형수 님이 "API 응답 형식 검증" 업무를 배정했습니다.',                 0),
('n006', NOW(), NOW(), NULL, 'u005', 'TASK_ASSIGNED',                  't006', '새 업무 배정',     '김현수 님이 "운영 환경 모니터링 설정" 업무를 배정했습니다.',             0);

-- 12. MEETINGS
INSERT IGNORE INTO meetings (
    id, created_at, updated_at, deleted_at,
    workspace_id, title, purpose, duration_minutes, status,
    confirmed_start_time, confirmed_end_time, ai_summary, meeting_link, top_candidate_times
) VALUES
('meet001', NOW(), NOW(), NULL, 'ws001',
 '6월 4주차 스프린트 플래닝',
 '대시보드 기능 우선순위 확정 및 담당자 배정',
 60, 'GATHERING', NULL, NULL, NULL, NULL, NULL),
('meet002', NOW(), NOW(), NULL, 'ws001',
 'API 설계 리뷰 — 대시보드 v2',
 'REST API 응답 공통 규격 최종 확정',
 45, 'CONFIRMED',
 DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 1 DAY), INTERVAL 45 MINUTE),
 NULL, 'https://meet.google.com/abc-defg-hij', NULL),
('meet003', NOW(), NOW(), NULL, 'ws003',
 '더미 워크스페이스 킥오프',
 '테스트 시나리오 공유 및 역할 분담',
 60, 'GATHERING', NULL, NULL, NULL, NULL, NULL),
('meet004', NOW(), NOW(), NULL, 'ws003',
 'QA 회고 미팅',
 '1차 QA 결과 공유 및 버그 우선순위 논의',
 30, 'CONFIRMED',
 DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 2 DAY), INTERVAL 30 MINUTE),
 NULL, 'https://meet.google.com/xyz-uvwx-yz0', NULL);

-- 13. MEETING_PARTICIPANTS
INSERT IGNORE INTO meeting_participants (
    id, created_at, updated_at, deleted_at,
    meeting_id, employee_id, status, role
) VALUES
('meetp001', NOW(), NOW(), NULL, 'meet001', 'EMP003', 'PENDING',   'HOST'),
('meetp002', NOW(), NOW(), NULL, 'meet001', 'EMP001', 'PENDING',   'ATTENDEE'),
('meetp003', NOW(), NOW(), NULL, 'meet001', 'EMP004', 'PENDING',   'ATTENDEE'),
('meetp004', NOW(), NOW(), NULL, 'meet001', 'EMP005', 'PENDING',   'ATTENDEE'),
('meetp005', NOW(), NOW(), NULL, 'meet002', 'EMP001', 'RESPONDED', 'HOST'),
('meetp006', NOW(), NOW(), NULL, 'meet002', 'EMP003', 'RESPONDED', 'ATTENDEE'),
('meetp007', NOW(), NOW(), NULL, 'meet002', 'EMP004', 'RESPONDED', 'ATTENDEE'),
('meetp008', NOW(), NOW(), NULL, 'meet003', 'EMP003', 'PENDING',   'HOST'),
('meetp009', NOW(), NOW(), NULL, 'meet003', 'EMP004', 'PENDING',   'ATTENDEE'),
('meetp010', NOW(), NOW(), NULL, 'meet004', 'EMP003', 'RESPONDED', 'HOST'),
('meetp011', NOW(), NOW(), NULL, 'meet004', 'EMP004', 'RESPONDED', 'ATTENDEE');
