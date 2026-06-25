-- ============================================================
-- 시연용 시드 데이터 (defer-datasource-initialization: true)
-- Hibernate DDL 이후 매 기동 시 실행 — INSERT IGNORE로 중복 방지
-- ============================================================

-- ============================================================
-- 1. USERS (5명 — EMP004 최펭수는 회원가입 시연용으로 제외)
-- ============================================================
INSERT IGNORE INTO users (
    id, created_at, updated_at, deleted_at,
    employee_id, email, password_hash, auth_provider,
    name, department, position, job_role, system_role
) VALUES
('u001', NOW(), NOW(), NULL, 'EMP001', 'admin@company.com',     '$2a$10$uAK.crtZC9lnuTPV/i5q7.wIj7jhFZTXbvq7WmKp/63bCEw4i27LO', 'LOCAL', '김현수', '개발팀',   'Backend Developer', 'DEVELOPER', 'ADMIN'),
('u002', NOW(), NOW(), NULL, 'EMP003', 'choie000208@gmail.com', '$2a$10$uAK.crtZC9lnuTPV/i5q7.wIj7jhFZTXbvq7WmKp/63bCEw4i27LO', 'LOCAL', '최형수', '기획팀',   '팀장',              'PLANNER',   'USER'),
('u004', NOW(), NOW(), NULL, 'EMP005', 'designer@company.com',  '$2a$10$uAK.crtZC9lnuTPV/i5q7.wIj7jhFZTXbvq7WmKp/63bCEw4i27LO', 'LOCAL', '전우석', '디자인팀', '디자이너',           'DESIGNER',  'USER'),
('u005', NOW(), NOW(), NULL, 'EMP006', 'operator@company.com',  '$2a$10$uAK.crtZC9lnuTPV/i5q7.wIj7jhFZTXbvq7WmKp/63bCEw4i27LO', 'LOCAL', '김서연', '운영팀',   '대리',              'OPERATOR',  'USER'),
('u006', NOW(), NOW(), NULL, 'EMP007', 'newcomer@company.com',  '$2a$10$uAK.crtZC9lnuTPV/i5q7.wIj7jhFZTXbvq7WmKp/63bCEw4i27LO', 'LOCAL', '김정윤', '인사팀',   '사원',              'NEWCOMER',  'USER');

-- ============================================================
-- 2. WORKSPACES (2개)
-- ============================================================
INSERT IGNORE INTO workspaces (
    id, created_at, updated_at, deleted_at,
    name, description
) VALUES
('ws001', NOW(), NOW(), NULL, 'DevBridge AX', '프로젝트 이해 지원 및 협업 자동화 시스템'),
('ws002', NOW(), NOW(), NULL, 'RestArt',      'AI 기반 전시 리플릿 서비스');

-- ============================================================
-- 3. WORKSPACE_MEMBERS
-- ============================================================
INSERT IGNORE INTO workspace_members (
    id, created_at, updated_at, deleted_at,
    workspace_id, user_id, permission, joined_at
) VALUES
('wm001', NOW(), NOW(), NULL, 'ws001', 'u001', 'OWNER',  NOW()),
('wm002', NOW(), NOW(), NULL, 'ws001', 'u002', 'MEMBER', NOW()),
('wm003', NOW(), NOW(), NULL, 'ws001', 'u004', 'MEMBER', NOW()),
('wm004', NOW(), NOW(), NULL, 'ws001', 'u005', 'MEMBER', NOW()),
('wm005', NOW(), NOW(), NULL, 'ws001', 'u006', 'MEMBER', NOW()),
('wm006', NOW(), NOW(), NULL, 'ws002', 'u001', 'OWNER',  NOW()),
('wm007', NOW(), NOW(), NULL, 'ws002', 'u002', 'MEMBER', NOW());

