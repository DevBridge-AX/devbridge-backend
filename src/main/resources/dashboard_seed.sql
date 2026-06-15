USE devbridge;

SET SQL_SAFE_UPDATES = 0;
SET FOREIGN_KEY_CHECKS = 0;

-- DELETE FROM notifications;
-- DELETE FROM owner_confirmations;
-- DELETE FROM chat_messages;
-- DELETE FROM chat_sessions;
-- DELETE FROM git_commits;
-- DELETE FROM knowledge_documents;
-- DELETE FROM data_sources;
-- DELETE FROM task_status_logs;
-- DELETE FROM task_deliverables;
-- DELETE FROM tasks;
-- DELETE FROM workspace_members;
-- DELETE FROM workspace_invitations;
-- DELETE FROM workspaces;
-- DELETE FROM users;

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;

INSERT INTO users (
    id, created_at, updated_at, deleted_at,
    employee_id, email, password_hash, auth_provider,
    name, department, position, system_role
) VALUES
('u001', NOW(), NOW(), NULL, 'EMP001', 'admin@company.com', '$2a$10$uAK.crtZC9lnuTPV/i5q7.wIj7jhFZTXbvq7WmKp/63bCEw4i27LO', 'LOCAL', '김현수', '개발팀', 'Backend Developer', 'ADMIN'),
('u002', NOW(), NOW(), NULL, 'EMP003', 'choie000208@gmail.com', '$2a$10$uAK.crtZC9lnuTPV/i5q7.wIj7jhFZTXbvq7WmKp/63bCEw4i27LO', 'LOCAL', '최형수', '기획팀', '팀장', 'USER'),
('u003', NOW(), NOW(), NULL, 'EMP004', 'sam000208@naver.com', '$2a$10$uAK.crtZC9lnuTPV/i5q7.wIj7jhFZTXbvq7WmKp/63bCEw4i27LO', 'LOCAL', '최펭수', '개발팀', '대리', 'USER');

INSERT INTO workspaces (
    id, created_at, updated_at, deleted_at,
    name, description
) VALUES
('ws001', NOW(), NOW(), NULL, 'DevBridge AX', '프로젝트 이해 지원 및 협업 자동화 시스템'),
('ws002', NOW(), NOW(), NULL, 'RestArt', 'AI 기반 전시 리플릿 서비스'),
('dummy-workspace-id', NOW(), NOW(), NULL, '더미 워크스페이스', '임시 테스트용 워크스페이스');

INSERT INTO workspace_members (
    id, created_at, updated_at, deleted_at,
    workspace_id, user_id, member_role, joined_at
) VALUES
('wm001', NOW(), NOW(), NULL, 'ws001', 'u001', 'OWNER', NOW()),
('wm002', NOW(), NOW(), NULL, 'ws001', 'u002', 'MEMBER', NOW()),
('wm003', NOW(), NOW(), NULL, 'ws002', 'u001', 'MEMBER', NOW()),
('wm004', NOW(), NOW(), NULL, 'ws001', 'u003', 'MEMBER', NOW()),
('wm005', NOW(), NOW(), NULL, 'dummy-workspace-id', 'u001', 'OWNER', NOW()),
('wm006', NOW(), NOW(), NULL, 'dummy-workspace-id', 'u002', 'MEMBER', NOW()),
('wm007', NOW(), NOW(), NULL, 'dummy-workspace-id', 'u003', 'MEMBER', NOW());

