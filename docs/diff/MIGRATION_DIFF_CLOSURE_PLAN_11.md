# Django ↔ Spring 차이 제거 계획 (11차)

## 1) 아직 남아 있는 차이 항목 우선순위 재정리

완료 제외 항목:
- 차이 #01: core 트렌딩 모델 연관 조회 동등화
- 차이 #02: core 트렌딩 캐시 갱신 동등화
- 차이 #03: users 비밀번호 변경 토큰 회전 동등화
- 차이 #04: users public_profile 마스킹 동등화
- 차이 #05: posts 모델 필수/기타 조합 규칙 동등화
- 차이 #06: posts satisfaction 0.5 step 검증 동등화
- 차이 #07: stats 활성 사용자/최근 활동 기준 시점 동등화
- 차이 #08: 모델 suggest 가중치 정렬/slug 기반 정교도 동등화
- 차이 #09: posts relativeTime 포맷 동등화
- 차이 #10: users 세션 device/browser/os 상세 파싱 동등화

최우선(겉보기만 구현됨):
- 없음

다음(릴리즈 전 필수 보완):
- 없음

마지막(backlog 허용):
1. 정렬/필터 옵션 응답 shape 및 `posts list` 쿼리 계약 세부 동등화

---

## 2) 지금 가장 먼저 닫아야 할 차이 1개 추천

추천 항목: `posts list 정렬/필터/검색 쿼리 계약 동등화`

선정 이유:
1. 남은 항목 중 API 계약 불일치 면적이 가장 큼(검색/정렬/필터 파라미터)
2. 프론트의 필터 UX와 결과 일관성에 직접 영향
3. Django source of truth가 명확해 1:1 동등화 기준을 확정하기 쉬움

---

## 3) 차이 1개 닫기 상세 계획

### Django 원본 경로
- `/Users/mac/Documents/prompthub2/backend/posts/services/post_service.py`
  - `build_posts_page(request)`
- `/Users/mac/Documents/prompthub2/backend/core/filters.py`
  - `PostFilter` (`categories`, `platforms`, `models` CSV 필터)
- `/Users/mac/Documents/prompthub2/backend/core/sorting.py`
  - `SortManager.sort_posts()` (`latest|oldest|popular|satisfaction|views`)
- `/Users/mac/Documents/prompthub2/backend/core/search.py`
  - `SearchManager.search_posts()` (`search_type` 분기)
- `/Users/mac/Documents/prompthub2/backend/posts/views.py`
  - `posts_list()` 응답 계약

### Django 기준 실제 동작
1. `GET /api/posts/`에서 아래 쿼리 계약을 지원
  - `search`, `search_type`, `sort_by`, `exclude_id`, `page`, `page_size`
  - `categories`, `platforms`, `models` (CSV)
2. 정렬 옵션
  - `latest`, `oldest`, `popular(like+bookmark)`, `satisfaction(nulls_last)`, `views`
3. 검색 타입 분기
  - `title`, `content`, `author`, `title_content`, `all`
4. CSV 필터 규칙
  - categories/models에서 `기타` 항목은 `_etc` 값 존재 조건으로 분기
5. 응답 구조
  - `status`, `data.results[]`, `data.pagination{current_page,total_pages,total_count,has_next,has_previous}`

### Spring 현재 동작
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostController.java`
  - `GET /api/posts/`는 `search`, `sort`, `page`, `page_size`만 지원
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `listPosts()`는 title contains 기반 검색
  - 정렬 키는 `oldest`, `most_viewed`, `most_liked`, default 최신
  - CSV 다중 필터/`search_type`/`exclude_id`/`popular`/`satisfaction` 정렬 없음

### 정확히 다른 점
1. 파라미터 명/의미 차이 (`sort_by` vs `sort`, `search_type` 부재, CSV 필터 부재)
2. 정렬 옵션 차이 (`popular`, `satisfaction`, `views` 계약 불일치)
3. `기타` category/model의 `_etc` 조건 필터 규칙 누락
4. `exclude_id` 기반 제외 조회 누락

### 왜 이 항목을 지금 먼저 닫아야 하는지
- 남은 유일 항목이며, 리스트 조회 계약은 프론트 사용 빈도가 가장 높은 핵심 API이기 때문.

### 최종적으로 어떤 동작으로 맞춰야 하는지
1. `GET /api/posts/`가 Django와 동일한 쿼리 계약(`sort_by`, `search_type`, `exclude_id`, `categories`, `platforms`, `models`) 지원
2. 정렬 semantics 동등화
  - `popular = like_count + bookmark_count desc, created_at desc`
  - `satisfaction = satisfaction desc nulls_last, created_at desc`
  - `views = view_count desc, created_at desc`
3. 검색 분기 동등화 (`title/content/author/title_content/all`)
4. CSV 필터에서 `기타` 항목의 `_etc` 존재 조건 반영
5. 응답 shape는 현재와 동일하게 유지

### 수정해야 할 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostController.java`
  - `GET /api/posts/` 파라미터 확장 및 Django 계약 alias 지원
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `listPosts()`를 Django 계약 기반 스펙/정렬/검색 분기로 확장
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostRepository.java`
  - 필요한 경우 정렬/집계용 쿼리 또는 Specification 보강
- 테스트
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java` 보강
  - 필요 시 `PostListContractParityTest` 신규 추가