-- ============================================================
-- 4. TASKS (10건 — 상태별 분포: ASSIGNED 2, IN_PROGRESS 3, DONE 3, DELAYED 2)
-- ============================================================
INSERT IGNORE INTO tasks (
    id, created_at, updated_at, deleted_at,
    workspace_id, requester_id, assignee_id,
    title, description, status, due_date
) VALUES
('t001', NOW(), NOW(), NULL, 'ws001', 'u002', 'u001', '대시보드 REST API 구현',          '프로젝트별 대시보드 요약 데이터를 조회하는 API를 구현한다.',             'IN_PROGRESS', DATE_ADD(NOW(), INTERVAL 2 DAY)),
('t002', NOW(), NOW(), NULL, 'ws001', 'u002', 'u004', '대시보드 UI 레이아웃 구현',        'Figma 와이어프레임 기준으로 대시보드 카드 레이아웃을 구현한다.',          'IN_PROGRESS', DATE_ADD(NOW(), INTERVAL 3 DAY)),
('t003', NOW(), NOW(), NULL, 'ws001', 'u001', 'u001', 'Git 커밋 분석 연동',              '최근 Git push 내역을 AI 엔진으로 분석하여 대시보드에 표시한다.',         'DONE',        DATE_SUB(NOW(), INTERVAL 1 DAY)),
('t004', NOW(), NOW(), NULL, 'ws001', 'u002', 'u004', '알림 드로어 UI 정리',             '알림 목록의 읽음/안읽음 상태를 시각적으로 구분하고 클릭 네비게이션 구현.', 'DELAYED',     DATE_SUB(NOW(), INTERVAL 2 DAY)),
('t005', NOW(), NOW(), NULL, 'ws001', 'u002', 'u005', 'API 응답 형식 QA 검증',           '대시보드 API 응답이 프론트엔드 스펙과 일치하는지 검증한다.',              'ASSIGNED',    DATE_ADD(NOW(), INTERVAL 5 DAY)),
('t006', NOW(), NOW(), NULL, 'ws001', 'u001', 'u005', '운영 환경 모니터링 대시보드 구성',  '서버 상태 및 에러율 모니터링 Grafana 대시보드를 구성한다.',              'IN_PROGRESS', DATE_ADD(NOW(), INTERVAL 4 DAY)),
('t007', NOW(), NOW(), NULL, 'ws001', 'u002', 'u001', 'WebSocket 알림 푸시 구현',        '실시간 알림을 WebSocket으로 브라우저에 전달하는 기능을 구현한다.',        'DONE',        DATE_SUB(NOW(), INTERVAL 3 DAY)),
('t008', NOW(), NOW(), NULL, 'ws001', 'u001', 'u002', '요구사항 정의서 v1.3 업데이트',    '6월 스프린트 결정 사항을 반영하여 요구사항 정의서를 갱신한다.',           'DONE',        DATE_SUB(NOW(), INTERVAL 1 DAY)),
('t009', NOW(), NOW(), NULL, 'ws001', 'u002', 'u006', '신규 인원 온보딩 문서 작성',       '프로젝트 구조 및 개발 환경 세팅 가이드를 작성한다.',                    'ASSIGNED',    DATE_ADD(NOW(), INTERVAL 7 DAY)),
('t010', NOW(), NOW(), NULL, 'ws001', 'u001', 'u004', 'Figma 디자인 시스템 컴포넌트 정리', '버튼/카드/모달 등 공통 컴포넌트를 디자인 시스템으로 정리한다.',           'DELAYED',     DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================================
-- 5. DATA_SOURCES
-- ============================================================
INSERT IGNORE INTO data_sources (
    id, created_at, updated_at, deleted_at,
    workspace_id, source_type, source_name, status
) VALUES
('ds001', NOW(), NOW(), NULL, 'ws001', 'DOC', 'DevBridge AX 문서 저장소',     'CONNECTED'),
('ds002', NOW(), NOW(), NULL, 'ws001', 'GIT', 'devbridge-backend Git 저장소', 'CONNECTED');

-- ============================================================
-- 6. KNOWLEDGE_DOCUMENTS
-- ============================================================
INSERT IGNORE INTO knowledge_documents (
    id, created_at, updated_at, deleted_at,
    source_id, workspace_id, title, document_type, vector_id, analysis_status
) VALUES
('doc001', NOW(), NOW(), NULL, 'ds001', 'ws001', '요구사항 정의서 v1.2',         'REQUIREMENTS', 'vec_req_001',   'COMPLETED'),
('doc002', NOW(), NOW(), NULL, 'ds001', 'ws001', 'API 설계 문서 v2.3',          'API_SPEC',     'vec_api_002',   'COMPLETED'),
('doc003', NOW(), NOW(), NULL, 'ds001', 'ws001', 'Figma 와이어프레임 — 대시보드', 'DESIGN',       'vec_figma_003', 'COMPLETED'),
('doc004', NOW(), NOW(), NULL, 'ds001', 'ws001', '회의록 — 6월 스프린트 킥오프',  'MEETING_NOTE', NULL,            'PENDING');

-- ============================================================
-- 7. GIT_COMMITS
-- ============================================================
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
 'feat: add WebSocket notification push', 'feature/notification',
 DATE_SUB(NOW(), INTERVAL 3 HOUR)),
