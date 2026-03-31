# DIFF-06 Release Gate Evidence

## 목적

- Django source of truth 기준의 사용자 핵심 플로우가 Spring 환경에서도 릴리즈 게이트 수준으로 재현 가능한지 증빙한다.
- 완료 기준: `브라우저 E2E + CI smoke gate + 결과 문서화`를 단일 근거 문서로 고정한다.

## Source of Truth

- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend`
  - `/Users/mac/Documents/prompthub2/backend/posts/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/core/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/users/urls.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/common/TrailingSlashCompatibilityFilter.java`

## 브라우저 E2E 증빙 (Playwright)

- 실행 일시: `2026-03-30 17:09~17:14 KST`
- 실행 환경:
  - Frontend: `http://localhost:3000`
  - Backend: `http://localhost:8000`
- 핵심 시나리오:
  - 홈 진입: `GET /home`
  - 커뮤니티 진입: `GET /community`
  - 트렌딩 진입: `GET /trending`
- 산출물:
  - `docs/ops/e2e-screenshots/diff06-home.png`
  - `docs/ops/e2e-screenshots/diff06-trending.png`
- 판정 근거:
  - 브라우저 실제 렌더링 경로와 UI 진입 경로가 확보됨
  - 레거시 슬래시 URL 호환(`.../`)은 필터 기반으로 보정됨

## CI Smoke Gate 증빙

- CI 워크플로우:
  - `.github/workflows/release-gate-smoke.yml`
- 게이트 테스트:
  - `backend/src/test/java/com/lshlabs/prompthubspring/smoke/ReleaseGateSmokeTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/smoke/TrailingSlashCompatibilitySmokeTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/verification/Diff06ReleaseGateEvidenceVerificationTest.java`
- 게이트 핵심 계약(비로그인 공개 경로 포함):
  - `GET /api/stats/dashboard` 공개 접근 허용(403 회귀 방지)
  - `GET /api/auth/users/{username}/summary` 공개 접근 허용(403 회귀 방지)
  - `GET /api/stats/user` 보호 경계 유지(인증 필요)
- 로컬 실행 명령(동일 게이트):
  - `cd backend && ./gradlew test --tests com.lshlabs.prompthubspring.smoke.ReleaseGateSmokeTest --tests com.lshlabs.prompthubspring.smoke.TrailingSlashCompatibilitySmokeTest --tests com.lshlabs.prompthubspring.verification.Diff06ReleaseGateEvidenceVerificationTest`

## 결과 문서화

- 릴리즈 게이트 근거 문서:
  - `docs/ops/diff06-release-gate-evidence.md` (본 문서)
- 체크리스트 반영 대상:
  - `DJANGO_SPRING_DIFF_CHECKLIST.md`의 `[DIFF-06] 호환성/릴리즈 게이트 1:1 재검증`

## 결론

- DIFF-06 완료 기준으로 정의한 `브라우저 E2E + CI smoke gate + 결과 문서화`가 단일 문서와 테스트/워크플로우로 연결됨.
- 릴리즈 직전 검증은 본 게이트를 기준으로 반복 수행할 수 있다.
