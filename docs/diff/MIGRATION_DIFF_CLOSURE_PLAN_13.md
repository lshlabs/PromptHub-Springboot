# Django ↔ Spring 차이 제거 계획 (13차)

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
- 차이 #11: posts list 정렬/필터/검색 쿼리 계약 동등화
- 차이 #12: core sort/filter options 응답 계약 동등화

최우선(겉보기만 구현됨):
- 없음

다음(릴리즈 전 필수 보완):
- 없음

마지막(backlog 허용):
1. `GET /api/core/search/` 쿼리 계약 동등화

---

## 2) 지금 가장 먼저 닫아야 할 차이 1개 추천

추천 항목: `core search 쿼리 계약 동등화`

선정 이유:
1. 남은 유일 차이이며, 검색 페이지의 서버 계약 정합성과 직접 연결됨
2. Django는 `search_type + PostFilter(CSV filters) + sort` 조합을 `core/search`에서 처리
3. 현재 Spring `core/search`는 단일 platform/category 중심 파라미터만 받아 Django 계약 대비 의미 축소가 있음

---

## 3) 차이 1개 닫기 상세 계획

### Django 원본 경로
- `/Users/mac/Documents/prompthub2/backend/core/views.py`
  - `search_posts(request)`
- `/Users/mac/Documents/prompthub2/backend/core/search.py`
  - `SearchManager.search_posts(queryset, query, search_type)`
- `/Users/mac/Documents/prompthub2/backend/core/filters.py`
  - `PostFilter` (`categories/platforms/models` CSV)
- `/Users/mac/Documents/prompthub2/backend/core/sorting.py`
  - `SortManager.sort_posts(queryset, sort_by)`

### Django 기준 실제 동작
1. `GET /api/core/search/`는 다음 계약을 처리
  - `q`, `search_type`, `sort`, `page`, `page_size`
  - `categories`, `platforms`, `models` (CSV 다중 선택)
2. 처리 순서
  - 검색(`SearchManager`) → 필터(`PostFilter`) → 정렬(`SortManager`) → 페이지네이션
3. `search_type` 분기
  - `title`, `content`, `author`, `title_content`, `all`
4. CSV 필터에서 `기타` category/model은 `_etc` 존재 조건 분기

### Spring 현재 동작
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java`
  - `q`, `sort`, `platform`, `category`, `satisfaction_min`, `satisfaction_max` 중심 파라미터
  - `search_type`, `categories/platforms/models` CSV 미지원
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
  - `postService.searchPosts()` 위임 시 단일 platform/category 축으로 처리

### 정확히 다른 점
1. `core/search` 파라미터 계약 불일치 (`search_type`, CSV filters 부재)
2. Django의 다중 선택 필터 의미(`categories/platforms/models`) 미반영
3. Django의 검색 타입 분기 계약이 `core/search` 레벨에서 누락

### 왜 이 항목을 지금 먼저 닫아야 하는지
- 마지막 남은 차이이며, 검색 UX의 계약 동등성을 최종 완료 판정할 때 반드시 필요한 항목이기 때문.

### 최종적으로 어떤 동작으로 맞춰야 하는지
1. Spring `core/search`도 Django와 동일 쿼리 계약 수용
  - `q`, `search_type`, `sort`, `page`, `page_size`, `categories`, `platforms`, `models`
2. 검색/필터/정렬 의미를 Django와 동일하게 적용
3. 기존 단일 `platform`, `category`, `satisfaction_*`는 하위 호환 alias/back-compat로 유지 가능하되, Django 계약 우선 보장
4. 응답 shape는 현재 Spring의 pagination contract 유지(기존 프론트 호환)

### 수정해야 할 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java`
  - `search()` 파라미터를 Django 계약 기반으로 확장
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
  - `search()`에서 Django 계약 파라미터를 `PostService`로 전달
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `core/search` 경로용 동등 검색 함수(또는 기존 함수 확장) 구현
- 테스트
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/core/` 하위 parity 테스트 신규

### 테스트/검증 포인트
1. `GET /api/core/search/`에서 `search_type=title|content|author|title_content|all` 각각 매칭 검증
2. `categories/platforms/models` CSV 다중 필터 검증
3. `기타 + _etc` 필터 부수효과 검증
4. `sort=latest|oldest|popular|satisfaction|views` 정렬 검증
5. pagination shape 유지 확인

### 완료 기준(이 차이가 닫혔다고 말할 수 있는 기준)
1. `core/search`가 Django 쿼리 계약(`search_type + CSV filters`)을 수용
2. 검색/필터/정렬 의미가 Django와 동등
3. `기타 + _etc` 부수효과까지 테스트로 증빙
4. 응답 shape 유지 및 프론트 호환 확인

---

## 4) 구현 결과 업데이트 (완료)

### 상태
- 완료

### 반영 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java`
  - `GET /api/core/search/` 쿼리 계약 확장
  - `search_type`, `categories`, `platforms`, `models`, `page_size` 파라미터 수용
  - 기존 `platform`, `category`, `satisfaction_min`, `satisfaction_max` 하위 호환 유지
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
  - Django 계약 우선 경로 추가 (`search_type + CSV filters`)
  - `postService.listPosts(...)`로 위임하여 Django와 동일 의미 적용
  - `platform/category` 단일 필터는 CSV alias로 병합 처리
  - `satisfaction_*`는 기존 search 경로(back-compat) 유지
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/core/CoreSearchContractParityTest.java`
  - parity 테스트 보강:
    - `search_type=title|content|author|title_content|all`
    - `categories/platforms/models` 단일 + CSV 다중 필터
    - `기타 + _etc` 필터 분기
    - `sort=latest|oldest|popular|satisfaction|views`
    - `platform/category` alias 하위 호환

### 하드코딩/placeholder 제거 여부
- 해당 diff 범위에서 하드코딩/placeholder 없음
- 검색 로직은 실제 DB 조건 기반으로 동작

### Django와 동일하게 맞춘 핵심 동작
1. `core/search`가 Django 쿼리 계약(`q/search_type/sort/page/page_size/categories/platforms/models`)을 수용
2. 검색 타입 분기(`title/content/author/title_content/all`) 의미 반영
3. CSV 필터 및 `기타` category/model의 `_etc` 분기 의미 반영
4. 정렬 옵션(`latest/oldest/popular/satisfaction/views`) 동작 반영

### 아직 남은 차이
- 본 diff 항목 기준 핵심 차이 없음
- `satisfaction_*` 파라미터는 Django 대비 확장(back-compat)으로 남아 있으나, Django 계약을 깨지 않는 추가 호환 축으로 backlog 성격

### 실행한 테스트
- `./gradlew test --tests "com.lshlabs.prompthubspring.core.CoreSearchContractParityTest"`

### 완료 기준 4개 충족 여부
1. `search_type + CSV filters` 계약 수용: 충족
2. 검색/필터/정렬 의미 동등: 충족
3. `기타 + _etc` 테스트 증빙: 충족
4. 응답 shape 유지/프론트 호환: 충족
