# Django ↔ Spring 차이 제거 계획 (8차)

## 1) 잔여 차이 우선순위 재정리 (완료 항목 제외)

완료 제외 항목:
- 차이 #01: core 트렌딩 모델 연관 조회 동등화
- 차이 #02: core 트렌딩 캐시 갱신 동등화
- 차이 #03: users 비밀번호 변경 토큰 회전 동등화
- 차이 #04: users public_profile 마스킹 동등화
- 차이 #05: posts 모델 필수/기타 조합 규칙 동등화
- 차이 #06: posts satisfaction 0.5 step 검증 동등화
- 차이 #07: stats 활성 사용자/최근 활동 기준 시점 동등화

최우선(겉보기만 구현됨):
- 없음

다음(릴리즈 전 필수 보완):
- 없음

마지막(backlog 허용):
1. 모델 suggest 가중치 정렬/slug 기반 정교도
2. relativeTime 포맷 동등성
3. 정렬/필터 옵션 응답 shape 세부 동등화
4. 세션 device/browser/os 상세 파싱

---

## 2) 지금 가장 먼저 닫아야 할 차이 1개 추천

추천 항목: 모델 suggest 가중치 정렬/slug 기반 정교도 동등화

선정 이유:
1. 남은 backlog 중 사용자 체감 영향(자동완성 정확도/순위)이 가장 큼
2. Django는 name/slug/platform(name,slug) 기반 가중치 스코어링인데 Spring은 name contains만 사용 중
3. 수정 범위가 posts 모델 자동완성 경계에 한정되어 리스크 대비 효과가 큼

---

## 3) 차이 1개 닫기 상세 계획

### Django 원본 경로
- `/Users/mac/Documents/prompthub2/backend/posts/services/model_suggest_service.py`
  - `suggest_models()`
  - `compute_score()`

### Django 기준 실제 동작
1. 검색 대상
  - `AiModel.name`, `AiModel.slug`, `Platform.name`, `Platform.slug`
2. 필터 조건
  - 활성 모델 + 활성 플랫폼
  - `platform_id`가 있으면 해당 플랫폼으로 제한
3. 정렬
  - `compute_score()`로 가중치 점수 계산 후 내림차순
  - 동점이면 `sort_order`, `name`으로 정렬
4. 응답
  - `suggestions[].slug` 포함
  - `suggestions[].platform.slug` 포함

### Spring 현재 동작
1. 검색 대상
  - `AiModel.name` contains만 사용
2. 정렬
  - DB 기본 정렬(`sortOrder`, `name`) 중심, Django 스코어링 없음
3. 응답
  - `slug`/`platform.slug` 누락

### 정확히 다른 점
1. 검색 스코프 차이(name only vs name+slug+platform)
2. 랭킹 로직 차이(가중치 스코어 부재)
3. 응답 계약 차이(slug 필드 누락)

### 왜 이 항목을 지금 먼저 닫아야 하는지
- 남은 항목 중 실제 사용자 입력 경험(검색 정확도, 추천 순위)과 직접 연결되는 핵심 품질 차이이기 때문.

### 최종적으로 맞춰야 할 동작
1. Spring도 Django와 동일하게 name/slug/platform name/platform slug를 모두 검색 범위에 포함
2. Django `compute_score`와 동일한 가중치 규칙으로 추천 순서 결정
3. 응답에 `slug` 및 `platform.slug` 포함
4. `platform_id` 필터/limit(10) 동작 유지

### 수정해야 할 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `suggestModels()`를 Django scoring 방식으로 리팩터링
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/AiModelRepository.java`
  - name-only 조회 대신 slug/platform까지 포함 가능한 조회 메서드 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/Platform.java` (필요 시)
  - slug 접근자/매핑 확인
- 테스트
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/` 하위에 suggest 전용 테스트 신규 추가

### 테스트/검증 포인트
1. 검색 범위 검증
  - query가 model slug/platform slug에만 매칭되는 케이스도 추천되는지
2. 랭킹 검증
  - startswith > contains 우선순위가 Django와 동일한지
  - 동점 시 sort_order/name tie-break 적용 여부
3. 응답 계약 검증
  - `suggestions[].slug`, `suggestions[].platform.slug` 포함 여부
4. 회귀 검증
  - 기존 API shape(`status`, `data.query`, `data.total_count`) 유지

### 완료 기준(이 차이가 닫혔다고 말할 수 있는 기준)
1. Django와 동일 검색 스코프(name/slug/platform name/platform slug) 반영
2. Django와 동일 가중치 정렬 규칙 반영
3. 응답에 slug 관련 필드 동등화
4. 테스트로 검색 범위+정렬+응답 계약이 증빙됨

---

## 4) 구현 결과 업데이트 (완료)

### 상태
- 완료

### 반영 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `suggestModels()`를 Django `compute_score`와 동일한 가중치 정렬 방식으로 변경
  - name/slug/platform name/platform slug를 모두 스코어 계산에 반영
  - 응답에 `slug`, `platform.slug` 포함
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/AiModelRepository.java`
  - suggest 후보 조회를 name-only에서 slug/platform까지 포함하는 쿼리로 확장
  - platform 활성 조건 포함
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/AiModel.java`
  - `slug` 필드/접근자 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/Platform.java`
  - `slug` 필드/접근자 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`
  - `/api/posts/models/suggest/`의 scoring/contract(slug 포함) 검증 테스트 추가

### 하드코딩/placeholder 여부
- 없음. 실제 DB 후보 조회 + 실 점수 계산 로직으로 구현

### Django와 동일하게 맞춘 핵심 동작
1. 검색 범위를 `model.name`, `model.slug`, `platform.name`, `platform.slug`로 확장
2. Django 가중치 규칙(`startswith` > `contains`)과 tie-break(`sort_order`, `name`) 반영
3. 응답에 `suggestions[].slug`, `suggestions[].platform.slug` 제공
4. `platform_id` 필터 및 최대 10개 제한 유지

### 실행 테스트
- `./gradlew test --tests "com.lshlabs.prompthubspring.post.PostControllerSection4FlowTest" --tests "com.lshlabs.prompthubspring.post.PostServiceValidationTest" --tests "com.lshlabs.prompthubspring.post.PostUpsertRequestValidationTest"` 통과

### 완료 기준 충족 여부
1. 검색 스코프 동등화: 충족
2. 가중치 정렬 동등화: 충족
3. slug 응답 계약 동등화: 충족
4. 테스트 증빙: 충족
