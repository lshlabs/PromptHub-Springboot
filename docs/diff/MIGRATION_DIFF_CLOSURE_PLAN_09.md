# Django ↔ Spring 차이 제거 계획 (9차)

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

최우선(겉보기만 구현됨):
- 없음

다음(릴리즈 전 필수 보완):
- 없음

마지막(backlog 허용):
1. `relativeTime` 포맷 동등성
2. 정렬/필터 옵션 응답 shape 세부 동등화
3. 세션 device/browser/os 상세 파싱

---

## 2) 지금 가장 먼저 닫아야 할 차이 1개 추천

추천 항목: `posts relativeTime 포맷 동등화`

선정 이유:
1. 남은 항목 중 사용자 노출 면적이 가장 큼(게시글 카드/상세 전반)
2. 현재 Spring은 `relativeTime`를 빈 문자열로 반환하여 계약은 있으나 의미가 비어 있음
3. Django는 `posts/utils.py::format_relative_time`로 명확한 규칙을 제공하므로, 1:1 동등화 기준이 가장 선명함

---

## 3) 차이 1개 닫기 상세 계획

### Django 원본 경로
- `/Users/mac/Documents/prompthub2/backend/posts/serializers.py`
  - `PostBaseSerializer.get_relativeTime()`
- `/Users/mac/Documents/prompthub2/backend/posts/utils.py`
  - `format_relative_time(date_obj)`

### Django 기준 실제 동작
1. `relativeTime`는 `created_at` 기준으로 계산
2. 출력 규칙:
  - `< 60초`: `방금 전`
  - `< 1시간`: `N분 전`
  - `< 1일`: `N시간 전`
  - `< 7일`: `N일 전`
  - `< 30일`: `N주 전`
  - `< 365일`: `N개월 전`
  - `>= 365일`: `N년 전`
3. 예외/비정상 입력 fallback 문자열 존재 (`날짜 없음`, `유효하지 않은 날짜`, `날짜 변환 오류`)

### Spring 현재 동작
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `toPost()`에서 `relativeTime`를 항상 `""`로 세팅
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsService.java`
  - 일부 응답에서도 `relativeTime`를 `""`로 세팅

### 정확히 다른 점
1. Django는 계산된 상대 시간을 제공하지만 Spring은 빈 문자열 고정
2. 프론트는 API `relativeTime`이 존재하면 해당 값을 우선 사용하므로, 현재는 의미 없는 값이 노출됨
3. 시간 경과 구간별 표현 규칙(분/시간/일/주/개월/년)이 Spring에 부재

### 왜 이 항목을 지금 먼저 닫아야 하는지
- 남은 backlog 중 계약 필드의 의미 공백이 가장 큰 항목이며, 사용자 체감과 데이터 신뢰도를 동시에 개선할 수 있음.

### 최종적으로 어떤 동작으로 맞춰야 하는지
1. Spring도 Django와 동일한 기준으로 `createdAt` 기반 `relativeTime` 문자열 생성
2. Post 목록/상세에서 `relativeTime`를 빈 문자열이 아닌 계산값으로 반환
3. 가능하면 stats의 post item 매핑에서도 같은 formatter를 재사용해 일관성 확보
4. API response shape은 유지하고 값만 동등화

### 수정해야 할 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `toPost()`의 `relativeTime` 계산 적용
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/` 하위 신규 유틸(권장)
  - 예: `RelativeTimeFormatter.java` (Django 규칙 대응)
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsService.java`
  - post item 매핑 `relativeTime` 동등화(현재 빈 문자열이면 동일 formatter 적용)
- 테스트 파일(신규/보강)
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/` 하위 formatter 테스트
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/` 또는 controller/service 테스트에 응답 필드 검증 추가

### 테스트/검증 포인트
1. 단위 테스트(포맷터):
  - 각 경계값(59초, 60초, 59분, 60분, 23시간, 24시간, 6일, 7일, 29일, 30일, 364일, 365일) 검증
2. 서비스/API 테스트:
  - post list/detail에서 `relativeTime`가 빈 문자열이 아닌 규칙값으로 반환되는지
3. 회귀:
  - 기존 필드 구조(`createdAt`, `relativeTime`, 기타 필드) 변화 없음

### 완료 기준(이 차이가 닫혔다고 말할 수 있는 기준)
1. `PostService.toPost()`에서 `relativeTime` 하드코딩 빈 문자열 제거
2. Django와 동일한 구간 규칙(분/시간/일/주/개월/년)으로 값 생성
3. 경계값 단위 테스트 + list/detail 응답 테스트로 증빙
4. API 계약(shape)은 유지되고 의미 값만 동등화

---

## 4) 구현 결과 업데이트 (완료)

### 상태
- 완료

### 반영 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/RelativeTimeFormatter.java`
  - Django `format_relative_time` 규칙을 Java로 이식한 공통 포맷터 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `toPost()`의 `relativeTime`를 빈 문자열에서 계산값으로 변경
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsService.java`
  - `recent_posts`의 `relativeTime`를 빈 문자열에서 계산값으로 변경
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/RelativeTimeFormatterTest.java`
  - 경계값(초/분/시간/일/주/개월/년) 단위 테스트 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`
  - post list/detail 응답의 `relativeTime` 비어있지 않음 검증 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/stats/StatsServiceSection6Test.java`
  - dashboard `recent_posts`의 `relativeTime` 비어있지 않음 검증 추가

### 하드코딩/placeholder 제거 여부
- 제거 완료
  - `PostService.toPost()`의 `relativeTime=""` 제거
  - `StatsService.toRecentPost()`의 `relativeTime=""` 제거

### Django와 동일하게 맞춘 핵심 동작
1. `createdAt(created_at)` 기준 상대시간 계산
2. 구간 규칙 동등화: `방금 전` → `N분 전` → `N시간 전` → `N일 전` → `N주 전` → `N개월 전` → `N년 전`
3. API 응답 계약(shape)은 유지하고 값만 동등화

### 아직 남은 차이
- 이 항목(`relativeTime 포맷 동등성`) 기준으로는 핵심 차이 없음
- 잔여는 다른 backlog 항목(정렬/필터 shape 세부, 세션 UA 파싱)만 존재

### 실행한 테스트
- `./gradlew test --tests "com.lshlabs.prompthubspring.post.RelativeTimeFormatterTest" --tests "com.lshlabs.prompthubspring.post.PostControllerSection4FlowTest" --tests "com.lshlabs.prompthubspring.stats.StatsServiceSection6Test"`

### 완료 기준 4개 충족 여부
1. `PostService.toPost()` 하드코딩 제거: 충족
2. Django 구간 규칙 반영: 충족
3. 경계값 + list/detail 테스트 증빙: 충족
4. API 계약(shape) 유지: 충족
