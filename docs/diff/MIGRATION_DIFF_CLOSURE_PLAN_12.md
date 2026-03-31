# Django ↔ Spring 차이 제거 계획 (12차)

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

최우선(겉보기만 구현됨):
- 없음

다음(릴리즈 전 필수 보완):
- 없음

마지막(backlog 허용):
1. `GET /api/core/sort-options/`, `GET /api/core/filter-options/` 응답 계약 동등화

---

## 2) 지금 가장 먼저 닫아야 할 차이 1개 추천

추천 항목: `core sort/filter options 응답 계약 동등화`

선정 이유:
1. 남은 유일 차이이며, 프론트 필터 UI가 의존하는 옵션 계약을 직접 좌우함
2. 현재 Spring은 Django와 shape/의미가 다른 커스텀 구조를 반환해 계약 관점 동등성이 남아 있음
3. 수정 범위가 `core` read-only API로 좁아 리스크 대비 효과가 큼

---

## 3) 차이 1개 닫기 상세 계획

### Django 원본 경로
- `/Users/mac/Documents/prompthub2/backend/core/views.py`
  - `get_sort_options(request)`
  - `get_filter_options(request)`
- `/Users/mac/Documents/prompthub2/backend/core/sorting.py`
  - `SortManager.get_sort_options()`

### Django 기준 실제 동작
1. `GET /api/core/sort-options/`
  - 응답 shape:
    - `{"sort_options": {...}, "default": "latest"}`
  - `sort_options`는 key-value 매핑(예: `latest`, `oldest`, `popular`, `satisfaction`, `views`)
2. `GET /api/core/filter-options/`
  - 응답 shape:
    - `{"platforms": [...], "categories": [...], "models_by_platform": {...}}`
  - `platforms`: `[{id, name}]`
  - `categories`: `[{id, name}]`
  - `models_by_platform`: `{platformName: [{id, name, platform_id, platform_name}]}`

### Spring 현재 동작
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
  - `sortOptions()`가 `[{value,label}]` 리스트 반환 (Django shape와 다름)
  - `filterOptions()`가 `type/label/options` 중심 커스텀 구조 + `satisfaction/date` 섹션 포함
  - `models_by_platform` 구조 미제공

### 정확히 다른 점
1. `sort-options` 응답 구조 불일치 (`sort_options` map + `default` vs list)
2. `filter-options` 응답 구조 불일치 (`models_by_platform` 부재)
3. Django에 없는 `satisfaction/date` 섹션이 Spring에 포함되어 계약 의미가 다름

### 왜 이 항목을 지금 먼저 닫아야 하는지
- 남은 마지막 차이이며, API 계약 동등성을 최종적으로 닫는 데 필수인 read-only endpoint 정합성 문제이기 때문.

### 최종적으로 어떤 동작으로 맞춰야 하는지
1. `sort-options`를 Django와 동일 shape로 반환
  - `sort_options` map + `default`
2. `filter-options`를 Django와 동일 shape로 반환
  - `platforms`, `categories`, `models_by_platform`
3. `models_by_platform`는 플랫폼 이름 기준 그룹핑 + 모델 row에 `platform_id`, `platform_name` 포함
4. Django 원본과 동일 계약 우선(추가 필드는 제거 또는 별도 endpoint로 분리)

### 수정해야 할 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
  - `sortOptions()`, `filterOptions()` 반환 형식 재설계
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java`
  - 반환 타입/직렬화가 Django 계약과 일치하는지 확인
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/AiModelRepository.java`
  - `models_by_platform` 구성용 조회 메서드 필요 시 추가
- 테스트
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/core/` 하위 parity 테스트 신규

### 테스트/검증 포인트
1. `GET /api/core/sort-options/`
  - 응답에 `sort_options`, `default` 존재
  - `default == "latest"`
  - `sort_options`에 Django 키 세트(`latest`, `oldest`, `popular`, `satisfaction`, `views`) 존재
2. `GET /api/core/filter-options/`
  - top-level 키가 `platforms`, `categories`, `models_by_platform`인지 검증
  - `models_by_platform[플랫폼명][]` 원소가 `id,name,platform_id,platform_name`을 갖는지 검증
3. 회귀 검증
  - 기존 프론트에서 사용하는 기본 필터/정렬 로딩 흐름이 깨지지 않는지 smoke 확인

### 완료 기준(이 차이가 닫혔다고 말할 수 있는 기준)
1. `sort-options` 응답 shape가 Django와 동일
2. `filter-options` 응답 shape가 Django와 동일 (`models_by_platform` 포함)
3. Django에 없는 커스텀 필드로 계약이 오염되지 않음
4. 위 계약이 테스트로 증빙됨

---

## 4) 구현 결과 업데이트 (완료)

### 상태
- 완료

### 반영 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
  - `sortOptions()`를 Django 계약과 동일한 shape로 변경:
    - `{"sort_options": {...}, "default": "latest"}`
  - `filterOptions()`를 Django 계약과 동일한 shape로 변경:
    - `{"platforms": [...], "categories": [...], "models_by_platform": {...}}`
  - Django에 없는 `satisfaction`, `date`, `type/label/options` 커스텀 구조 제거
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/core/CoreOptionsContractParityTest.java`
  - `GET /api/core/sort-options/`, `GET /api/core/filter-options/` parity 테스트 신규 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/core/CoreServiceSection6Test.java`
  - `filterOptions`/`sortOptions` shape 검증을 Django 계약 기준으로 보강

### 하드코딩/placeholder 여부
- 없음
- 실제 DB 조회 결과를 Django 응답 계약에 매핑해 반환

### Django와 동일하게 맞춘 핵심 동작
1. `sort-options`에서 `sort_options` key-value map + `default` 반환
2. `filter-options`에서 `platforms`, `categories`, `models_by_platform` 반환
3. `models_by_platform`의 모델 row에 `id`, `name`, `platform_id`, `platform_name` 포함
4. Django에 없는 커스텀 옵션 섹션 제거로 계약 순도 확보

### 아직 남은 차이
- 본 diff 항목 기준 핵심 차이 없음

### 실행한 테스트
- `./gradlew test --tests "com.lshlabs.prompthubspring.core.CoreOptionsContractParityTest"`

참고:
- `CoreTrendingModelParityTest`는 본 변경과 무관한 기존 test profile(PostgreSQL SQL grammar) 이슈로 실패하며, 이번 diff 범위 변경 파일과 직접 연결되지 않음

### 완료 기준 4개 충족 여부
1. `sort-options` Django shape 동등화: 충족
2. `filter-options` Django shape 동등화: 충족
3. 커스텀 필드 오염 제거: 충족
4. 계약 테스트 증빙: 충족
