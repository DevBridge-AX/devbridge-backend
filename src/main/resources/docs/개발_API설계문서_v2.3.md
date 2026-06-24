# DevBridge AX API 설계 문서 v2.3

## 1. 공통 규격

### 1.1 Base URL
- Spring Backend: `http://localhost:8080`
- AI Engine: `http://localhost:8000`

### 1.2 인증
모든 API는 JWT Bearer 토큰 인증이 필요하다 (로그인/회원가입 제외).
```
Authorization: Bearer {accessToken}
```

### 1.3 워크스페이스 컨텍스트
워크스페이스 범위 API는 헤더로 워크스페이스 ID를 전달한다.
```
X-Workspace-Id: {workspaceId}
```

### 1.4 공통 에러 응답
```json
{
  "message": "에러 메시지",
  "status": 400,
  "timestamp": "2026-06-24T12:00:00.000Z"
}
```

---

## 2. 인증 API

### POST /api/auth/login
로컬 로그인. 이메일과 비밀번호로 인증한다.

**Request Body:**
```json
{
  "email": "admin@company.com",
  "password": "ssafy1234"
}
```

**Response 200:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "employeeId": "EMP001",
  "name": "김현수",
  "systemRole": "ADMIN"
}
```

### POST /api/auth/register
회원가입. 사번(employee_id)은 HR 시스템에서 부여된 값을 입력한다.

**Request Body:**
```json
{
  "employeeId": "EMP001",
  "email": "admin@company.com",
  "password": "ssafy1234",
  "name": "김현수",
  "department": "개발팀",
  "position": "Backend Developer"
}
```

---

## 3. 워크스페이스 API

### POST /api/workspaces
워크스페이스 생성. ADMIN 권한 필요.

### GET /api/workspaces
내 워크스페이스 목록 조회.

**Response 200:**
```json
[
  {
    "id": "ws001",
    "name": "DevBridge AX",
    "description": "프로젝트 이해 지원 및 협업 자동화 시스템",
    "myPermission": "OWNER"
  }
]
```

### POST /api/workspaces/invite
멤버 초대. OWNER 권한 필요.

### GET /api/workspaces/members?keyword={keyword}
멤버 검색. 이름 부분 일치.

**Response 200:**
```json
[
  {
    "userId": "u001",
    "employeeId": "EMP001",
    "name": "김현수",
    "department": "개발팀",
    "position": "Backend Developer",
    "permission": "OWNER"
  }
]
```

---

## 4. 대시보드 API

### GET /api/dashboard/summary
워크스페이스 대시보드 요약 데이터.

**Response 200:**
```json
{
  "taskSummary": {
    "assigned": 2,
    "inProgress": 2,
    "done": 1,
    "delayed": 1
  },
  "recentCommits": [
    {
      "commitHash": "a1b2c3d",
      "message": "feat: implement dashboard API structure",
      "authorName": "김현수",
      "pushedAt": "2026-06-24T11:00:00"
    }
  ],
  "dataSources": [
    {
      "id": "ds001",
      "sourceType": "DOC",
      "sourceName": "DevBridge AX 문서 저장소",
      "status": "CONNECTED"
    }
  ],
  "unreadNotificationCount": 3
}
```

---

## 5. 챗봇 API

### POST /api/chat/sessions
대화 세션 생성.

### GET /api/chat/sessions
내 대화 세션 목록.

### POST /api/chat/sessions/{sessionId}/messages
메시지 전송 및 AI 응답 수신 (SSE 스트리밍).

**Request Body:**
```json
{
  "content": "API 응답 형식은 어디 문서를 보면 되나요?"
}
```

**SSE Response:**
```
data: {"type":"token","content":"API"}
data: {"type":"token","content":" 설계"}
data: {"type":"token","content":" 문서"}
data: {"type":"done","messageId":"cm002","citations":[{"sourceType":"DOC","sourceId":"doc002","similarity":0.89}]}
```

---

## 6. 업무 API

### GET /api/tasks
워크스페이스 내 업무 목록.

### POST /api/tasks
업무 생성.

**Request Body:**
```json
{
  "title": "대시보드 API 구현",
  "description": "프로젝트별 대시보드 데이터를 조회하는 API를 구현한다.",
  "assigneeId": "u001",
  "dueDate": "2026-06-26"
}
```

---

## 7. 일정 관리 API

### POST /api/meetings
회의 조율 요청 생성. 참석자를 지정하고 소요 시간을 설정한다.

**Request Body:**
```json
{
  "title": "6월 4주차 스프린트 플래닝",
  "durationMinutes": 60,
  "participantEmployeeIds": ["EMP003", "EMP004"],
  "purpose": "대시보드 기능 우선순위 확정",
  "agenda": "1. 스프린트 목표 공유\n2. 백로그 우선순위 결정\n3. 담당자 배정"
}
```

### POST /api/meetings/{meetingId}/participants/me/times
참석자 가능 시간 제출.

### GET /api/meetings/{meetingId}
회의 상세 조회. 참석자 목록에 이름/부서/직급이 포함된다.

**Response 200 (participants 예시):**
```json
{
  "participants": [
    {
      "employeeId": "EMP003",
      "name": "최형수",
      "department": "기획팀",
      "position": "팀장",
      "role": "HOST",
      "status": "RESPONDED"
    }
  ]
}
```

---

## 8. 알림 API

### GET /api/notifications/users/{employeeId}
알림 목록 조회 (페이지네이션).

**Response 200:**
```json
{
  "content": [
    {
      "notificationId": "n001",
      "notificationType": "TASK_ASSIGNED",
      "referenceId": "t001",
      "workspaceId": "ws001",
      "title": "새 업무 배정",
      "message": "최형수 님이 대시보드 API 구현 업무를 배정했습니다.",
      "isRead": false,
      "createdAt": "2026-06-24T12:00:00"
    }
  ],
  "totalElements": 5,
  "number": 0,
  "size": 20
}
```

### PUT /api/notifications/{notificationId}/read
알림 읽음 처리.

---

## 9. AI 엔진 내부 API (Spring → FastAPI)

### POST /api/chat (AI Engine)
RAG 기반 질의응답. Spring이 대화 히스토리와 워크스페이스 컨텍스트를 전달한다.

### POST /api/ingestion/documents (AI Engine)
문서 인덱싱 요청. 파일을 청킹하고 임베딩하여 벡터 스토어에 저장한다.

### POST /api/ingestion/git (AI Engine)
Git 저장소 커밋 인덱싱. 커밋 diff를 분석하고 벡터화한다.

### POST /api/analysis/document (AI Engine)
문서 분석 요청. 요약, 키워드, 위험도, 다음 액션을 추출한다.
