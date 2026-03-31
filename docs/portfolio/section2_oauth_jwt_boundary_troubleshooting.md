File Path: /portfolio/section2_oauth_jwt_boundary_troubleshooting.md

# Section 2 Troubleshooting: AI Baseline 검토 과정에서 발견한 OAuth/JWT 경계 위험

## 문제 정의

이번 Spring Boot 마이그레이션에서는 기능 구현 속도를 높이기 위해 AI를 활용해 인증 baseline을 먼저 작성했다.  
baseline은 Google 로그인과 내부 JWT 발급을 빠르게 연결하는 데 유효했지만, **외부 신원 검증과 내부 세션 정책의 경계가 흐려질 수 있는 구조적 위험**을 포함하고 있었다.

즉, 이 이슈는 운영 장애가 먼저 발생해 확인된 것이 아니라,  
**AI baseline review 과정에서 Human Architect가 선제적으로 식별한 아키텍처 결함**이다.

---

## Before: AI baseline이 빠르게 해결한 것

AI baseline은 다음을 짧은 시간 안에 해결했다.

- `POST /api/auth/google/`를 통한 Google ID Token 로그인 연결
- `aud/iss/sub` 중심의 기본 클레임 검증
- 내부 Access/Refresh 토큰 발급 및 인증 필터 연결
- 프론트 인증 플로우가 동작 가능한 최소 경로 확보

즉 baseline의 목적은 완성형 보안 정책이 아니라 **빠른 이관과 기능 가시화**였다.

---

## Problem: 숨겨진 구조적 결함

baseline 검토에서 확인한 핵심 결함은 다음과 같다.

- Google 검증 책임과 내부 JWT 수명/회전/폐기 책임이 서비스 로직에서 혼재
- refresh token 상태 모델이 약해 재발급/무효화 기준이 명확하지 않음
- 실패 케이스(Provider 실패, 클레임 불일치, 재사용 시도)에 대한 계약이 정책 단위로 분리되지 않음

즉 baseline은 **기능은 동작하지만 장기 운영 시 보안 정책 확장성이 낮아질 수 있는 구조**였다.

---

## 왜 중요한지

인증 경계가 흐려지면 다음 리스크가 누적된다.

- 사고 발생 시 원인 추적과 영향 범위 파악이 어려워짐
- 토큰 회전/세션 강제 만료 같은 정책 강화 시 리팩터링 비용 급증
- 멀티 디바이스/세션 단위 제어 요구사항 대응 속도 저하

결론적으로 이 문제는 단순 버그가 아니라 **인증 아키텍처 경계 통제 문제**다.

---

## After: Human Architect의 리팩터링 방향

### 1) 경계 분리
- 외부 신원 검증: Google ID Token 검증 책임
- 내부 인증 정책: Access/Refresh 발급/회전/폐기 책임
- 인증 필터: 내부 Access token만 신뢰

### 2) refresh token 정책 명시화
- 토큰 타입(`ACCESS`, `REFRESH`)을 엔티티로 분리
- 재발급 시 기존 refresh 무효화 + 새 토큰 페어 회전
- 로그아웃 시 활성 토큰 일괄 폐기 정책 적용

### 3) 실패 계약 정리
- 잘못된 요청/클레임 오류(400)
- 외부 공급자 검증 실패(502)
- 내부 인증 상태 오류(401)

---

## 테스트/검증 방법

### 1. 단위 테스트
- access token을 refresh 파서에 넣었을 때 거절되는지 검증
- refresh 재발급 시 기존 토큰 revoke + 새 토큰 발급이 수행되는지 검증

### 2. 통합 시나리오 테스트
- Google 로그인 성공 -> access/refresh 발급 -> 보호 API 접근
- refresh 재발급 후 기존 refresh 재사용 차단 확인
- logout 후 access/refresh 모두 무효화되는지 확인

### 3. 회귀 테스트
- 기존 프론트 경로(`/api/auth/google/`, `/api/auth/login/`, `/api/auth/token/refresh/`) 호환성 확인

---

## 면접 설명 포인트

> AI로 OAuth 연동 baseline을 빠르게 만든 뒤, Human Architect 관점에서 외부 신원 검증과 내부 세션 정책이 섞여 있는 구조적 위험을 선제 식별했다. 이후 경계를 분리하고 refresh 회전/무효화 정책을 명시화했으며, 테스트로 토큰 타입 경계와 재사용 차단을 증명했다.

핵심 포인트:
- AI는 baseline 속도, Human은 경계 설계와 무결성 책임
- 문제를 기능 버그가 아니라 인증 아키텍처 결함으로 정의
- 리팩터링 후 테스트로 운영 가능한 정책임을 입증