('gc003', NOW(), NOW(), NULL, 'ds002', 'ws001', 'u001',
 '김현수', 'admin@company.com',
 'c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2', 'c3d4e5f',
 'fix: resolve chatbot citation card rendering issue', 'feature/chat',
 DATE_SUB(NOW(), INTERVAL 5 HOUR)),
('gc004', NOW(), NOW(), NULL, 'ds002', 'ws001', 'u001',
 '김현수', 'admin@company.com',
 'd4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3', 'd4e5f6a',
 'feat: meeting auto-scheduling algorithm', 'feature/schedule',
 DATE_SUB(NOW(), INTERVAL 8 HOUR));

-- ============================================================
-- 8. CHAT_SESSIONS
-- ============================================================
INSERT IGNORE INTO chat_sessions (
    id, created_at, updated_at, deleted_at,
    workspace_id, user_id, session_title
) VALUES
('cs001', NOW(), NOW(), NULL, 'ws001', 'u001', '대시보드 API 응답 형식 질문'),
('cs002', NOW(), NOW(), NULL, 'ws001', 'u002', '스프린트 결정사항 확인');

-- ============================================================
-- 9. CHAT_MESSAGES
-- ============================================================
INSERT IGNORE INTO chat_messages (
    id, created_at, updated_at, deleted_at,
    session_id, sender_type, content,
    prompt_tokens, completion_tokens, context_truncated
) VALUES
('cm001', NOW(), NOW(), NULL, 'cs001', 'USER',
 'API 응답 형식은 어디 문서를 보면 되나요?', 12, 0, false),
('cm002', NOW(), NOW(), NULL, 'cs001', 'AI',
 'API 설계 문서 v2.3의 3장 "REST API 응답 공통 규격"을 참고하시면 됩니다. 응답은 CommonResponse 래퍼로 감싸져 있으며 data, message, timestamp 필드를 포함합니다.',
 12, 58, false),
('cm003', NOW(), NOW(), NULL, 'cs002', 'USER',
 '대시보드 Git 커밋 표시 건수가 몇 건으로 정해졌나요?', 15, 0, false),
('cm004', NOW(), NOW(), NULL, 'cs002', 'AI',
 '요구사항 정의서에 따르면 대시보드 Git 섹션은 최근 5건의 커밋을 표시하도록 되어 있습니다. 해당 내용에 대해 담당자에게 확인이 필요합니다.',
 15, 42, false);

-- ============================================================
-- 10. OWNER_CONFIRMATIONS
-- ============================================================
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

