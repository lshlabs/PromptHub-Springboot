# 🚀 PromptHub Spring Boot 마이그레이션 마스터플랜

## 1. 🛠️ 기술 스택 및 아키텍처 (Tech Stack)

### 백엔드 핵심 스택
- **Language/Framework**: `Java 21 (LTS)` + `Spring Boot 3.5.x`
- **Build Tool**: `Gradle`
- **Database**: `PostgreSQL`
- **ORM/Query**: `Spring Data JPA` + `QueryDSL` *(복잡한 동적 조회 쿼리 작성 시 선택적으로 사용)*
- **Security**: `Spring Security` + `OAuth2 Resource Server`
- **Test/Monitor**: `JUnit5`, `Mockito`, `Testcontainers`, `Micrometer` + `Prometheus`

### AI-Driven Development (AI 주도 개발) 도입 및 역할 정의

본 프로젝트는 개발 생산성 극대화를 위해 AI(Cursor/Copilot 등)를 적극 도입하되, **'철저한 역할 분리(Human as Architect, AI as Typist)'** 원칙을 고수합니다.

- **AI의 역할 (단순 구현 위임):**
  - Python ➔ Java 문법 번역
  - DTO/Controller 보일러플레이트 초안 생성
  - 기능 연결을 위한 baseline scaffold 작성

- **엔지니어의 역할 (아키텍처 통제):**
  - DB ERD/인덱스 전략
  - 도메인 권한 검증
  - 영속성 컨텍스트(JPA) 튜닝
  - 동시성 제어
  - 정합성 기준과 테스트 기준 설계
  - AI가 만든 baseline 리뷰 및 리팩터링

- **재투자 전략:**
  - AI로 절약된 시간을 Security 체인 설계, JPA N+1 최적화, 캐시 전략, 통합 테스트 강화 같은 고난도 엔지니어링 영역에 재투자한다.

---

## 2. 🧭 Troubleshooting 문서화 원칙

이 프로젝트의 troubleshooting 문서는 **"구현 중 우연히 발생한 버그 회고"** 중심으로 작성하지 않는다.

대신, **AI가 빠르게 작성한 baseline을 Human Architect가 아키텍처/정합성/확장성/성능 관점에서 리뷰하는 과정에서 발견한 구조적 한계**를 문서화한다.

즉, 문제의 출처는 단순 구현 실수라기보다 아래와 같이 정의한다.

1. AI가 빠르게 작성한 baseline
2. Human Architect가 식별한 숨겨진 구조적 위험
3. correctness criteria를 다시 정의한 뒤 진행한 리팩터링
4. 테스트/측정으로 검증한 결과

따라서 모든 troubleshooting 문서는 다음 질문에 답할 수 있어야 한다.

- AI baseline은 무엇을 빠르게 해결했는가
- 그 baseline의 구조적 한계는 무엇이었는가
- Human Architect는 어떤 correctness criteria를 새로 정의했는가
- 어떤 리팩터링 전략으로 구조를 재설계했는가
- 테스트와 측정을 통해 무엇을 검증했는가

---

## 3. 🚀 핵심 기능별 Spring Boot 전환 전략 (Core Deep Dive)

기존 Django 기반의 핵심 비즈니스 로직을 Spring 생태계의 Best Practice에 맞게 재설계한다.

### 3-1) Google OAuth 토큰 백엔드 재검증 및 계정 연동

- **구현 목표:** 클라이언트를 신뢰하지 않고, 서버에서 직접 `aud`, `iss`, `sub` 클레임을 검증하여 계정 탈취를 방지한다.
- **설계 포인트:** `GoogleTokenVerifier` 컴포넌트를 구현하고, 이메일이 아닌 Google 고유 식별자(`sub`)를 기준으로 사용자를 식별한다. 이후 내부 인증은 자체 JWT(`Access/Refresh`) 체계로 전환한다.

### 3-2) 게시글 도메인 규칙 강제 (3중 검증 아키텍처)

- **구현 목표:** 비정상적인 데이터가 DB에 저장되지 않도록 한다.
- **설계 포인트 (3중 방어):**
  1. **DTO:** `@Valid` 기반 형식/길이/필수값 검증
  2. **Domain/Service:** 교차 필드 규칙, 상태 전이, 권한 검증
  3. **DB:** `CHECK`, `UNIQUE`, `NOT NULL` 등 제약조건으로 최종 방어

### 3-3) 조회수 동시성 처리 (Race Condition 방지)

