# Stats/Search 기능 동등성 검증 (쿼리/집계 계약)

## 1) 목적 / 판정 체계

이 문서는 `search`와 `stats`의 동작을 Django 원본 기준으로 1:1 대조하고,
쿼리/집계 계약의 동등성을 아래 3단계로 판정한다.

- `A`: 동등 + 타당 (유지)
- `B`: 동등성은 대부분 유지되나 설계 차이/확장 존재 (호환 유지 + 개선 과제)
- `C`: 불일치 + 부적합 (즉시 수정 권고)

## 2) 근거 경로 (source of truth)

- Django:
  - `/Users/mac/Documents/prompthub2/backend/core/views.py`
  - `/Users/mac/Documents/prompthub2/backend/core/search.py`
  - `/Users/mac/Documents/prompthub2/backend/core/filters.py`
  - `/Users/mac/Documents/prompthub2/backend/core/sorting.py`
  - `/Users/mac/Documents/prompthub2/backend/core/pagination.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/serializers.py`
  - `/Users/mac/Documents/prompthub2/backend/stats/views.py`
- Spring:
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostRepository.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostInteractionRepository.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsService.java`

## 3) Search 기능 동등성 점검

## 3.1 검색 조건/정렬/필터

- 검증 항목: `search_type` 기본값
  - Django 기준: 기본 `all` (title + content + author)
  - Spring 기준: 기본 `all` (`normalizeSearchType`)
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지 (정합화 완료)
  - 근거 경로: Django `core/search.py`, Spring `PostService.normalizeSearchType`
- 검증 항목: `search_type=content` 범위
  - Django 기준: `prompt`, `ai_response`, `additional_opinion`, `tags`
  - Spring 기준: `prompt`, `aiResponse`, `additionalOpinion`, `tags`
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지
  - 근거 경로: Django `core/search.py`, Spring `PostService.listPosts` 스펙
- 검증 항목: `search_type=author` 처리
  - Django 기준: `author.username` 포함 검색
  - Spring 기준: `author.username` 포함 검색
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지
  - 근거 경로: Django `core/search.py`, Spring `PostService.listPosts`
- 검증 항목: `categories/models` 필터에서 `기타` 처리
  - Django 기준: `기타` 선택 시 `*_etc` 값이 비어있지 않은 항목만 포함
  - Spring 기준: 동일 규칙 적용 (`categoryEtc`, `modelEtc` non-empty)
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지
  - 근거 경로: Django `core/filters.py`, Spring `PostService.buildCategoryFilterPredicate/buildModelFilterPredicate`
- 검증 항목: `satisfaction_min/max` 검색 처리
  - Django 기준: 해당 필터 없음 (검색에서 무시)
  - Spring 기준: search_type + csv filters + satisfaction 조건을 모두 AND로 통합
  - 기능상 영향: Django보다 확장된 검색 계약 (Spring 개선)
  - 판정 등급: `B`
  - 조치 권고: Spring 개선 동작으로 문서화
  - 근거 경로: Django `core/views.py`, Spring `PostService.listPosts`
- 검증 항목: 정렬 옵션 (`latest/oldest/popular/satisfaction/views`)
  - Django 기준: 정렬 로직 및 tie-breaker 명확
  - Spring 기준: 동일 로직 구현 (popular = like+bookmark, satisfaction nulls last)
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지
  - 근거 경로: Django `core/sorting.py`, Spring `PostService.resolveListSortOrders`
- 검증 항목: 페이지네이션 응답 계약
  - Django 기준: `count/total_count/next/previous/results/current_page/total_pages/has_next/has_previous`
  - Spring 기준: Django와 동일한 top-level pagination 계약
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지 (정합화 완료)
  - 근거 경로: Django `core/pagination.py`, Spring `CoreController.search`
- 검증 항목: 검색 결과 필드셋
  - Django 기준: `PostCardSerializer` (avatarSrc, authorInitial, model/category display name 포함)
  - Spring 기준: `PostCardData`로 Django 필드셋 정합화
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지 (정합화 완료)
  - 근거 경로: Django `posts/serializers.py`, Spring `PostService.PostCardData`
- 검증 항목: `filter-options` 응답 구조
  - Django 기준: `platforms`, `categories`, `models_by_platform`
  - Spring 기준: 동일 키 구조 (`models_by_platform` Map)
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지
  - 근거 경로: Django `core/views.py`, Spring `CoreService.filterOptions`

## 4) Stats 기능 동등성 점검

## 4.1 대시보드 집계 계약

- 검증 항목: 집계 필드 목록
  - Django 기준: total_posts/users/views/likes/bookmarks/avg_satisfaction
  - Spring 기준: 동일 필드 존재
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지
  - 근거 경로: Django `stats/views.py:_dashboard_payload`, Spring `StatsService.dashboard`
- 검증 항목: `avg_satisfaction` 반올림
  - Django 기준: 소수 1자리 반올림
  - Spring 기준: 소수 1자리 반올림
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지 (정합화 완료)
  - 근거 경로: Django `stats/views.py`, Spring `StatsService.scale`
- 검증 항목: `weekly_added_posts` 기준
  - Django 기준: 최근 7일 생성
  - Spring 기준: 최근 7일 생성
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지
  - 근거 경로: Django `stats/views.py`, Spring `StatsService.dashboard`
- 검증 항목: `active_users` 기준
  - Django 기준: 최근 30일 내 글 작성한 유저
  - Spring 기준: 최근 30일 내 글 작성한 유저 (distinct)
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지
  - 근거 경로: Django `stats/views.py`, Spring `PostRepository.countDistinctAuthorsSince`
- 검증 항목: `recent_posts` 필드셋
  - Django 기준: id/title/author/created_at/views/likes/platform_name/category_name
  - Spring 기준: Django 동일 필드셋 반환
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지 (정합화 완료)
  - 근거 경로: Django `_serialize_recent_posts`, Spring `StatsService.toRecentPost`
- 검증 항목: `popular_tags` 계산
  - Django 기준: comma split 후 상위 10
  - Spring 기준: 동일 로직
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지
  - 근거 경로: Django `_popular_tags`, Spring `StatsService.extractPopularTags`
- 검증 항목: `platform/category distribution` 기준
  - Django 기준: post_count > 0, count desc
  - Spring 기준: count desc (0건 제외 결과 동일)
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지
  - 근거 경로: Django `_platform_distribution/_category_distribution`, Spring `PostRepository.countPostsByPlatform/Category`
- 검증 항목: 캐시 TTL
  - Django 기준: 60초 TTL
  - Spring 기준: stats 캐시 TTL 60초 명시
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지 (정합화 완료)
  - 근거 경로: Django `cache_value_or_set`, Spring `AppConfig.cacheManager`

## 4.2 사용자 통계 집계 계약

- 검증 항목: posts_count=0 처리
  - Django 기준: 0일 때 명시적 zero/null 응답
  - Spring 기준: 집계 결과로 0/null 반환 (분기 없음)
  - 기능상 영향: 실질 동등
  - 판정 등급: `A`
  - 조치 권고: 유지
  - 근거 경로: Django `user_stats`, Spring `StatsService.userStats`
- 검증 항목: total_likes/total_bookmarks 산정 방식
  - Django 기준: 작성한 글의 like/bookmark 합계
  - Spring 기준: 작성한 글에 대한 interaction count
  - 기능상 영향: denormalized count와 불일치 가능
  - 판정 등급: `B`
  - 조치 권고: Spring 방식 유지 (정확도 우선) + 공식 스펙 문서화
  - 근거 경로: Django `user_stats`, Spring `PostInteractionRepository` + `docs/ops/spring-behavior-extension-spec.md`
  - 근거 경로: Django `user_stats`, Spring `PostInteractionRepository.countLikedReceivedByAuthor`
- 검증 항목: avg_satisfaction 반올림
  - Django 기준: 소수 1자리
  - Spring 기준: 소수 1자리
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지 (정합화 완료)
  - 근거 경로: Django `user_stats`, Spring `StatsService.scale`
- 검증 항목: most_used_platform/category
  - Django 기준: user_posts 기준 최다 사용
  - Spring 기준: 동일 기준
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지
  - 근거 경로: Django `user_stats`, Spring `PostRepository.findTopPlatformNamesByAuthor/findTopCategoryNamesByAuthor`
- 검증 항목: recent_activity 날짜
  - Django 기준: last_post/last_like/last_bookmark (ISO)
  - Spring 기준: 동일 날짜 기준 (Instant ISO)
  - 기능상 영향: 동등
  - 판정 등급: `A`
  - 조치 권고: 유지
  - 근거 경로: Django `user_stats`, Spring `PostRepository.findLastPostDateByAuthor` + `PostInteractionRepository`

## 5) 우선 수정 대상 요약

- 즉시(릴리즈 전):
  - 없음 (C 항목 정합화 완료)
- 후속(개선 필요):
  - user_stats total_likes/bookmarks 산정 방식 일관화 (`B`)
  - satisfaction_min/max 처리 계약 정리 (`B`)
