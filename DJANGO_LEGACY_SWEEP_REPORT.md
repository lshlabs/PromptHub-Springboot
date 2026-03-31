# Django Legacy Sweep Report

작성일: 2026-03-31  
범위: `/frontend`, `/backend`, 루트 문서/설정  
목적: Spring Boot 전환 이후 남은 Django/DRF 잔재 전수 점검

## 스캔 기준

- 키워드(대소문자 무시): `django`, `drf`, `python`, `wsgi`, `asgi`, `gunicorn`, `celery`, `csrftoken`, `sessionid`, `serializer`, `viewset`
- 예외(잔재 판정 제외):
  - `Authorization: Token ...` (의도적 호환)
  - trailing slash URI (`/api/.../`) (의도적 호환)
  - `DJANGO_LEGACY_REMAINS_REPORT.md` 등 히스토리/검증 문서

## 결론 요약

- 런타임 코드 기준으로 즉시 청소 대상은 **5건(P1)**.
- 테스트/검증 명명 잔재는 다수 존재하지만 동작 영향은 낮음(**P2**).
- 문서/설정 일부는 목적성 유지 가능하나, 운영 기준 문서 분리 필요.

---

## 1) 발견된 잔재 목록 (코드 우선)

1. `frontend/lib/api.ts:2`

- 발견: `API 클라이언트 - Django 백엔드와의 통신을 위한 모든 함수들`
- 제안: `Spring Boot 백엔드 통신` 문구로 교체

1. `frontend/lib/api.ts:295`

- 발견: `Django Token Authentication은 토큰 갱신을 지원하지 않습니다.`
- 제안: `Backend Token Auth 호환 레이어에서는 토큰 갱신 미지원`으로 교체

1. `backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java:61`

- 발견: `searchForDjangoContract(...)`
- 제안: `searchForCompatibilityContract(...)` 등 중립 명칭으로 리네이밍

1. `backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java:38`

- 발견: `coreService.searchForDjangoContract(...)`
- 제안: 3번 리네이밍에 맞춰 호출부 동기화

1. `backend/src/main/java/com/lshlabs/prompthubspring/common/TrailingSlashCompatibilityFilter.java:15`

- 발견: `legacy Django-style trailing slash routes compatible`
- 제안: `legacy client trailing-slash compatibility` 등 프레임워크 중립 주석으로 수정

---

## 2) 발견된 잔재 목록 (테스트/검증 명명)

아래는 기능 동작보다는 테스트 네이밍/설명 잔재:

- `backend/src/test/java/com/lshlabs/prompthubspring/verification/DjangoSpringMappingEvidenceVerificationTest.java`
- `backend/src/test/java/com/lshlabs/prompthubspring/security/PublicEndpointParitySecurityTest.java`
- `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreServiceSection6Test.java`
- `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreOptionsContractParityTest.java`
- `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreTrendingModelParityTest.java`
- `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreSearchContractParityTest.java`
- `backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`
- `backend/src/test/java/com/lshlabs/prompthubspring/user/UserInfoContractParityTest.java`
- `backend/src/test/java/com/lshlabs/prompthubspring/user/UserControllerFlowTest.java`
- `backend/src/test/java/com/lshlabs/prompthubspring/auth/UserAgentParserTest.java`
- `backend/src/test/java/com/lshlabs/prompthubspring/post/RelativeTimeFormatterTest.java`
- `backend/src/test/java/com/lshlabs/prompthubspring/post/PostInteractionOrderingRepositoryTest.java`
- `backend/src/test/java/com/lshlabs/prompthubspring/smoke/ReleaseGateSmokeTest.java`

권고:

- 클래스/메서드명의 `Django`를 `Legacy`/`Compatibility`로 단계적 전환
- 동등성 증빙 문서와 테스트의 의미 연결은 유지

---

## 3) 문서/설정 잔재

1. `.dockerignore`

- 발견: Python 가상환경/캐시 ignore 다수 (`__pycache__`, `*.pyc`, `.Python`, `venv`)
- 판단: 현재 Java/Node 중심 운영에서는 불필요 가능
- 권고: 필요 시 제거, 아니면 “운영 외 데이터 툴링 대비” 주석 명시

1. `BACKEND_API_COMPAT_MATRIX.md`, `DJANGO_SPRING_DIFF_CHECKLIST.md` 등

- 발견: Django 용어 다수
- 판단: 해당 문서가 “마이그레이션/동등성 증빙” 목적이면 유지 가능
- 권고: 운영 가이드와 검증 문서를 분리해 혼선 방지

---

## 4) 오탐(잔재 아님)

- `sessionId` 식별자 2건은 `sessionid` 키워드 오탐:
  - `frontend/app/profile/settings/page.tsx:127`
  - `frontend/components/profile/settings/security-section.tsx:23`

---

## 5) 우선순위 제안

### P1 (즉시 청소 권장, 동작 영향/혼선 큼)

- `frontend/lib/api.ts` 2건
- `CoreService/CoreController` 메서드명 2건
- `TrailingSlashCompatibilityFilter` 주석 1건

### P2 (단계적 청소)

- 테스트 네이밍 `Django`* 전환
- `.dockerignore` Python 잔재 정리 여부 결정

---

## 6) 완료 기준(이 문서 기준)

- P1 5건 수정 완료
- 테스트/문서 잔재는 목적성(증빙 vs 운영) 기준으로 유지/정리 분류 완료
- 예외 항목(`Authorization: Token`, trailing slash)은 호환 레이어로 명시 유지

