# 🌉 DevBridge AX (Backend Core)

<p align="center">
  <strong>문서 · Git 커밋 · 업무 · 회의 · 채팅을 하나의 워크스페이스 지식 흐름으로 연결하는 AI 기반 프로젝트 지식 협업 플랫폼</strong><br />
  DevBridge AX의 인증/인가, 워크스페이스 데이터 관리, 핵심 도메인 API, AI Engine 연동을 담당하는 Spring Boot 기반 Backend Core API Server입니다.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java_25-007396?style=for-the-badge&logo=java&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot_4.0.6-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" />
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" />
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Data_JPA-gray?style=for-the-badge" />
  <img src="https://img.shields.io/badge/MyBatis-red?style=for-the-badge" />
  <img src="https://img.shields.io/badge/JWT_Auth-black?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Gradle_9.4.1-02303A?style=for-the-badge&logo=gradle&logoColor=white" />
</p>

---

## 📌 Summary

DevBridge AX는 프로젝트 산출물을 단순히 보관하는 서비스가 아니라, 흩어진 문서·업무·Git 변경사항·회의 기록을 하나의 프로젝트 맥락으로 연결하는 협업 플랫폼입니다.

Backend는 이 연결 흐름의 중심에서 다음 역할을 수행합니다.

- 사용자 인증/인가 및 JWT 기반 접근 제어
- Workspace 기준 데이터 분리 및 멤버 권한 검증
- Task, Document, Git, Schedule, Notification, Setting 등 핵심 도메인 API 제공
- 문서 업로드 및 AI 분석 요청/상태 관리
- Git commit 수집 및 프로젝트 맥락 데이터화
- Dashboard에서 사용할 업무·문서·Git 요약 데이터 제공
- FastAPI 기반 AI Engine과의 내부 API 통신
- AI Chat, Owner Question 등 프로젝트 질의응답 흐름 지원

> **정보가 부족한 것이 아니라, 정보 사이의 연결이 부족했다.**

---

## 🔗 Quick Links

