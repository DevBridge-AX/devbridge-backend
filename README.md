# 🌉 DevBridge AI (Backend Core)

<p align="center">
  <strong>파편화된 사내 데이터를 하나의 정형화된 지식으로 결합하는 인텔리전트 데이터 허브</strong><br />
  DevBridge AI 플랫폼의 핵심 비즈니스 로직, 권한 제어 및 AI 추론 엔진과의 데이터 연동을 담당하는 코어 API 서버입니다.
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

## 🔗 Quick Links (상세 문서 & 설계도)
> 💡 **백엔드의 뼈대가 되는 API 명세 및 DB 설계도는 아래에서 확인 가능합니다.**
* 📝 [DevBridge AI 공식 Notion (요구사항 & API 명세서)](https://notion-link-here.notion.site)
* 📊 [ERD (Entity Relationship Diagram) 설계도](https://erd-link-here.com)
* 🛠️ [기획서, 유스케이스 및 WBS 일정 관리 저장소](https://notion-link-here.com)

---

## 🎯 1. Core Value (백엔드 핵심 역할)

### 🛑 Problem (파편화된 데이터와 보안 이슈)
1. **Raw Data의 범람**: Git 커밋, Figma, 사내 문서 등 형태가 제각각인 데이터들이 정제되지 않은 채 방치되어 활용이 불가능함.
2. **보안 및 권한 부재**: 현업 담당자가 데이터를 직접 조회하고 싶어도, 직무별/부서별 권한 제어가 없어 민감한 DB에 직접 접근시키는 것이 위험함.

### 🚀 Solution (백엔드 코어 시스템의 해결 방안)
* **지식 통합 파이프라인 (Data Hub)**: 수집기(Ingestion)가 가져온 비정형 데이터를 `domain_glossary`와 매핑하여, AI와 프론트엔드가 즉시 소비할 수 있는 **정형화된 API(Well-structured Documents)**로 서빙.
* **직무 기반 안전한 접근 제어 (RBAC)**: JWT 기반의 토큰 인증을 통해 기획자, 개발자, 관리자의 권한을 분리하고, 인가된 데이터 및 생성된 스마트 쿼리만 안전하게 반환.

---

## 🛠 2. Tech Stack & Architecture

| 분류 | 기술 스택 | 적용 목적 / 비고 |
| :--- | :--- | :--- |
| **Language / Framework** | `Java 25` & `Spring Boot 4.0.6` | 안정적인 엔터프라이즈급 RESTful API 구축 |
| **Database** | `MySQL` & `Redis` | RDB(캘린더, 공지사항, 사용자 메타데이터)와 캐시/토큰 저장소 이원화 |
| **ORM** | `Spring Data JPA` & `MyBatis` | 객체 중심 도메인 설계(JPA) + 복잡한 쿼리 생산성(MyBatis) 병행 |
| **Security** | `Spring Security`, `JWT`, `OAuth2 Client` | Stateless 기반 사용자 인증/인가 및 구글 소셜 로그인 연동 |
| **Realtime / Streaming** | `WebSocket` & `WebFlux(WebClient)` | 순수 WebSocket 실시간 통신, SSE 스트리밍 수신(MVC와 공존) |
| **API Docs** | `springdoc-openapi` | Swagger UI 기반 API 명세 자동화 |
| **Build Tool** | `Gradle 9.4.1` | 의존성 관리 및 빌드 자동화 |

---

## 📂 3. Scalable Project Structure
계층(`api`/`domain`/`global`) 우선 분리 후, 각 계층 내부를 기능 단위(auth, chat, workspace 등)로 재분리하여 응집도를 높인 구조입니다.

```text
src/main/java/com/devbridge/backend/
├── api/               # Controller 계층 (기능별 패키지)
│   ├── auth/          # 회원 가입, 로그인 API
│   ├── chat/          # AI 챗봇 대화 / 담당자 직접 질문 API
│   ├── datasource/    # 외부 데이터소스 연동 API
│   ├── document/      # 문서 조회 및 인덱싱 API
│   ├── git/           # Git 커밋/이슈 연동 API
│   ├── internal/      # 서버 간 내부 통신용 API
│   ├── notification/  # 알림 API
│   ├── schedule/      # 캘린더 및 일정 조율 API
│   ├── setting/       # 사용자/워크스페이스 설정 API
│   ├── task/          # 작업(Task) 관리 API
│   ├── user/          # 사용자 정보 API
│   └── workspace/     # 워크스페이스 관리 API
├── domain/            # 서비스 로직 및 DTO/Entity (api와 동일한 기능별 패키지 구조)
│   └── (auth, chat, datasource, document, git, notification, schedule, setting, task, user, workspace)
└── global/            # 전역 설정 및 공통 모듈
    ├── auth/           # JWT 발급/검증, 인증 필터
    ├── common/         # BaseEntity, 공통 예외 처리
    └── config/         # Security, Swagger, Redis, CORS, WebSocket, FastAPI 연동 등 설정
