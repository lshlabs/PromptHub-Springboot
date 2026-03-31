# Stats/Search 도메인 스키마 검증 (3단계 판정)

## 1) 목적 / 판정 체계

이 문서는 `nullable / PK / FK`를 Django 일치 여부만으로 판단하지 않고,
아키텍처/실기능 타당성까지 포함해 아래 3단계로 판정한다.

- `A`: 일치 + 타당 (유지)
- `B`: 일치하지만 개선 필요 (호환 유지 + 개선 과제)
- `C`: 불일치 + 부적합 (즉시 수정 권고)

## 2) 근거 경로 (source of truth)

- Django:
  - `/Users/mac/Documents/prompthub2/backend/core/views.py` (검색)
  - `/Users/mac/Documents/prompthub2/backend/core/search.py` (검색)
  - `/Users/mac/Documents/prompthub2/backend/stats/views.py` (통계)
  - `/Users/mac/Documents/prompthub2/backend/stats/tests.py`
- Spring:
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsController.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsService.java`
- DB 증빙:
  - `information_schema.tables` 조회 결과, **stats/search 전용 테이블 없음**

## 3) 테이블별 검증 결과

### 스키마 전용 테이블 없음

Stats/Search 도메인은 **전용 테이블이 존재하지 않으며**, 기존 도메인 테이블
(`posts`, `users`, `post_interactions` 등)에 대한 **조회/집계 로직**으로만 동작한다.

따라서 `nullable / PK / FK` 관점의 **스키마 판정 항목은 없음**으로 기록한다.

## 4) B/C 항목 우선순위

- 해당 없음 (스키마 레벨 C 항목 없음)

## 5) 다음 섹션 재사용 템플릿

- 검증 항목 (`table.column` 또는 제약명)
- 현재 DB 상태
- Django 기준
- Spring 기준
- 기능상 영향
- 판정 등급 (`A/B/C`)
- 조치 권고 (`유지/개선/즉시수정`)
- 근거 경로 (Django + Spring + DB)
