# Django ↔ Spring 차이 제거 계획 (7차)

## 1) 잔여 차이 우선순위 재정리 (완료 항목 제외)

완료 제외 항목:
- 차이 #01: core 트렌딩 모델 연관 조회 동등화
- 차이 #02: core 트렌딩 캐시 갱신 동등화
- 차이 #03: users 비밀번호 변경 토큰 회전 동등화
- 차이 #04: users public_profile 마스킹 동등화
- 차이 #05: posts 모델 필수/기타 조합 규칙 동등화
- 차이 #06: posts satisfaction 0.5 step 검증 동등화

최우선(겉보기만 구현됨):
- 없음

다음(릴리즈 전 필수 보완):
1. stats 활성 사용자/최근 활동 기준 시점 불일치

마지막(backlog 허용):
1. 세션 device/browser/os 상세 파싱
2. 모델 suggest 가중치 정렬/slug 기반 정교도
3. relativeTime 포맷 동등성
4. 정렬/필터 옵션 응답 shape 세부 동등화

---

## 2) 지금 가장 먼저 닫아야 할 차이 1개 추천

추천 항목: stats 활성 사용자/최근 활동 기준 시점 동등화

선정 이유:
1. 대시보드 숫자(active_users)는 운영 판단 지표라 의미 불일치가 크고, 릴리즈 전 보정이 필요함
2. user stats의 recent_activity는 사용자 활동 이력 계약의 핵심인데, Django와 시점 기준이 달라 값이 달라질 수 있음
3. 수정 범위가 stats/post repository 내부로 좁아 작업 대비 효과가 큼

---

## 3) 차이 1개 닫기 상세 계획

### Django 원본 경로
- /Users/mac/Documents/prompthub2/backend/stats/views.py
  - `_dashboard_payload`의 `active_users`
  - `user_stats`의 `recent_activity.last_like_date`, `recent_activity.last_bookmark_date`

### Django 기준 실제 동작
1. dashboard.active_users
  - 최근 30일 이내에 게시글을 작성한 사용자만 집계
  - `User.objects.filter(posts__created_at__gte=thirty_days_ago).distinct().count()`
2. user_stats.recent_activity
  - `last_post_date`: author의 최신 `Post.created_at`
  - `last_like_date`: 사용자의 `PostInteraction(is_liked=True)` 최신 `updated_at`
  - `last_bookmark_date`: 사용자의 `PostInteraction(is_bookmarked=True)` 최신 `updated_at`

### Spring 현재 동작
1. dashboard.active_users
  - `postRepository.countDistinctAuthors()` 사용(전체 기간 작성자 수)
  - 30일 조건 누락
2. user_stats.recent_activity
  - `findLastLikedAtByUser`, `findLastBookmarkedAtByUser`가 `createdAt` 기준
  - Django의 `updated_at` 기준과 불일치

### 정확히 다른 점
1. active_users 집계 기간 조건 누락(30일 -> 전체 기간)
2. last_like_date/last_bookmark_date 기준 컬럼 불일치(updated_at vs created_at)

### 왜 이 항목을 지금 먼저 닫아야 하는지
- 현재 남은 릴리즈 전 필수 보완 중 유일한 항목이며, 운영 대시보드와 사용자 통계 신뢰도를 직접 훼손하는 의미 차이라 우선 해결해야 함.

### 최종적으로 맞춰야 할 동작
1. dashboard.active_users를 Django처럼 최근 30일 작성자 distinct count로 계산
2. recent_activity의 like/bookmark 마지막 시점을 `updatedAt` 기준으로 계산
3. 기존 응답 shape/status 계약은 유지

### 수정해야 할 파일
- /Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsService.java
  - active_users 계산을 30일 조건 기반 호출로 변경
- /Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostRepository.java
  - 최근 30일 distinct author count 쿼리 메서드 추가
- /Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostInteractionRepository.java
  - last liked/bookmarked 시점 조회를 updatedAt 기준으로 변경
- 테스트
  - /Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/stats/StatsServiceSection6Test.java
  - 필요 시 통합 보강 테스트 신규 추가

### 테스트/검증 포인트
1. active_users 기간 검증
  - 40일 전 작성자 + 최근 7일 작성자 데이터 세팅
  - dashboard.active_users가 최근 30일 작성자만 카운트하는지 확인
2. recent_activity 시점 검증
  - 같은 interaction row를 update하여 updatedAt이 createdAt보다 뒤가 되게 만든 뒤
  - user_stats의 last_like_date/last_bookmark_date가 updatedAt 기준으로 반환되는지 확인
3. 회귀 검증
  - dashboard/user_stats 응답 구조(status/data 키, 필드명) 유지

### 완료 기준(이 차이가 닫혔다고 말할 수 있는 기준)
1. dashboard.active_users가 최근 30일 작성자 기준으로 집계됨
2. user_stats.last_like_date/last_bookmark_date가 updatedAt 기준으로 계산됨
3. 위 2개가 테스트 코드로 재현/증빙됨
4. 응답 계약(shape)은 기존과 동일하게 유지됨

---

## 4) 구현 결과 업데이트 (완료)

### 상태
- 완료

### 반영 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsService.java`
  - `active_users` 계산을 `최근 30일 작성자` 기준으로 변경
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostRepository.java`
  - `countDistinctAuthorsSince(Instant since)` 쿼리 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostInteractionRepository.java`
  - `last_like_date`/`last_bookmark_date` 조회를 `createdAt`에서 `updatedAt` 기준으로 변경
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/stats/StatsServiceSection6Test.java`
  - `dashboard_activeUsers_countsOnlyRecent30DaysAuthors` 추가
  - `userStats_recentActivity_usesInteractionUpdatedAt` 추가
  - 테스트 datasource를 H2로 고정해 환경 의존성 제거
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/stats/StatsServiceSection6PerformanceTest.java`
  - 테스트 datasource를 H2로 고정

### 하드코딩/placeholder 여부
- 없음. 집계/시점 계산 모두 실제 DB 필드 기준으로 동작

### Django와 동일하게 맞춘 핵심 동작
1. `dashboard.active_users` = 최근 30일 내 게시글 작성 사용자(distinct)
2. `user_stats.recent_activity.last_like_date/last_bookmark_date` = interaction `updated_at` 기준
3. 기존 응답 구조(`status`, `data`, 키 이름)는 유지

### 실행 테스트
- `./gradlew test --tests "com.lshlabs.prompthubspring.stats.StatsServiceSection6Test" --tests "com.lshlabs.prompthubspring.stats.StatsServiceSection6PerformanceTest"` 통과

### 완료 기준 충족 여부
1. 최근 30일 active_users 집계: 충족
2. recent_activity updatedAt 기준: 충족
3. 테스트 재현/증빙: 충족
4. 응답 계약 유지: 충족