- 🖥️ [Frontend Repository](https://github.com/DevBridge-AX/devbridge-frontend)
- ⚙️ [Backend Repository](https://github.com/DevBridge-AX/devbridge-backend)
- 🤖 [AI Engine Repository](https://github.com/DevBridge-AX/devbridge-ai-engine)
- 📝 DevBridge AX Notion / API 명세서: 링크 연결 예정
- 📊 ERD: 링크 연결 예정

---

## 🎯 1. Planning Background

### Problem 1. 프로젝트 산출물의 분산

프로젝트가 진행될수록 요구사항 정의서, API 명세서, ERD, README, 회의록, 디자인 산출물, Git commit, 업무 상태가 여러 도구에 흩어집니다.  
이로 인해 팀원은 “이 기능이 왜 만들어졌는지”, “어떤 업무가 어떤 commit으로 이어졌는지”, “해당 문서가 실제 구현과 어떻게 연결되는지”를 반복해서 찾아야 합니다.

### Problem 2. 개발자와 비개발자의 이해 기준 차이

같은 프로젝트를 보더라도 역할에 따라 필요한 정보가 다릅니다.

| 역할 | 주로 보는 관점 |
| :--- | :--- |
| 기획자 / 비개발자 | 사용자 흐름, 화면, 정책, 기능 목적 |
| 디자이너 | 화면 구조, UX, 컴포넌트 흐름 |
| 개발자 | API, DB, 권한, 예외처리, 코드 변경, 배포 |

DevBridge AX는 기획 문서, 디자인 문서, 개발 문서, Git commit, 업무 상태를 연결하여 각 역할이 자신의 관점에서 프로젝트 맥락을 이해할 수 있도록 설계되었습니다.

### Problem 3. 일반 AI의 한계

일반 생성형 AI는 우리 프로젝트의 최신 문서, 업무 상태, Git 변경사항을 알지 못합니다.  
DevBridge AX는 범용 답변이 아니라, 현재 Workspace에 축적된 문서와 Git 데이터를 기반으로 프로젝트 맥락에 맞는 답변을 제공하는 것을 목표로 합니다.

---

## 🚀 2. Service Concept

```text
Documents / Git Commits / Tasks / Meeting Notes / Design Assets
        ↓
DevBridge AX Workspace
        ↓
Backend Core API Server
        ↓
AI Engine / Dashboard / AI Chat / Owner Question
```

| 흐름 | 설명 |
| :--- | :--- |
| Workspace 중심 관리 | 프로젝트별로 업무, 문서, Git, 일정, 알림 데이터를 분리 |
| Document → AI 지식화 | 업로드된 문서를 분석 요청하고 AI Chat의 근거 데이터로 활용 |
| Task → Git 맥락 연결 | 업무와 commit을 연결하여 변경 이유와 구현 흐름 추적 |
| Dashboard 요약 | 업무 현황, 최근 문서, Git 변경사항, 지연 업무를 요약 |
| AI Chat / Owner Question | AI가 프로젝트 문맥 기반 답변을 제공하고, 부족한 내용은 담당자 확인 요청으로 연결 |

---

## 🧩 3. Main Features

| Domain | Backend Responsibility |
| :--- | :--- |
| Auth / User | 회원가입, 로그인, JWT 인증, Google OAuth, 사용자 정보 관리 |
| Workspace | 워크스페이스 생성, 멤버 관리, 접근 권한 검증, 기준 workspaceId 관리 |
| Dashboard | 업무 통계, 최근 업무, 최근 문서, 최근 Git commit, 지연 업무 데이터 제공 |
| Task | 업무 생성/조회/수정/삭제, 상태 변경, 담당자/마감일 관리, 상태 이력 관리 |
| Document | 문서 업로드, 목록/상세 조회, 미리보기, 다운로드, AI 분석 상태 관리 |
| Data Source | Git 저장소 등 외부 데이터소스 등록 및 관리 |
| Git | commit 수집, 작성자/브랜치/메시지/시간 저장, Task 및 Dashboard와 연결 |
| AI Chat | 질문 저장, AI Engine 요청, 답변 저장, 근거 데이터 연결 |
| Owner Question | AI 답변이 부족한 경우 담당자에게 확인 요청 생성 |
| Schedule | 회의 일정 생성, 조회, 조율 |
| Notification | 초대, 업무, 회의, 담당자 확인 요청 등 주요 이벤트 알림 |
| Setting | 사용자 및 워크스페이스 설정 관리 |
| Internal API | AI Engine 등 서버 간 통신용 API 제공 |

---

## 🛠 4. Tech Stack & Architecture

| 분류 | 기술 스택 | 적용 목적 / 비고 |
| :--- | :--- | :--- |
| **Language / Framework** | `Java 25`, `Spring Boot 4.0.6` | RESTful API 서버 및 핵심 비즈니스 로직 구현 |
| **Database** | `MySQL` | 사용자, 워크스페이스, 업무, 문서, Git, 일정, 알림 데이터 저장 |
| **Cache / Token Store** | `Redis` | 인증 토큰, 캐시성 데이터, 이메일 인증 등 빠른 조회가 필요한 데이터 관리 |
| **ORM** | `Spring Data JPA` | 도메인 Entity 중심의 CRUD 및 관계 매핑 |
| **SQL Mapper** | `MyBatis` | Dashboard 통계, 복잡한 조회 쿼리 처리 |
| **Security** | `Spring Security`, `JWT`, `OAuth2 Client` | Stateless 인증/인가 및 Google OAuth 로그인 연동 |
| **Mail** | `Spring Mail`, `Thymeleaf` | 이메일 인증 및 메일 템플릿 처리 |
| **Realtime / Streaming** | `WebSocket`, `WebFlux(WebClient)` | 실시간 알림/채팅 확장, AI Engine HTTP/SSE 통신 |
| **API Docs** | `springdoc-openapi` | Swagger UI 기반 API 문서화 |
| **Build Tool** | `Gradle 9.4.1` | 의존성 관리 및 빌드 자동화 |
| **Container** | `Docker`, `Docker Compose` | Backend, MySQL, Redis, AI Engine 실행 환경 구성 |
| **Infra** | `AWS EC2`, `Nginx`, `GitHub Actions` | 서버 배포 및 운영 환경 구성 |

---

## 🏗 5. Backend Architecture

```text
[Frontend - Vue.js]
        ↓ REST API / WebSocket
[Backend - Spring Boot]
        ↓
[MySQL] [Redis]
        ↓ Internal API / WebClient
[AI Engine - FastAPI]
        ↓
[Vector Store / RAG Pipeline]
```
<img width="2907" height="2061" alt="AWS Cloud" src="https://github.com/user-attachments/assets/f4b0417b-5bd7-4ca2-8fb3-40476f1126bf" />

---

## 📂 6. Project Structure

계층(`api` / `domain` / `global`)을 먼저 나누고, 각 계층 내부를 기능 단위로 분리한 구조입니다.

```text
src/main/java/com/devbridge/backend/
├── api/                         # Controller 계층
│   ├── auth/                    # 회원가입, 로그인, 인증 API
│   ├── chat/                    # AI Chat, Owner Question API
│   ├── datasource/              # 외부 데이터소스 연동 API
│   ├── document/                # 문서 업로드, 조회, 분석 상태 API
│   ├── git/                     # Git commit / repository 연동 API
│   ├── internal/                # 서버 간 내부 통신 API
│   ├── notification/            # 알림 API
│   ├── schedule/                # 일정 및 회의 API
│   ├── setting/                 # 사용자/워크스페이스 설정 API
│   ├── task/                    # 업무 관리 API
│   ├── user/                    # 사용자 정보 API
│   └── workspace/               # 워크스페이스 관리 API
│
├── domain/                      # DTO, Entity, Repository, Service
│   ├── auth/
│   ├── chat/
│   ├── datasource/
│   ├── document/
│   ├── git/
│   ├── notification/
│   ├── schedule/
│   ├── setting/
│   ├── task/
│   ├── user/
│   └── workspace/
│
└── global/                      # 전역 설정 및 공통 모듈
    ├── auth/                    # JWT 발급/검증, 인증 필터
    ├── common/                  # BaseEntity, 공통 응답, 예외 처리
    └── config/                  # Security, Swagger, Redis, CORS, WebSocket, FastAPI 연동 설정
```

---

## 🔐 7. Backend Design Principles

| Principle | Description |
| :--- | :--- |
| **Workspace First** | Task, Document, Git, Chat, Schedule, Notification 등 주요 데이터는 workspaceId 기준으로 분리 |
| **API Contract First** | Frontend와 안정적으로 연동하기 위해 요청/응답 구조를 명확히 관리 |
| **Loose Coupling with AI Engine** | 문서 분석, 임베딩, RAG 답변 생성은 AI Engine에서 처리하고 Backend는 요청·상태·결과를 관리 |
| **Role-aware Knowledge Flow** | 기획 문서, 디자인 문서, 개발 문서, Git commit, 업무 상태를 연결해 개발자와 비개발자가 각자의 관점에서 프로젝트 맥락을 이해하도록 지원 |

---

## 📖 8. API Documentation

로컬 실행 후 Swagger UI에서 API 명세를 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui/index.html
```

주요 API 그룹:

```text
/auth
/users
/workspaces
/dashboard
/tasks
/documents
/data-sources
/git
/chat
/schedules
/notifications
/settings
/internal
```

---

## 🐳 9. Local Run

Docker Compose를 사용해 Backend, MySQL, Redis, AI Engine을 함께 실행할 수 있습니다.

```bash
docker compose up -d --build
```

실행 상태 확인:

```bash
docker ps
```

종료:

```bash
docker compose down
```

> 환경 변수와 세부 실행 옵션은 `.env` 및 `docker-compose.yml`을 기준으로 설정합니다.

---

## 🧭 Project Message

DevBridge AX는 프로젝트 산출물을 단순히 보관하는 서비스가 아니라, 팀이 다시 이해하고 활용할 수 있는 지식으로 연결하는 플랫폼입니다.

Backend는 이 연결 흐름의 중심에서 문서, 업무, Git, 일정, 알림, AI Chat 데이터를 안정적으로 관리하고, AI Engine과 Frontend가 활용할 수 있는 정형화된 API를 제공합니다.