INSERT INTO tasks (
    id, created_at, updated_at, deleted_at,
    workspace_id, requester_id, assignee_id,
    title, description, status, due_date
) VALUES
('t001', NOW(), NOW(), NULL, 'ws001', 'u002', 'u001', '대시보드 API 구현', '프로젝트별 대시보드 데이터를 조회하는 API를 구현한다.', 'IN_PROGRESS', DATE_ADD(NOW(), INTERVAL 2 DAY)),
('t002', NOW(), NOW(), NULL, 'ws001', 'u002', 'u002', '대시보드 UI 배치 개선', 'Figma 와이어프레임을 기준으로 대시보드 화면을 정리한다.', 'ASSIGNED', DATE_ADD(NOW(), INTERVAL 3 DAY)),
('t003', NOW(), NOW(), NULL, 'ws001', 'u001', 'u001', 'Git 변경 이력 연동', '최근 Git push 내역을 대시보드에 표시한다.', 'DONE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
('t004', NOW(), NOW(), NULL, 'ws001', 'u002', 'u002', '알림 목록 UI 정리', '대시보드 알림 목록과 읽음 상태를 정리한다.', 'DELAYED', DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO data_sources (
    id, created_at, updated_at, deleted_at,
    workspace_id, source_type, source_name, status
) VALUES
('ds001', NOW(), NOW(), NULL, 'ws001', 'DOC', 'DevBridge AX 문서 저장소', 'CONNECTED'),
('ds002', NOW(), NOW(), NULL, 'ws001', 'GIT', 'devbridge-backend Git 저장소', 'CONNECTED');

INSERT INTO knowledge_documents (
    id, created_at, updated_at, deleted_at,
    source_id, title, vector_id
) VALUES
('doc001', NOW(), NOW(), NULL, 'ds001', '요구사항 정의서', 'vec_req_001'),
('doc002', NOW(), NOW(), NULL, 'ds001', 'API 설계 문서 v2.3', 'vec_api_002'),
('doc003', NOW(), NOW(), NULL, 'ds001', 'Figma 와이어프레임', 'vec_figma_003');

INSERT INTO git_commits (
    id, created_at, updated_at, deleted_at,
    source_id, author_id, commit_hash, commit_message, pushed_at
) VALUES
('gc001', NOW(), NOW(), NULL, 'ds002', 'u001', 'a1b2c3d', 'feat: implement dashboard API structure', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('gc002', NOW(), NOW(), NULL, 'ds002', 'u001', 'b2c3d4e', 'fix: update chatbot citation card layout', DATE_SUB(NOW(), INTERVAL 3 HOUR));

INSERT INTO chat_sessions (
    id, created_at, updated_at, deleted_at,
    workspace_id, user_id, session_title
) VALUES
('cs001', NOW(), NOW(), NULL, 'ws001', 'u001', '대시보드 API 관련 질문');

INSERT INTO chat_messages (
    id, created_at, updated_at, deleted_at,
    session_id, sender_type, content, prompt_tokens, completion_tokens
) VALUES
('cm001', NOW(), NOW(), NULL, 'cs001', 'USER', 'API 응답 형식은 어디 문서를 보면 되나요?', 12, 0),
('cm002', NOW(), NOW(), NULL, 'cs001', 'AI', 'API 설계 문서 v2.3을 먼저 확인하면 됩니다.', 12, 30);

INSERT INTO owner_confirmations (
    id, created_at, updated_at, deleted_at,
    workspace_id, task_id, question_message_id, assigned_owner_id,
    status, answer_content, is_faq, target_role, answered_at
) VALUES
('oc001', NOW(), NOW(), NULL, 'ws001', 't001', 'cm001', 'u001', 'PENDING', NULL, 0, 'Backend Developer', NULL);

INSERT INTO notifications (
    id, created_at, updated_at, deleted_at,
    user_id, type, reference_id, is_read
) VALUES
('n001', NOW(), NOW(), NULL, 'u001', 'TASK_ASSIGNED', 't001', 0),
('n002', NOW(), NOW(), NULL, 'u001', 'OWNER_CONFIRMATION_REQUESTED', 'oc001', 0);

INSERT INTO meetings (
    id, created_at, updated_at, deleted_at,
    workspace_id, title, duration_minutes, status,
    confirmed_start_time, confirmed_end_time, ai_summary, meeting_link, top_candidate_times
) VALUES
('meet001', NOW(), NOW(), NULL, 'ws001', '[목업] 파일 업로드 테스트용 회의', 60, 'GATHERING', NULL, NULL, NULL, NULL, NULL),
('meet002', NOW(), NOW(), NULL, 'ws001', '[목업] 확정된 파일 업로드 테스트용 회의', 60, 'CONFIRMED', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL),
('meet003', NOW(), NOW(), NULL, 'dummy-workspace-id', '[목업] 파일 업로드 테스트용 회의', 60, 'GATHERING', NULL, NULL, NULL, NULL, NULL),
('meet004', NOW(), NOW(), NULL, 'dummy-workspace-id', '[목업] 확정된 파일 업로드 테스트용 회의', 60, 'CONFIRMED', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL);

INSERT INTO meeting_participants (
    id, created_at, updated_at, deleted_at,
    meeting_id, employee_id, status, role
) VALUES
('meetp001', NOW(), NOW(), NULL, 'meet001', 'EMP003', 'PENDING', 'HOST'),
('meetp002', NOW(), NOW(), NULL, 'meet001', 'EMP004', 'PENDING', 'ATTENDEE'),
('meetp003', NOW(), NOW(), NULL, 'meet002', 'EMP003', 'RESPONDED', 'HOST'),
('meetp004', NOW(), NOW(), NULL, 'meet002', 'EMP004', 'RESPONDED', 'ATTENDEE'),
('meetp005', NOW(), NOW(), NULL, 'meet003', 'EMP003', 'PENDING', 'HOST'),
('meetp006', NOW(), NOW(), NULL, 'meet003', 'EMP004', 'PENDING', 'ATTENDEE'),
('meetp007', NOW(), NOW(), NULL, 'meet004', 'EMP003', 'RESPONDED', 'HOST'),
('meetp008', NOW(), NOW(), NULL, 'meet004', 'EMP004', 'RESPONDED', 'ATTENDEE');