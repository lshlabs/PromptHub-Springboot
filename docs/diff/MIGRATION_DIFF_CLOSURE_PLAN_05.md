# Django ↔ Spring 차이 제거 계획 (5차)

## 1) 잔여 차이 우선순위 재정리 (완료 항목 제외)

완료 제외 항목:
- 차이 #01: `core` 트렌딩 모델 연관 조회 동등화
- 차이 #02: `core` 트렌딩 캐시 갱신 동등화
- 차이 #03: `users` 비밀번호 변경 토큰 회전 동등화
- 차이 #04: `users` 공개 프로필(`public_profile`) 마스킹 동등화

### 최우선: 겉보기만 구현됨
- 현재 없음

### 다음: 릴리즈 전 필수 보완
1. `posts` 생성/수정 도메인 규칙 중 모델 필수/기타 조합 규칙 일부 누락
2. `posts` 만족도 단위(0.5 step) 검증 불일치
3. `stats` 활성 사용자/최근 활동 기준 시점 불일치

### 마지막: backlog 허용
1. 세션 device/browser/os 상세 파싱
2. 모델 suggest 가중치 정렬/slug 기반 정교도
3. `relativeTime` 포맷 동등성
4. 정렬/필터 옵션 응답 shape 세부 동등화

---

## 2) 지금 가장 먼저 닫아야 할 차이 1개 추천

**추천 항목:** `posts` 생성/수정의 모델 필수/기타 조합 규칙 동등화

선정 이유:
1. 글 생성/수정의 핵심 도메인 무결성이라 릴리즈 전 필수 보완 항목 중 영향이 가장 큼
2. 현재 Spring은 일부 규칙만 있고, Django가 강제하는 핵심 조합 검증(특히 `platform=기타`, `model 미선택+model_etc 미입력`)이 빠져 데이터 정합성이 깨질 수 있음
3. 범위가 `posts` 경계 내부(`apply` + validation tests)로 집중되어 있어 작업 대비 완료 효과가 큼

---

## 3) 차이 1개 닫기 상세 계획

### Django 원본 경로
- `/Users/mac/Documents/prompthub2/backend/posts/serializers.py`
  - `PostValidationMixin.validate_business_logic`
- `/Users/mac/Documents/prompthub2/backend/posts/models.py`
  - `Post.clean`
  - `Post.save` (`full_clean()` 강제)

### Django 기준 실제 동작
1. `platform`이 있으면 `model` 또는 `model_etc` 중 하나는 반드시 있어야 함
   - (`validate_business_logic`: model 미선택 + model_etc 빈값 금지)
2. `platform.name == '기타'`이면:
   - `model`은 `None` 또는 이름이 `'기타'`여야 함
   - `model_etc` 필수
   - `model_detail` 금지
3. `model.name == '기타'`이면:
   - `model_etc` 필수
   - `model_detail` 금지
4. `model`이 없는데 `model_detail` 있으면 금지
5. 위 규칙은 serializer + model `clean()` + `save()->full_clean()`로 다중 방어

### Spring 현재 동작
- `PostService.apply(...)`에서 일부만 검증:
  - `model == null && modelDetail != null` 금지
  - `model == '기타' && modelEtc == null` 강제
  - `model != '기타' && modelEtc != null` 금지
- 누락된 핵심 규칙:
  - `model`도 `model_etc`도 없는 경우 차단 없음
  - `platform == '기타'` 관련 3개 제약(모델 제한 / model_etc 필수 / model_detail 금지) 미구현
- DB 레벨에서도 해당 조합 제약 없음

### 정확히 다른 점
1. **필수 입력 경계 차이**: Django는 플랫폼 선택 시 모델 정보 필수, Spring은 공백 조합 허용
2. **플랫폼-모델 연계 규칙 차이**: Django는 `platform='기타'`일 때 엄격한 조합 강제, Spring은 미강제
3. **다중 방어 차이**: Django는 serializer+model clean, Spring은 service 단일 검증 일부만 적용

