# Django ↔ Spring 차이 제거 계획 (4차)

## 1) 잔여 차이 우선순위 재정리 (완료 항목 제외)

완료 제외 항목:
- 차이 #01: `core` 트렌딩 모델 연관 조회 동등화
- 차이 #02: `core` 트렌딩 캐시 갱신 동등화
- 차이 #03: `users` 비밀번호 변경 토큰 회전 동등화

### 최우선: 겉보기만 구현됨
- 현재 없음

### 다음: 릴리즈 전 필수 보완
1. `users` 공개 프로필(`public_profile`) 마스킹 규칙 누락
2. `posts` 생성/수정 도메인 규칙 중 모델 필수/기타 조합 규칙 일부 누락
3. `posts` 만족도 단위(0.5 step) 검증 불일치
4. `stats` 활성 사용자/최근 활동 기준 시점 불일치

### 마지막: backlog 허용
1. 세션 device/browser/os 상세 파싱
2. 모델 suggest 가중치 정렬/slug 기반 정교도
3. `relativeTime` 포맷 동등성
4. 정렬/필터 옵션 응답 shape 세부 동등화

---

## 2) 지금 가장 먼저 닫아야 할 차이 1개 추천

**추천 항목:** `users` 공개 프로필(`public_profile`) 마스킹 규칙 동등화

선정 이유:
1. 현재는 개인정보 노출 경계가 Django와 달라 보안/정책 리스크가 큼
2. API 계약 불일치가 바로 외부 응답에 나타나며 영향이 즉시 큼
3. 구현 범위가 `summary` 경로에 집중되어 작업 대비 효과가 높음

---

## 3) 차이 1개 닫기 상세 계획

### Django 원본 경로
- `/Users/mac/Documents/prompthub2/backend/users/views.py`
  - `user_summary(request, username)`
- 관련 모델/설정 참조
  - `/Users/mac/Documents/prompthub2/backend/users/models.py` (`UserSettings.public_profile`)

### Django 기준 실제 동작
1. `GET /api/auth/users/{username}/summary/`에서 사용자 조회
2. `user.settings.public_profile` 확인
3. `public_profile == false`이면 `bio`를 `null`로 마스킹
4. 그 외 요약 집계(`post_count`, `total_views`, `total_likes_received`, `total_bookmarks_received`)는 유지
5. 아바타 URL은 상대 경로면 절대 URL로 변환 처리

### Spring 현재 동작
- `UserService.summary(username)`가 `public_profile`을 확인하지 않고 `bio`를 항상 노출
- `UserSettings` 존재 여부/값에 따른 요약 응답 분기 없음
- 결과적으로 Django 정책과 다르게 비공개 설정 사용자의 `bio`가 노출됨

### 정확히 다른 점
1. **정책 차이**: Django는 `public_profile` 기준으로 `bio` 마스킹, Spring은 무조건 노출
2. **개인정보 경계 차이**: 공개 범위 제어가 Spring에서 미적용
3. **계약 차이**: 동일 요청에서 Django와 Spring의 `bio` 필드 값 의미가 다름

### 왜 이 항목을 지금 먼저 닫아야 하는지
- 인증/권한/개인정보 영역의 의미 불일치라 릴리즈 전 필수 보완이다.
- 변경 영향이 summary endpoint에 집중되어 리스크 낮고 검증이 명확하다.

### 최종적으로 맞춰야 할 동작
1. `summary` 응답 생성 시 `UserSettings.public_profile`를 source of truth로 반영
2. `public_profile=false`이면 `bio=null` 반환
3. `public_profile=true` 또는 설정 미존재 시 Django와 동일 기본 정책 유지(`bio` 노출)
4. 기존 집계 필드/응답 구조는 그대로 유지

### 수정해야 할 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/UserService.java`
  - `summary(String username)`에서 `UserSettings` 조회 및 `bio` 마스킹 적용
- 필요 시 보조 수정
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/UserSettingsRepository.java`
    - 사용자별 설정 조회 메서드 점검/보강
- 테스트
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/user/UserControllerFlowTest.java`
    - public_profile true/false 요약 응답 검증 케이스 추가
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/user/UserServiceTest.java`
    - 서비스 단위 마스킹 분기 테스트 추가

### 테스트/검증 포인트
1. `public_profile=false` 사용자의 summary
   - `bio == null`
   - 집계 필드(`post_count`, `total_views`, `total_likes_received`, `total_bookmarks_received`) 정상
2. `public_profile=true` 사용자의 summary
   - `bio` 원문 노출
3. 설정 미존재 사용자
   - Django 기본 동작과 동일하게 `bio` 노출
4. 회귀
   - `summary` 필수 키 구조 유지

### 완료 기준 (이 차이가 닫혔다고 말할 수 있는 기준)
1. `summary`에서 `public_profile=false` 사용자의 `bio`가 null로 마스킹됨
2. `public_profile=true`/미설정 사용자는 기존과 동일하게 `bio`가 노출됨
3. 집계 필드/응답 계약은 손상 없이 유지됨
4. controller/service 테스트로 true/false/미설정 3가지 분기 증빙 완료

---

## 4) 구현 결과 업데이트 (완료)

### 상태
- 완료

### 반영 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/UserService.java`
  - `summary(String username)`에서 `UserSettings.public_profile` 반영
  - `public_profile=false`일 때 `bio=null` 마스킹 적용
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/user/UserServiceTest.java`
  - 서비스 단위 마스킹 분기 테스트 2건 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/user/UserPublicProfileSummaryMaskingFlowTest.java` (신규)
  - 컨트롤러 통합 분기 테스트(공개/비공개/설정 미존재) 추가

### 실행 테스트
- `./gradlew test --tests "com.lshlabs.prompthubspring.user.UserServiceTest" --tests "com.lshlabs.prompthubspring.user.UserPublicProfileSummaryMaskingFlowTest"` 통과

### 완료 기준 충족 여부
1. `public_profile=false` 사용자의 `bio` 마스킹: 충족
2. `public_profile=true`/미설정 사용자의 `bio` 노출: 충족
3. 집계 필드 및 summary 응답 계약 유지: 충족
4. service/controller 레벨에서 3가지 분기 증빙: 충족
