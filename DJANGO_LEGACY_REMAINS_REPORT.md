# Django 잔재 통합 보고서

작성일: 2026-03-31  
최종 업데이트: 2026-03-31 (P0/P1 조치 반영)  
범위: 프론트/백엔드/문서 전역 (Spring 이관 완료 이후 잔재 점검)

## 1. 결론 요약
- P0/P1 핵심 잔재는 조치 완료되었다.
- 현재 남은 항목은 **의도적으로 유지하는 P2 호환 레이어** 중심이다.
- 잔재는 아래 3가지로 분리 관리한다.
  - `P0` 즉시 수정: 사용자 기능/로그인 흐름에 영향
  - `P1` 조속 정리: 운영/유지보수 혼선 유발
  - `P2` 의도된 호환 잔재: 당장 유지 가능(근거 문서화 필요)

## 2. 통합 잔재 목록

### P0. 런타임 영향 잔재 (즉시 수정)

1. Google 로그인 성공 후 앱 인증 실패 가능 구조
- 파일: [route.ts](/Users/mac/Documents/prompthub-springboot/frontend/app/api/auth/[...nextauth]/route.ts)
- 현재 상태:
  - `/api/auth/google/` 교환 실패를 `catch`에서 흡수
  - `signIn` callback이 항상 `true`
  - `token.djangoToken` 미설정이어도 `/home` 이동 가능
- 영향:
  - “Google 창은 통과했는데 홈에서 비로그인” 증상 발생
- 조치 상태: `완료`
  - 교환 실패 시 sign-in 실패 강제 처리 반영
  - `BACKEND_AUTH_*` 코드 기준 사용자 메시지 매핑 반영

### P1. 기능 영향은 간접적이나 혼선 큰 잔재 (조속 정리)

1. 인증 세션 네이밍이 Django로 고정
- 파일:
  - [route.ts](/Users/mac/Documents/prompthub-springboot/frontend/app/api/auth/[...nextauth]/route.ts)
  - [use-auth.ts](/Users/mac/Documents/prompthub-springboot/frontend/hooks/use-auth.ts)
  - [auth-orchestrator.ts](/Users/mac/Documents/prompthub-springboot/frontend/lib/auth-orchestrator.ts)
  - [next-auth.d.ts](/Users/mac/Documents/prompthub-springboot/frontend/types/next-auth.d.ts)
- 현재 상태:
  - `djangoToken`, `djangoUser`, `DJANGO_AUTH_*` 코드명 사용
- 영향:
  - Spring 운영 중 장애 분석/인수인계 시 오판 위험
- 조치 상태: `완료`
  - `backendToken/backendUser` 계열로 통일
  - 에러 코드명 `BACKEND_AUTH_*`로 전환

2. 프론트 타입/엔드포인트 설명이 Django 기준 표현 유지
- 파일: [api.ts](/Users/mac/Documents/prompthub-springboot/frontend/types/api.ts)
- 현재 상태:
  - 상단 주석이 “Django 백엔드” 기준
  - trailing slash 중심 endpoint 상수 다수 유지(`/api/.../`)
- 영향:
  - 코드 리딩 혼선, 새 API 정리 시 기준 불명확
- 조치 상태: `완료`
  - 상단 주석을 Spring 기준으로 수정
  - `Authorization: Token ...`, trailing slash 유지를 P2 호환 레이어로 명시

3. 백엔드 메서드/테스트 명명에 Django 계약 표현 잔존
- 파일 예시:
  - [CoreService.java](/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java) (`searchForDjangoContract`)
  - `*Parity*`, `*Django*` 명명 테스트 다수
- 영향:
  - 기준이 “현재 운영 스펙”이 아닌 “마이그레이션 임시 상태”로 보일 수 있음
- 조치:
  - 운영 스펙명 기반 네이밍으로 단계적 교체
  - Parity 테스트는 `migration/parity` 패키지로 분리 유지

4. 루트 README가 아직 Django 프로젝트로 설명
- 파일: [README.md](/Users/mac/Documents/prompthub-springboot/README.md)
- 현재 상태:
  - “Backend: Django” 등 과거 설명이 남아 있음
- 영향:
  - 신규 기여자/운영자 온보딩 오류
- 조치 상태: `완료`
  - Spring Boot + PostgreSQL + NextAuth(프론트) + Spring Auth(백엔드) 기준으로 README 전면 개정

5. 홈 화면 카피에 Django 표기 잔존
- 파일: [page.tsx](/Users/mac/Documents/prompthub-springboot/frontend/app/home/page.tsx)
- 현재 상태:
  - “Next.js + Django REST” 텍스트 노출
- 영향:
  - 사용자/팀 신뢰 저하(배포 상태와 불일치)
- 조치 상태: `완료`
  - “Next.js + Spring Boot”로 교체

### P2. 의도된 호환 잔재 (문서화 후 유지 가능)

1. `Authorization: Token ...` 헤더 방식 유지
- 파일:
  - [TokenAuthFilter.java](/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/security/TokenAuthFilter.java)
  - [api.ts](/Users/mac/Documents/prompthub-springboot/frontend/lib/api.ts)
- 현재 상태:
  - DRF 스타일 `Token` 접두어 유지
- 판단:
  - 보안상 즉시 문제는 아님(백엔드와 프론트가 일치)
  - 다만 업계 표준(`Bearer`)과 다름
- 조치:
  - 단기 유지 가능
  - 중기에는 `Bearer` 병행 수용 후 단계 전환 권장

2. trailing slash + legacy URI 이중 지원
- 파일:
  - [PostController.java](/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostController.java)
  - [UserController.java](/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/UserController.java)
  - [types/api.ts](/Users/mac/Documents/prompthub-springboot/frontend/types/api.ts)
- 현재 상태:
  - `/create`, `/{id}/update`, `/{id}/delete`와 슬래시 endpoint 병행
- 판단:
  - 프론트 호환성 확보 목적이면 합리적
- 조치:
  - “호환 레이어”로 명시 유지
  - API v2에서 표준 URI로 수렴

## 3. 이번 장애(구글 로그인 미완료)와 직접 연결되는 항목
- 직접 원인권:
  - `route.ts`의 실패 전파 누락(P0)
- 간접 악화 요인:
  - `django*` 네이밍 잔재(P1)로 로그/세션 해석 난이도 상승

## 4. 정리 우선순위(실행 순서)
1. `P0` Google sign-in 실패 전파 수정
2. `P1` 인증 네이밍 통일(`django*` → `backend*`)
3. `P1` README/홈 카피/타입 주석 정정
4. `P2` 호환 잔재는 ADR/운영문서로 승인 범위 고정

## 5. 판정
- 판정: **P0/P1 조치 완료, P2 호환 잔재 관리 단계**
- 현재 상태: **런타임 영향 잔재 해소, 의도적 호환 레이어 문서화 완료**
