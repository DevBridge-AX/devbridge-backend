# 🌉 DevBridge AI (Backend Core)

<p align="center">
  <strong>파편화된 사내 데이터를 하나의 정형화된 지식으로 결합하는 인텔리전트 데이터 허브</strong><br />
  DevBridge AI 플랫폼의 핵심 비즈니스 로직, 권한 제어 및 AI 추론 엔진과의 데이터 연동을 담당하는 코어 API 서버입니다.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java_17-007396?style=for-the-badge&logo=java&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot_3.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" />
  <img src="https://img.shields.io/badge/MySQL_8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Data_JPA-gray?style=for-the-badge" />
  <img src="https://img.shields.io/badge/JWT_Auth-black?style=for-the-badge" />
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
| **Language / Framework** | `Java 17` & `Spring Boot 3.x` | 안정적인 엔터프라이즈급 RESTful API 구축 |
| **Database** | `MySQL 8.0` | 캘린더, 공지사항, 사용자 메타데이터 등 RDB 관리 |
| **ORM** | `Spring Data JPA` | 객체 중심의 도메인 설계 및 쿼리 생산성 극대화 |
| **Security** | `Spring Security` & `JWT` | Stateless 기반 사용자 인증 및 인가 처리 |
| **Build Tool** | `Gradle` | 의존성 관리 및 빌드 자동화 |

---

## 📂 3. Scalable Project Structure
도메인 주도 설계(DDD) 개념을 차용하여, 기능별로 응집도를 높인 디렉토리 구조입니다.

```text
src/main/java/com/devbridge/
├── global/            # 전역 설정(Security, Swagger), 에러 핸들러, 유틸리티
├── domain/            # 도메인별 패키지 분리 (응집도 향상)
│   ├── auth/          # 회원 가입, 로그인, JWT 발급 처리
│   ├── notice/        # 공지사항 및 실시간 댓글 CRUD 로직
│   ├── calendar/      # 부서 간 회의 조율 및 캘린더 데이터 적재
│   └── dictionary/    # 데이터 딕셔너리 및 도메인 용어 서빙 API
└── infrastructure/    # 외부 API 통신 (AI Engine, Webhook 연동)
