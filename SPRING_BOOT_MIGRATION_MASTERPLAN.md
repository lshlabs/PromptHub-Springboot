# 🚀 PromptHub Spring Boot 마이그레이션 마스터플랜

## 1. 🛠️ 기술 스택 및 아키텍처 (Tech Stack)

### 백엔드 핵심 스택

- **Language/Framework**: `Java 21 (LTS)` + `Spring Boot 3.3.x` (최신 생태계 및 가상 스레드 이점 활용)
- **Build Tool**: `Gradle (Kotlin DSL)`
- **Database**: `PostgreSQL 16` (주력 DB) / `Redis` (캐시 및 분산락)
- **ORM/Query**: `Spring Data JPA` + `QueryDSL` (타입 안정성이 보장된 동적 쿼리 구현)
- **Security**: `Spring Security` + `OAuth2 Resource Server` (Google ID Token/JWT 인가)
- **Test/Monitor**: `JUnit5`, `Mockito`, `Testcontainers`, `Micrometer` + `Prometheus`

### AI-Driven Development (AI 주도 개발) 도입 및 역할 정의

본 프로젝트는 개발 생산성 극대화를 위해 AI(Cursor/Copilot 등)를 적극 도입하되, 엔터프라이즈급 품질 확보를 위해 **'철저한 역할 분리(Human as Architect, AI as Typist)'** 원칙을 고수합니다.

- **AI의 역할 (단순 구현 위임):** Python ➔ Java 문법 번역, DTO/Controller 껍데기 및 보일러플레이트 초안 생성. 이를 통해 단순 타이핑 시간을 70% 이상 단축.
- **엔지니어의 역할 (아키텍처 통제):** DB ERD/인덱스 전략, 도메인 권한 검증, 영속성 컨텍스트(JPA) 튜닝, 동시성 제어 등 핵심 정합성은 인간이 직접 설계하고 AI의 코드를 리뷰 및 리팩토링.
- **재투자 전략:** AI로 절약된 시간을 Spring Security 필터 체인 설계 및 JPA N+1 쿼리 최적화 등 고난도 엔지니어링 영역에 전량 재투자.

---

## 2. 🚀 핵심 기능별 Spring Boot 전환 전략 (Core Deep Dive)

기존 Django 기반의 핵심 비즈니스 로직을 Spring 생태계의 Best Practice에 맞게 재설계합니다.

### 2-1) Google OAuth 토큰 백엔드 재검증 및 계정 연동

- **구현 목표:** 클라이언트를 신뢰하지 않고, 서버에서 직접 `aud`, `iss`, `sub` 클레임을 검증하여 계정 탈취 방지.
- **설계 포인트:** `GoogleTokenVerifier` 컴포넌트를 구현하여 서명을 검증하고, 이메일이 아닌 고유 식별자(`sub`)를 기준으로 사용자를 식별 및 자동 가입 처리. 자체 JWT(`Access/Refresh`) 파이프라인 구축.

### 2-2) 게시글 도메인 규칙 강제 (3중 검증 아키텍처)

- **구현 목표:** 비정상적인 데이터(예: 비공개인데 발행일이 존재)의 DB 삽입 원천 차단.
- **설계 포인트 (3중 방어):** 1. **DTO:** `@Valid` (형식, 길이 검증)
  1. **Domain/Service:** 엔티티 내부 캡슐화 로직을 통한 교차 필드 논리 검증
  2. **DB:** `CHECK`, `UNIQUE` 제약조건으로 최종 방어

### 2-3) 조회수 동시성 처리 (Race Condition 방지)

- **구현 목표:** 고트래픽 환경에서 Lost Update를 방지하고 조회수 정합성 보장.
- **설계 포인트:** 초기에는 DB 원자적 업데이트(`update Post p set p.viewCount = p.viewCount + 1`)로 구현. 트래픽 증가 시 Redis `INCR` 기반의 비동기 벌크 업데이트로 확장 설계.

