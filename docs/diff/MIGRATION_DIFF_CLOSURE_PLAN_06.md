# Django ↔ Spring 차이 제거 계획 (6차)

## 1) 잔여 차이 우선순위 재정리 (완료 항목 제외)

완료 제외 항목:
- 차이 #01: `core` 트렌딩 모델 연관 조회 동등화
- 차이 #02: `core` 트렌딩 캐시 갱신 동등화
- 차이 #03: `users` 비밀번호 변경 토큰 회전 동등화
- 차이 #04: `users` 공개 프로필(`public_profile`) 마스킹 동등화
- 차이 #05: `posts` 생성/수정 모델 필수/기타 조합 규칙 동등화

### 최우선: 겉보기만 구현됨
- 현재 없음

### 다음: 릴리즈 전 필수 보완
1. `posts` 만족도 단위(0.5 step) 검증 불일치
2. `stats` 활성 사용자/최근 활동 기준 시점 불일치

### 마지막: backlog 허용
1. 세션 device/browser/os 상세 파싱
2. 모델 suggest 가중치 정렬/slug 기반 정교도
3. `relativeTime` 포맷 동등성
4. 정렬/필터 옵션 응답 shape 세부 동등화

---

## 2) 지금 가장 먼저 닫아야 할 차이 1개 추천

**추천 항목:** `posts` 만족도(`satisfaction`) 0.5 단위 검증 동등화

선정 이유:
1. 게시글 생성/수정의 핵심 도메인 값 무결성 항목이며 릴리즈 전 필수 보완 범주
2. 현재 Spring은 0.1 단위 입력을 허용/반올림하고 있어 Django와 의미가 다름
3. 수정 범위가 `posts` 내부 검증 계층(요청 DTO + 서비스/엔티티 + 테스트)으로 좁아 작업 대비 효과가 큼

---

## 3) 차이 1개 닫기 상세 계획

### Django 원본 경로
- `/Users/mac/Documents/prompthub2/backend/posts/serializers.py`
  - `PostValidationMixin.validate_satisfaction`
  - `PostEditSerializer.satisfaction` (`min_value=0.5`, `max_value=5.0`)
- `/Users/mac/Documents/prompthub2/backend/posts/models.py`
  - `Post.clean` (`satisfaction*10 % 5 == 0` 강제)
  - `Post.save` (`full_clean()` 호출)

### Django 기준 실제 동작
1. `satisfaction` 범위: `0.5 <= value <= 5.0`
2. 단위: `0.5` 간격만 허용 (`0.5, 1.0, 1.5, ...`)
3. 예: `4.3`, `3.7`, `0.1`은 검증 실패
4. serializer + model clean에서 이중 방어

### Spring 현재 동작
- `PostUpsertRequest`:
  - `@DecimalMin("0.0")`, `@DecimalMax("5.0")` (0.0 허용)
- `PostService.apply(...)`:
  - `setScale(1, HALF_UP)` 처리로 사실상 0.1 단위 허용 및 반올림 저장
- 결과적으로 Django에서 실패해야 하는 값이 Spring에서 통과될 수 있음

### 정확히 다른 점
1. **최소값 차이**: Django는 0.5부터, Spring은 0.0부터 허용
2. **단위 차이**: Django는 0.5 간격 강제, Spring은 0.1 간격 + 반올림
3. **검증 의미 차이**: Django는 입력 엄격 검증, Spring은 느슨한 보정 저장

### 왜 이 항목을 지금 먼저 닫아야 하는지
- 이미 수정한 모델/카테고리 조합 규칙과 같은 `posts` 입력 무결성 축이라 연속적으로 닫는 것이 가장 효율적이며, 사용자 데이터 품질(평균 만족도/랭킹/통계)에 직접 영향을 주기 때문.

### 최종적으로 맞춰야 할 동작
1. Spring도 `satisfaction`을 `0.5~5.0` 범위로 제한
2. `0.5` 단위가 아닌 값은 생성/수정에서 즉시 `400` 반환
3. 반올림으로 허용하지 않고 Django와 동일하게 입력값 그대로 검증
4. 가능하면 DB 제약(체크 제약)까지 보강해 서비스 레이어 우회 저장도 방지

### 수정해야 할 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostUpsertRequest.java`
  - 최소값 `0.5`로 조정
  - 0.5 단위 검증(커스텀 validation 또는 record-level 검증) 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `setScale(1, HALF_UP)` 기반 보정 제거(엄격 검증 기준으로 저장)
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/Post.java` (선택적이지만 권장)
  - `@Check` 제약을 Django 의미에 맞게 강화 (`0.5~5.0`, 0.5 step)
- 테스트
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/PostUpsertRequestValidationTest.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/PostServiceValidationTest.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`

### 테스트/검증 포인트
1. DTO/Bean Validation
  - `0.0`, `0.1`, `4.3` 입력 시 validation 실패
  - `0.5`, `1.0`, `4.5`, `5.0` 입력 시 통과
2. 서비스/컨트롤러
  - 생성/수정 API에서 0.5-step 위반값은 `400` + 명확한 에러 메시지
3. 회귀
  - 기존 정상 생성/수정 시나리오(`4.5`) 유지
4. DB 보호(적용 시)
  - 서비스 우회 저장 시도에서도 동일 제약 유지 확인

### 완료 기준 (이 차이가 닫혔다고 말할 수 있는 기준)
1. `satisfaction` 최소값이 Django와 동일하게 `0.5`로 강제됨
2. 0.5 단위 외 값이 생성/수정에서 모두 거절됨
3. 반올림 보정 없이 입력 검증 실패로 처리됨
4. DTO/서비스/컨트롤러 테스트로 위 규칙이 증빙됨

---

## 4) 구현 결과 업데이트 (완료)

### 상태
- 완료

### 반영 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostUpsertRequest.java`
  - `@DecimalMin`을 `0.0`에서 `0.5`로 변경 (Django 최소값 동등화)
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `satisfaction` 저장 전 0.5-step 검증 로직(`validateSatisfactionHalfStep`) 추가
  - `setScale(1, HALF_UP)` 반올림 저장 제거 (보정 대신 실패 처리)
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/PostUpsertRequestValidationTest.java`
  - `0.0` 입력 실패 검증 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/PostServiceValidationTest.java`
  - `4.3`(0.5-step 위반) 입력 실패 케이스 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`
  - API 레벨에서 `4.3` 입력 시 `400` + `"만족도는 0.5점 단위로 입력해야 합니다."` 검증 추가

### 하드코딩/placeholder 여부
- 없음. 실제 도메인 검증 로직으로 반영됨

### Django와 동일하게 맞춘 핵심 동작
1. `satisfaction` 최소값 `0.5` 강제
2. `0.5` 단위 외 값(`4.3` 등) 생성/수정에서 거절
3. 반올림 보정 저장 제거 (입력값 오류는 그대로 실패)

### 실행 테스트
- `./gradlew test --tests "com.lshlabs.prompthubspring.post.PostUpsertRequestValidationTest" --tests "com.lshlabs.prompthubspring.post.PostServiceValidationTest" --tests "com.lshlabs.prompthubspring.post.PostControllerSection4FlowTest"` 통과

### 완료 기준 충족 여부
1. 최소값 0.5 강제: 충족
2. 0.5 단위 외 값 거절: 충족
3. 반올림 보정 제거: 충족
4. DTO/서비스/컨트롤러 테스트 증빙: 충족