### 테스트/검증 포인트
1. 정렬 동등성
  - `sort_by=popular/satisfaction/views/latest/oldest` 각각 결과 순서 검증
2. 검색 타입 동등성
  - `search_type=title|content|author|title_content|all`별 매칭 필드 검증
3. 필터 동등성
  - `categories/platforms/models` CSV 다중 선택
  - `기타` category/model 선택 시 `_etc` 조건 반영 검증
4. 제외 동작
  - `exclude_id` 지정 시 해당 post 제외 검증
5. 계약 검증
  - pagination shape 및 필드명 유지 확인

### 완료 기준(이 차이가 닫혔다고 말할 수 있는 기준)
1. `/api/posts/`가 Django와 동일 쿼리 계약을 수용
2. 정렬/검색/필터 의미가 Django 동작과 동일
3. `기타 + _etc` 및 `exclude_id` 부수효과까지 테스트로 증빙
4. 응답 shape 변화 없이 계약 동등화 완료

---

## 4) 구현 결과 업데이트 (완료)

### 상태
- 완료

### 반영 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostController.java`
  - `GET /api/posts/`에 Django 계약 쿼리 파라미터 추가
  - `search_type`, `sort_by`, `exclude_id`, `categories`, `platforms`, `models` 지원
  - 기존 `sort` 파라미터는 alias로 유지
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `listPosts()`를 Django `build_posts_page + PostFilter + SortManager + SearchManager` 동작에 맞춰 재구성
  - 검색 분기(`title/content/author/title_content/all`) 반영
  - 정렬 분기(`latest/oldest/popular/satisfaction/views`) 반영
  - CSV 필터(`categories/platforms/models`) 및 `기타 + _etc` 규칙 반영
  - `exclude_id` 제외 조건 반영
  - `page_size` 상한(50) 반영
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`
  - Django 쿼리 계약 parity 검증 테스트(`postsList_supportsDjangoQueryContract_sortSearchFilterExclude`) 추가

### 하드코딩/placeholder 여부
- 없음
- 검색/정렬/필터/제외 모두 실제 DB 조건식으로 수행

### Django와 동일하게 맞춘 핵심 동작
1. `/api/posts/` 쿼리 계약 동등화(`search_type`, `sort_by`, `exclude_id`, CSV 필터)
2. `popular = like_count + bookmark_count`, `satisfaction = nulls_last` 정렬 semantics 동등화
3. `categories/models`에서 `기타` 선택 시 `_etc` 비어있지 않은 행만 매칭하는 규칙 동등화
4. 응답 shape(`status`, `data.results`, `data.pagination`) 유지

### 아직 남은 차이
- 현재 backlog 기준 핵심 차이 없음 (이번 항목 기준)

### 실행한 테스트
- `./gradlew test --tests "com.lshlabs.prompthubspring.post.PostControllerSection4FlowTest"`
- `./gradlew test --tests "com.lshlabs.prompthubspring.post.PostServiceValidationTest" --tests "com.lshlabs.prompthubspring.post.PostUpsertRequestValidationTest"`

### 완료 기준 4개 충족 여부
1. `/api/posts/` Django 쿼리 계약 수용: 충족
2. 정렬/검색/필터 의미 동등화: 충족
3. `기타 + _etc`, `exclude_id` 테스트 증빙: 충족
4. 응답 shape 유지: 충족
