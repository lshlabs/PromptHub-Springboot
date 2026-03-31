# Django ↔ Spring 차이 제거 계획 (2차)

## 1) 잔여 차이 우선순위 재정리 (차이 #01 완료 제외)

### 최우선: 겉보기만 구현됨
1. `core` 트렌딩 캐시 갱신 동등화
   - `GET /api/core/trending/category-rankings/`
   - `POST /api/core/trending/refresh-cache/`

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

## 2) 지금 가장 먼저 닫아야 할 차이 1개 추천

**추천 항목:** `core` 트렌딩 캐시 갱신 동등화

대상 API:
- `GET /api/core/trending/category-rankings/`
- `POST /api/core/trending/refresh-cache/`

선정 이유:
1. 현재는 `from_cache`가 항상 `true`로 고정되어 실제 캐시 hit/miss 의미가 깨져 있음(겉보기 구현)
2. 캐시 삭제 endpoint도 Django 계약(메시지/오류 처리)과 의미가 다름
3. 작업 범위는 작지만, 운영 검증 가능성/관측 신뢰도를 즉시 회복하는 효과가 큼

---

## 3) 차이 1개 닫기 상세 계획

### Django 원본 경로
- `/Users/mac/Documents/prompthub2/backend/core/services/trending_service.py`
- `/Users/mac/Documents/prompthub2/backend/core/views.py`

### Django 기준 실제 동작
1. `get_category_rankings()`
   - 캐시 키(`trending_category_rankings`) 조회
   - 캐시 hit: `{status: success, data: ..., from_cache: true}`
   - 캐시 miss: DB 조회 후 cache set, `{status: success, data: ..., from_cache: false}`
2. `refresh_cache()`
   - 해당 캐시 키 삭제
   - 성공: `{status: success, message: "트렌딩 캐시가 성공적으로 삭제되었습니다."}`
   - 실패: 서비스 예외를 통해 500 + 에러 계약 반환

### Spring 현재 동작
1. `category-rankings`
   - `@Cacheable` 사용은 있으나, Controller에서 `from_cache=true`를 강제로 덮어써 항상 true 반환
2. `refresh-cache`
   - `@CacheEvict(allEntries=true)`로 일괄 제거
   - 성공 메시지가 Django와 다름(`갱신되었습니다`)
   - 실패 분기/오류 계약이 Django 스타일과 다름

### 정확히 다른 점
1. **의미 차이**: Django는 cache hit/miss를 응답으로 노출, Spring은 고정값으로 의미 손실
2. **계약 차이**: refresh 성공 메시지 문구/의미가 다름(삭제 vs 갱신)
3. **검증 가능성 차이**: 현재 Spring 응답으로는 캐시 동작 검증 불가

### 왜 이 항목을 지금 먼저 닫아야 하는지
- 이 항목은 "동작이 있는 것처럼 보이지만 실제 운영 신뢰도 지표가 깨진" 전형적인 겉보기 구현 문제다.
- 수정 난이도 대비 효과가 크고, 이후 성능/트렌딩 검증(릴리즈 게이트)의 기반 신뢰도를 회복한다.

### 최종적으로 맞춰야 할 동작
1. `GET /category-rankings/`가 실제로 `from_cache`를 true/false로 구분해 반환
2. `POST /refresh-cache/`가 Django와 동일 의미 메시지(삭제) 반환
3. 캐시 삭제 후 첫 조회는 반드시 `from_cache=false`가 되도록 동작 보장
4. 캐시 처리 실패 시 Django와 같은 수준의 오류 계약(`status:error`, 메시지)으로 반환

### 수정해야 할 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
  - `@Cacheable` 고정 응답 구조 대신 explicit cache read/write로 `from_cache` 계산
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java`
  - `category-rankings`의 강제 `from_cache=true` 제거
  - `refresh-cache` 성공 메시지를 Django 계약으로 조정
  - 예외시 500 오류 계약 정리(필요 시)
- 테스트 추가/수정
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/core/` 내 신규 테스트

### 테스트/검증 포인트
1. `category-rankings` cache miss/hit
   - 첫 호출: `from_cache=false`
   - 같은 데이터 두 번째 호출: `from_cache=true`
2. `refresh-cache` 이후 재검증
   - refresh 호출 성공
   - 직후 `category-rankings` 호출: `from_cache=false`
3. 계약 검증
   - refresh 성공 메시지: `트렌딩 캐시가 성공적으로 삭제되었습니다.`
   - 실패 경로가 생길 경우 Django와 동일한 에러 응답 구조 유지
4. 회귀
   - category ranking 데이터 shape(`title/subtitle/icon/data`) 영향 없음

### 완료 기준 (이 차이가 닫혔다고 말할 수 있는 기준)
1. `category-rankings`에서 `from_cache`가 실제 hit/miss를 반영
2. `refresh-cache` 성공 메시지가 Django 계약과 동일 의미/문구
3. refresh 직후 재조회 시 miss가 재현됨을 테스트로 증빙
4. 하드코딩된 `from_cache=true` 강제 주입 코드 제거