- **구현 목표:** 고트래픽 환경에서 Lost Update를 방지하고 조회수 정합성을 보장한다.
- **설계 포인트:** 초기에는 DB 원자적 업데이트(`update Post p set p.viewCount = p.viewCount + 1`)로 구현하고, 트래픽 증가 시 Redis `INCR` 기반 비동기 벌크 업데이트 구조로 확장 가능성을 열어둔다.

> **💡 [AI Baseline Review 기반 Troubleshooting 포인트]**
>
> - **Baseline:** AI는 기능 연결을 우선해 서비스 계층 중심의 빠른 초안을 작성한다.
> - **구조적 한계:** Human Architect는 이 baseline이 동시성/정합성/DTO 분리 관점에서 충분히 안전한지 검토한다.
> - **리팩터링 방향:** 조회수 증가를 원자적 쿼리로 분리하고, 상세 응답은 API 전용 DTO로 제한하며, 필요한 데이터만 명시적으로 조회하도록 재설계한다.

### 3-4) 트렌딩 랭킹 API 캐시 적용 (DB 병목 방지)

- **구현 목표:** 랭킹 집계 쿼리로 인한 DB 부하를 Redis 캐싱으로 상쇄한다.
- **설계 포인트:** `@Cacheable` 활용, 복합 캐시 키(`trend:{window}:{category}:{sort}`) 설계, TTL 정책 적용.

> **💡 [AI Baseline Review 기반 Troubleshooting 포인트]**
>
> - **Baseline:** AI는 우선 기능 중심 캐시 코드를 빠르게 구성한다.
> - **구조적 한계:** Human Architect는 TTL 만료 시 동시 요청이 몰릴 때 Cache Stampede 가능성, DTO Projection 누락에 따른 N+1 가능성을 검토한다.
> - **리팩터링 방향:** `@Cacheable(sync = true)` 또는 분산 락을 검토하고, 조회를 DTO Projection 중심으로 재구성한다.

### 3-5) 검색 쿼리 최적화 및 필터/정렬 통합

- **구현 목표:** 동적 필터링과 안전한 정렬을 지원하는 고성능 검색 API를 구축한다.
- **설계 포인트:** `QueryDSL`을 활용해 타입 안전성을 확보하고, 필요한 경우 Cursor 기반 페이지네이션을 검토한다. 인덱스는 실제 조회 패턴 기준으로 설계한다.

---

## 4. 🛡️ 테스트 전략 (Testing Strategy)

포트폴리오의 신뢰성을 증명하기 위해 명확한 테스트 전략을 유지한다.

- **단위 테스트 (Controller/Service):**
  - `JUnit5` + `Mockito`
  - 도메인 규칙 위반, OAuth 검증 실패, 권한 실패 케이스 검증

- **슬라이스/통합 테스트 (Repository/Infra):**
  - `Testcontainers` (PostgreSQL, Redis)
  - QueryDSL 동적 필터 정확성
  - `@Cacheable` hit/miss
  - Redis 장애 시 fallback
  - 동시성/정합성 검증

- **품질 게이트:**
  - 단순 커버리지보다 핵심 비즈니스 규칙 검증을 우선
  - N+1 발생 여부 확인
  - baseline flaw가 있는 경우, 최종 완료 판정은 리팩터링 후 기준으로 판단

---

## 5. 📅 주차별 개발 마일스톤 (4주 완성)

- **1주차: 뼈대 구축 및 인증 (AI 가속)**
  - Spring Boot 기본 세팅, 예외 처리, 응답 포맷 통합
  - AI를 활용한 DTO/Controller 보일러플레이트 구축
  - Security 체인 및 Google OAuth 통합 (직접 설계)

- **2주차: 도메인 규칙 및 동시성 1차 구현**
  - 게시글 도메인 규칙 강제 파이프라인 완성
  - 조회수 동시성 처리 baseline 검토 및 리팩터링 방향 수립
  - 테스트 기준 정의

- **3주차: JPA 튜닝 및 캐시 안정화**
  - QueryDSL 동적 검색 쿼리 최적화 및 인덱스 적용
  - Redis 트렌딩 캐시 구축 및 Cache Stampede 방어 전략 반영

- **4주차: 인가(Authorization) 최적화 및 운영 준비**
  - 사용자 권한별 API 접근 제어 세분화
  - Testcontainers 기반 통합 테스트 완성
  - troubleshooting 문서/포트폴리오 문서 정리