-- ============================================================
-- 11. NOTIFICATIONS (10건 — 김현수 5, 최형수 3, 전우석 1, 김정윤 1)
-- ============================================================
INSERT IGNORE INTO notifications (
    id, created_at, updated_at, deleted_at,
    user_id, type, reference_id, workspace_id, title, message, is_read
) VALUES
('n001', NOW(), NOW(), NULL, 'u001', 'TASK_ASSIGNED',                't001', 'ws001', '새 업무 배정',       '최형수 님이 "대시보드 REST API 구현" 업무를 배정했습니다.',             0),
('n002', NOW(), NOW(), NULL, 'u001', 'OWNER_CONFIRMATION_REQUESTED', 'oc001', 'ws001', '담당자 확인 요청',   'API 응답 형식에 대한 확인 요청이 도착했습니다.',                        0),
('n003', NOW(), NOW(), NULL, 'u001', 'MEETING_INVITED',              'meet001', 'ws001', '회의 초대',        '"7월 1주차 스프린트 플래닝" 회의에 초대되었습니다.',                     0),
('n004', NOW(), NOW(), NULL, 'u001', 'TASK_ASSIGNED',                't007', 'ws001', '새 업무 배정',       '최형수 님이 "WebSocket 알림 푸시 구현" 업무를 배정했습니다.',            1),
('n005', NOW(), NOW(), NULL, 'u001', 'OWNER_CONFIRMATION_ANSWERED',  'oc002', 'ws001', '담당자 답변 완료',   '커밋 표시 건수에 대한 답변이 등록되었습니다.',                           1),
('n006', NOW(), NOW(), NULL, 'u002', 'TASK_ASSIGNED',                't008', 'ws001', '새 업무 배정',       '김현수 님이 "요구사항 정의서 v1.3 업데이트" 업무를 배정했습니다.',        0),
('n007', NOW(), NOW(), NULL, 'u002', 'OWNER_CONFIRMATION_ANSWERED',  'oc002', 'ws001', '담당자 답변 완료',   '최근 커밋 수 제한에 대한 답변이 등록되었습니다.',                        0),
('n008', NOW(), NOW(), NULL, 'u002', 'MEETING_INVITED',              'meet002', 'ws001', '회의 초대',        '"API 설계 리뷰 — 대시보드 v2" 회의에 초대되었습니다.',                  1),
('n009', NOW(), NOW(), NULL, 'u004', 'TASK_ASSIGNED',                't002', 'ws001', '새 업무 배정',       '최형수 님이 "대시보드 UI 레이아웃 구현" 업무를 배정했습니다.',            0),
('n010', NOW(), NOW(), NULL, 'u006', 'TASK_ASSIGNED',                't009', 'ws001', '새 업무 배정',       '최형수 님이 "신규 인원 온보딩 문서 작성" 업무를 배정했습니다.',           0);

-- ============================================================
-- 12. MEETINGS (3건 — GATHERING/CONFIRMED/CANCELED)
-- ============================================================
INSERT IGNORE INTO meetings (
    id, created_at, updated_at, deleted_at,
    workspace_id, title, purpose, duration_minutes, status,
    confirmed_start_time, confirmed_end_time, ai_summary, meeting_link, top_candidate_times
) VALUES
('meet001', NOW(), NOW(), NULL, 'ws001',
 '7월 1주차 스프린트 플래닝',
 '대시보드 MVP 완성 후 다음 스프린트 목표 수립 및 업무 배정',
 60, 'GATHERING', NULL, NULL, NULL, NULL, NULL),
('meet002', NOW(), NOW(), NULL, 'ws001',
 'API 설계 리뷰 — 대시보드 v2',
 'REST API 응답 공통 규격 최종 확정 및 프론트 연동 점검',
 45, 'CONFIRMED',
 DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 1 DAY), INTERVAL 45 MINUTE),
 NULL, 'https://meet.google.com/abc-defg-hij', NULL),
('meet003', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, 'ws001',
 '긴급 버그 대응 회의',
 '인코딩 이슈 및 권한 매핑 버그 대응 논의',
 30, 'CANCELED', NULL, NULL, NULL, NULL, NULL);

-- ============================================================
-- 13. MEETING_PARTICIPANTS
-- ============================================================
INSERT IGNORE INTO meeting_participants (
    id, created_at, updated_at, deleted_at,
    meeting_id, employee_id, status, role
) VALUES
-- meet001 (GATHERING — 시간 제출 시연용)
('mp001', NOW(), NOW(), NULL, 'meet001', 'EMP003', 'PENDING',   'HOST'),
('mp002', NOW(), NOW(), NULL, 'meet001', 'EMP001', 'PENDING',   'ATTENDEE'),
('mp003', NOW(), NOW(), NULL, 'meet001', 'EMP005', 'PENDING',   'ATTENDEE'),
('mp004', NOW(), NOW(), NULL, 'meet001', 'EMP006', 'PENDING',   'ATTENDEE'),
-- meet002 (CONFIRMED)
('mp005', NOW(), NOW(), NULL, 'meet002', 'EMP001', 'RESPONDED', 'HOST'),
('mp006', NOW(), NOW(), NULL, 'meet002', 'EMP003', 'RESPONDED', 'ATTENDEE'),
('mp007', NOW(), NOW(), NULL, 'meet002', 'EMP005', 'RESPONDED', 'ATTENDEE'),
-- meet003 (CANCELED)
('mp008', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, 'meet003', 'EMP001', 'PENDING', 'HOST'),
('mp009', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, 'meet003', 'EMP003', 'PENDING', 'ATTENDEE');