> **💡 [리팩토링 계획: AI 구조적 한계 극복]**
>
> - **🔴 초기 한계 (Baseline):** 단순 `@Transactional`과 변경 감지를 활용한 코드 (충돌 시 Lost Update 발생 위험), DTO 없이 Entity를 직접 반환하여 연관관계 순환 참조 위험 내포.
> - **🚨 위험성 분석:** 트래픽 집중 시 DB Write Hotspot 발생 및 JSON 직렬화 과정에서 N+1 쿼리 연쇄 폭발 가능성.
> - **✅ 엔지니어링 리팩토링:** 조회수 증가는 원자적 쿼리로 분리 격리. 상세 조회 응답은 API 전용 DTO로 강제하고, 필요한 데이터만 `Fetch Join`으로 가져오도록 리팩토링하여 DB 커넥션 점유 시간 최소화.

### 2-4) 트렌딩 랭킹 API 캐시 적용 (DB 병목 방지)

- **구현 목표:** 실시간 랭킹 집계 쿼리로 인한 DB 부하를 Redis 캐싱으로 상쇄.
- **설계 포인트:** `@Cacheable` 활용. 랭킹 윈도우/카테고리에 따른 복합 캐시 키(`trend:{window}:{category}:{sort}`) 설계 및 60초 TTL 정책 적용.

> **💡 [리팩토링 계획: Cache Stampede 방어]**
>
> - **🔴 초기 한계 (Baseline):** 단순 고정 키 방식의 캐싱 및 연관 데이터 지연 로딩(Lazy Loading) 방치.
> - **🚨 위험성 분석:** TTL 만료 시점에 대규모 트래픽이 몰리면 수백 개의 쓰레드가 동시에 DB에 랭킹 집계 쿼리를 날리는 Cache Stampede(캐시 붕괴) 현상 발생.
> - **✅ 엔지니어링 리팩토링:** `@Cacheable(sync = true)` 옵션 또는 분산 락(Redisson)을 적용하여 캐시 미스 시 단일 스레드만 DB에 접근하도록 통제. 랭킹 DTO Projection을 통해 지연 로딩으로 인한 N+1 쿼리 완벽 차단.

### 2-5) 검색 쿼리 최적화 및 필터/정렬 통합

- **구현 목표:** 동적 필터링과 안전한 정렬을 지원하는 고성능 검색 API 구축.
- **설계 포인트:** `QueryDSL`을 활용하여 컴파일 타임에 쿼리 오류를 잡고, Cursor 기반 페이지네이션을 적용하여 대용량 데이터 조회 시 Offset 병목 완화. 복합 인덱스(`status, category, created_at`) 사전 설계.

---

## 3. 🛡️ 테스트 전략 (Testing Strategy)

포트폴리오의 신뢰성을 증명하기 위해 명확한 테스트 피라미드를 구축합니다.

- **단위 테스트 (Controller/Service, 70%):** `JUnit5` + `Mockito` 활용. 도메인 규칙 위반, OAuth 검증 실패 등 엣지 케이스 집중 검증.
- **슬라이스/통합 테스트 (Repository/Infra, 25%):** `Testcontainers` (PostgreSQL, Redis) 연동. QueryDSL 동적 필터 정확성, `@Cacheable` 히트/미스 동작 및 Redis 장애 시 Fallback 검증.
- **품질 게이트:** 단순 라인 커버리지 수치보다 '핵심 비즈니스 규칙 커버'를 우선시하며, N+1 쿼리 발생 여부를 통합 테스트에서 검증.

---

## 4. 📅 주차별 개발 마일스톤 (4주 완성)

- **1주차: 뼈대 구축 및 인증 (AI 가속)**
  - Spring Boot 기본 세팅, 예외 처리, 응답 포맷 통합.
  - AI를 활용한 DTO/Controller 보일러플레이트 구축.
  - Security 체인 및 Google OAuth 통합 (직접 설계).
- **2주차: 도메인 규칙 및 동시성 1차 구현**
  - 게시글 도메인 규칙 강제 파이프라인 완성.
  - 조회수 동시성 처리 로직 리팩토링 및 테스트 증명.
- **3주차: JPA 튜닝 및 캐시 안정화**
  - QueryDSL 동적 검색 쿼리 최적화 및 인덱스 적용.
  - Redis 트렌딩 캐시 구축 및 Cache Stampede 방어 로직 구현.
- **4주차: 인가(Authorization) 최적화 및 운영 준비**
  - 사용자 권한별 API 접근 제어 세분화.
  - Testcontainers 기반 통합 테스트 완성 및 포트폴리오 문서화.