### 왜 이 항목을 지금 먼저 닫아야 하는지
- 만족도 0.5 규칙/통계 시점 차이보다 우선순위가 높다. 이유는 생성/수정 입력의 핵심 정합성 붕괴를 즉시 유발하고, 이후 검색/통계/트렌딩 데이터 품질까지 연쇄적으로 오염시키기 때문이다.

### 최종적으로 맞춰야 할 동작
1. Spring도 Django와 동일하게 모델 정보 조합 규칙을 생성/수정 시 동일 강도로 강제
2. 최소 아래 케이스를 모두 `400`으로 차단:
   - `model=null` + `model_etc` 공백
   - `platform='기타'` + `model`이 `'기타'`가 아닌 모델
   - `platform='기타'` + `model_etc` 공백
   - `platform='기타'` + `model_detail` 입력
   - `model='기타'` + `model_detail` 입력
3. 허용 케이스는 Django와 동일하게 통과:
   - `platform='기타'` + `model='기타'` + `model_etc` 유효 + `model_detail` 없음
   - 일반 플랫폼 + 일반 모델 + `model_etc` 없음

### 수정해야 할 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `apply(...)`에 Django `validate_business_logic + clean` 동등 규칙 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/PostServiceValidationTest.java`
  - 실패 케이스(누락 규칙) + 성공 케이스 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`
  - API 레벨에서 400 계약 검증(메시지 포함) 보강

### 테스트/검증 포인트
1. 서비스 단위(도메인 규칙)
   - 누락된 금지 조합 5개가 모두 `ApiException(BAD_REQUEST)`로 실패하는지
   - 허용 조합 2개가 성공하는지
2. 컨트롤러 통합(계약)
   - 생성/수정 엔드포인트에서 동일 조합 입력 시 400 + expected message 반환
3. 회귀
   - 기존 통과 시나리오(일반 모델 생성, category 기타 규칙 등) 유지

### 완료 기준 (이 차이가 닫혔다고 말할 수 있는 기준)
1. Django가 금지하는 모델 조합이 Spring에서도 모두 400으로 차단됨
2. Django가 허용하는 조합이 Spring에서도 동일하게 통과됨
3. 서비스/컨트롤러 테스트로 생성+수정 경로 모두 증빙됨
4. `model 미선택 + model_etc 미입력`과 `platform='기타'` 누락 규칙이 코드상 명시적으로 반영됨

---

## 4) 구현 결과 업데이트 (완료)

### 상태
- 완료

### 반영 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - Django `PostValidationMixin.validate_business_logic` + `Post.clean` 기준으로 모델 조합 검증 로직 보강
  - 추가 반영 규칙:
    - `model` + `model_etc` 동시 미입력 차단
    - `platform='기타'`일 때 모델 제약/`model_etc` 필수/`model_detail` 금지
    - `model='기타'`일 때 `model_detail` 금지
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/PostServiceValidationTest.java`
  - 누락 규칙 실패 케이스 및 허용 케이스 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`
  - API 레벨 실패/성공 계약 테스트 보강
  - 테스트 datasource를 H2로 고정해 환경 의존 없이 재현 가능하도록 정리

### 하드코딩/placeholder 여부
- 해당 차이 항목에서는 하드코딩/placeholder 대체 구현 없음
- 모든 검증은 실제 `PostService.apply(...)` 도메인 로직에서 수행

### Django와 동일하게 맞춘 핵심 동작
1. 플랫폼 선택 시 모델 정보(`model` 또는 `model_etc`) 필수
2. `platform='기타'`에서 비기타 모델 선택 차단, `model_etc` 강제, `model_detail` 금지
3. `model='기타'`에서 `model_detail` 금지
4. 허용 조합(`platform='기타' + model='기타' + model_etc`) 통과

### 실행 테스트
- `./gradlew test --tests "com.lshlabs.prompthubspring.post.PostServiceValidationTest" --tests "com.lshlabs.prompthubspring.post.PostControllerSection4FlowTest"` 통과

### 완료 기준 충족 여부
1. Django 금지 조합 400 차단: 충족
2. Django 허용 조합 통과: 충족
3. 서비스/컨트롤러 테스트 증빙: 충족
4. 누락 규칙 코드 반영: 충족
