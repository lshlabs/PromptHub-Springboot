# DIFF-05 Core/Stats Parity Evidence

## 목적

- Django source of truth 기준으로 `검색/필터/트렌딩/통계`의 기능 의미와 계약이 Spring에서 동등하게 구현됐는지 증빙한다.
- 완료 기준: `질의 조합별 결과 비교 + 캐시/통계 계약 점검` 근거를 단일 문서로 고정한다.

## Source of Truth

- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/core/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/core/views.py`
  - `/Users/mac/Documents/prompthub2/backend/core/services/*.py`
  - `/Users/mac/Documents/prompthub2/backend/stats/views.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsService.java`

## 질의 조합별 결과 비교 (검색/필터/정렬)

- 대상 endpoint: `GET /api/core/search`
- 비교 축:
  - `search_type`: `title`, `content`, `author`, `title_content`, `all`
  - `sort`: `latest`, `oldest`, `popular`, `satisfaction`, `views`
  - `filters`: `categories`, `models`, `platforms`, 하위호환 alias(`platform`, `category`)
  - `pagination`: `current_page`, `total_pages`, `total_count`, `has_next`, `has_previous`
- 증빙 테스트:
  - `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreSearchContractParityTest.java`
- 판정 근거:
  - Django 검색 타입 의미 및 CSV 필터 조합 동작을 Spring API 결과로 검증.

## 캐시 계약 점검 (트렌딩)

- 대상 endpoint:
  - `GET /api/core/trending/category-rankings`
  - `POST /api/core/trending/refresh-cache`
- 비교 축:
  - 첫 조회 `from_cache=false`
  - 재조회 `from_cache=true`
  - refresh 이후 재조회 `from_cache=false`
  - refresh 응답 메시지 계약
- 증빙 테스트:
  - `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreTrendingCacheParityTest.java`
- 판정 근거:
  - Django 트렌딩 캐시 의미를 Spring 캐시 응답 계약으로 검증.

## 통계 계약 점검 (대시보드/유저)

- 대상 로직:
  - `StatsService.dashboard()`
  - `StatsService.userStats()`
- 비교 축:
  - 집계 필드: `total_posts`, `total_views`, `total_likes`, `total_bookmarks`, `active_users`
  - 최근 활동 필드: `last_post_date`, `last_like_date`, `last_bookmark_date`
  - 캐시 키(`stats:dashboard`) 생성
- 증빙 테스트:
  - `backend/src/test/java/com/lshlabs/prompthubspring/stats/StatsServiceSection6Test.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/stats/StatsServiceSection6PerformanceTest.java`
- 판정 근거:
  - Django 통계/활동 집계 의미를 Spring 서비스 테스트로 검증.

## 결론

- 질의 조합별 검색 비교, 캐시 계약, 통계 계약에 대한 테스트 증빙이 단일 문서로 연결됨.
- `DIFF-05`의 잔여 이슈(근거 분산)를 해소하고 완료 판정 가능 상태로 전환.

