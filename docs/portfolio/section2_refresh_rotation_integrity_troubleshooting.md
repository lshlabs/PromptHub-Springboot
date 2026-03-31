File Path: /portfolio/section2_refresh_rotation_integrity_troubleshooting.md

# Section 2 - Refresh Token Lifecycle Integrity Troubleshooting

## 문제 정의
AI baseline은 Google ID Token 검증 경계를 빠르게 구현해 인증 플로우를 동작시켰지만, Human Architect 리뷰에서 refresh 토큰 수명주기 무결성(회전/재사용/소유자 일치) 기준이 불충분하다는 구조적 결함이 확인되었다.

## Before: AI가 빠르게 작성한 초기 Baseline
- `/api/auth/google` 및 내부 JWT 발급/재발급 플로우를 우선 구현
- refresh 재발급 시 기존 토큰 revoke 로직은 있었으나, 입력/클레임/저장 토큰 간 무결성 경계가 느슨함
- 인증 성공 경로 중심으로 구현되어 실패 경계 테스트가 제한적

## Problem: Architect 관점에서 확인한 숨겨진 구조적 결함
- refresh token 형식/타입 검증과 저장소 상태 검증의 결합이 약해 경계 케이스 방어가 불완전
- JWT subject와 DB 저장 refresh token 소유자 일치성 검증 기준이 명시적으로 고정되지 않음
- 결과적으로 "동작은 하지만 보안 correctness criteria가 약한 baseline" 상태

## 왜 이 문제가 중요한가
- refresh는 세션 지속성의 핵심이며, 경계가 약하면 재사용/오용 위험으로 확장된다
- 인증 경계 문제는 기능 버그보다 탐지/재현이 어렵고 운영 사고 비용이 높다
- 포트폴리오/면접에서 OAuth 검증만이 아니라 토큰 수명주기 무결성 설계까지 설명해야 한다

## After: Human Architect가 재설계한 방향
- refresh 입력 검증(`null/blank`)과 JWT 유효성 검증을 선행
- refresh 토큰 타입 검증 + 저장소 조회 + 만료 + 토큰 소유자-주체(subject) 일치 검증으로 체인 구성
- 재발급 시 기존 refresh revoke를 명시적으로 유지해 rotation invariant 강화
- 컨트롤러 입력을 DTO + `@Valid`로 고정해 실패 경계를 API 단에서 일관화

## 테스트로 어떻게 증명할지
- `AuthServiceRefreshTest`
  - refresh rotation 성공 및 기존 refresh revoke 검증
  - refresh 누락/만료/유효하지 않은 JWT/소유자 불일치 실패 검증
- `GoogleTokenVerifierTest`
  - `aud/iss/sub` 실패 경계 검증
- `AuthControllerTest`
  - `/api/auth/google` 성공/실패 및 `/api/auth/token/refresh/` 입력 검증
- 최종 회귀: `./gradlew test` 통과

## 이번 사례에서 강조할 점
- AI baseline은 기능 스캐폴딩에 강하고, Human Architect는 보안 correctness criteria 정의와 경계 강화를 담당했다.
- 이 분업이 인증 도메인에서 실질적인 품질 상승으로 연결되었다.

## 면접에서 어떻게 설명할지
- "Google 검증 이후 내부 인증을 유지하는 핵심은 refresh lifecycle integrity였습니다."
- "검증 순서를 입력 -> JWT 타입/유효성 -> 저장소 상태 -> 소유자 일치 -> 회전 불변식으로 재정의했고, 실패 경계 테스트로 증명했습니다."
- "결과적으로 인증 기능 구현을 넘어, 재사용 방지와 토큰 결속 무결성을 설계 수준으로 끌어올렸습니다."
