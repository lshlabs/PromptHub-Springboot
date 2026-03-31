# Django ↔ Spring 차이 제거 계획 (1차)

## 1) 우선순위 재분류

### 최우선: 겉보기만 구현됨

1. `core` 트렌딩 모델 연관 조회(`/api/core/trending/model/{modelName}/posts/`, `/info/`)
2. `core` 트렌딩 캐시 갱신(`/api/core/trending/refresh-cache/`)

### 다음: 릴리즈 전 필수 보완

1. `users` 비밀번호 변경 시 토큰 회전/세션 안전성 동작 불일치
2. `users` 공개 프로필(`public_profile`) 마스킹 규칙 누락
3. `posts` 생성/수정 도메인 규칙 중 모델 필수/기타 조합 규칙 일부 누락
4. `posts` 만족도 단위(0.5 step) 검증 불일치
5. `stats` 활성 사용자/최근 활동 기준 시점 불일치

### 마지막: backlog 허용

1. 세션 device/browser/os 상세 파싱
2. 모델 suggest 가중치 정렬/slug 기반 정교도
3. `relativeTime` 포맷 동등성
4. 정렬/필터 옵션 응답 shape 세부 동등화

---

## 2) 지금 가장 먼저 수정할 항목 (1개 추천)

**추천 항목:** `core` 트렌딩 모델 연관 조회 동등화  
대상 API:

- `GET /api/core/trending/model/{modelName}/posts/`
- `GET /api/core/trending/model/{modelName}/info/`

선정 이유:

1. 현재 `겉보기만 구현됨` 판정 항목 중 핵심 비즈니스 로직(related_model + exact matching + category 맥락)을 가장 크게 훼손
2. 하드코딩/placeholder 제거 효과가 즉시 큼
3. 트렌딩 페이지의 데이터 신뢰도를 직접 좌우

---

## 3) 차이 1개 닫기 상세 계획

### Django 원본 경로

- `/Users/mac/Documents/prompthub2/backend/core/services/trending_service.py`
- `/Users/mac/Documents/prompthub2/backend/core/models/trending.py`
- `/Users/mac/Documents/prompthub2/backend/core/views.py`

### Django 기준 실제 동작

1. `model_name`으로 활성 `TrendingRanking`을 조회
2. `related_model`이 연결된 경우 해당 모델의 게시글을 기본 필터로 사용
3. `use_exact_matching=True`면 `model_detail_contains` / `model_etc_contains` 조건을 추가 적용
4. `info` 응답은 실제 랭킹 데이터(`provider`, `score`, `rank`, `category`)와 연관 모델 매핑 정보/연관 게시글 수를 반환
5. `posts` 응답은 위 연관 조건으로 추린 게시글을 정렬/페이지네이션해 반환

### Spring 현재 동작

- `/posts/`: `modelName` 키워드 검색으로 대체
- `/info/`: `provider=community`, `rank=0`, `category=weekly` 하드코딩
- `related_model` 기준 필터 및 exact matching 규칙 미적용

### 정확히 다른 점

1. **조회 기준이 다름**: Django는 `TrendingRanking` 매핑 기반, Spring은 문자열 검색 기반
2. **부수효과가 다름**: Django는 랭킹-모델 연결 규칙(`exact matching`)을 반영, Spring은 반영 불가
3. **계약이 다름**: Django `info`는 실제 랭킹 메타를 반환, Spring은 placeholder 값 반환

### 최종적으로 맞춰야 할 동작 (목표)

1. Spring도 `TrendingRankingEntity`를 source of truth로 사용
2. `relatedModel`, `useExactMatching`, `modelDetailContains`, `modelEtcContains`를 이용해 게시글 필터링
3. `/info/`는 DB 랭킹 실데이터 기반으로 반환 (하드코딩 제거)
4. `/posts/`는 연관 게시글 필터 결과 + 정렬/페이지네이션 + `trending_model` 메타를 Django 계약과 동일 의미로 반환

### 수정해야 할 파일

- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java` (필요 시 응답 shape 정렬)
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/TrendingRankingRepository.java` (조회 메서드 보강)
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostRepository.java` (related model + exact 조건 대응 쿼리 추가 필요 시)
- 테스트 추가:
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/.../core/...` (신규)

### 테스트/검증 포인트

1. `info`:
  - 존재 모델: Django와 동일한 `provider/score/rank/category` 의미값 반환
  - 미존재 모델: Django와 동일한 404/에러 계약
2. `posts`:
  - `related_model`만으로 매칭되는 게시글만 반환되는지
  - `use_exact_matching=true`일 때 detail/etc 추가 조건이 실제 적용되는지
  - 페이지네이션 필드(`current_page`, `total_pages`, `total_count`, `has_next`, `has_previous`) 동등성
3. 회귀:
  - 기존 트렌딩 category-rankings 응답 영향 없음

### 완료 기준 (1:1 복제 판정 기준)

아래 모두 충족 시 “이 차이 1개는 닫힘”으로 판정:

1. `/trending/model/{modelName}/info/`가 placeholder 없이 Django와 동일 의미 데이터 반환
2. `/trending/model/{modelName}/posts/`가 키워드 검색이 아니라 랭킹 매핑 + exact 규칙 기반으로 동작
3. 존재/미존재/exact on/off 핵심 케이스 테스트 통과
4. 코드 상 하드코딩된 `community/weekly/rank=0` 제거

