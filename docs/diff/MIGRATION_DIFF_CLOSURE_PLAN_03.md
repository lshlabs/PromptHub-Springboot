# Django ↔ Spring 차이 제거 계획 (3차)

## 1) 잔여 차이 우선순위 재정리 (차이 #01, #02 완료 제외)

### 최우선: 겉보기만 구현됨
- 현재 없음 (기존 최우선 2건 완료)

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

**추천 항목:** `users` 비밀번호 변경 시 토큰 회전/세션 안전성 동등화

선정 이유:
1. 인증 무효화는 릴리즈 전 필수 보안 요구사항이며 영향 범위가 넓음
2. 현재 Spring은 비밀번호만 바꾸고 기존 토큰을 유지해, Django 대비 보안 의미가 다름
3. 수정 범위가 `user/auth` 경계로 명확해 작업 대비 효과가 큼

---

## 3) 차이 1개 닫기 상세 계획

### Django 원본 경로
- `/Users/mac/Documents/prompthub2/backend/users/views.py` (`PasswordChangeView.post`)
- `/Users/mac/Documents/prompthub2/backend/users/serializers.py` (`PasswordChangeSerializer`)

### Django 기준 실제 동작
1. 현재 비밀번호/새 비밀번호/확인값 검증
2. 비밀번호 변경 저장
3. **트랜잭션 내 기존 토큰 삭제 + 신규 토큰 1개 재발급**
4. 응답에 새 토큰 포함
   - `{ "message": "비밀번호가 성공적으로 변경되었습니다.", "token": "..." }`
5. 토큰 재발급 실패 시 APIException으로 명시적 오류 처리

### Spring 현재 동작
- `/api/auth/profile/password/` (`PATCH`)에서 비밀번호만 변경하고 다음을 수행하지 않음:
  - 기존 access/refresh 토큰 일괄 무효화
  - 새 토큰 재발급
  - 토큰 교체 응답 반환
- 현재 응답: `{ "message": "비밀번호가 변경되었습니다." }`

### 정확히 다른 점
1. **보안 의미 차이**: Django는 비밀번호 변경 직후 기존 토큰이 무효화되지만 Spring은 기존 토큰이 계속 유효
2. **계약 차이**: Django는 토큰 교체 결과를 응답에 제공, Spring은 미제공
3. **세션 안전성 차이**: Spring은 타 디바이스/기존 세션 토큰이 계속 살아 있어 비밀번호 변경 효과가 약함

### 왜 이 항목을 지금 먼저 닫아야 하는지
- 인증 경계의 핵심 차이이며, 릴리즈 전 반드시 맞춰야 하는 항목이다.
- 프론트 UX와도 직결된다(비밀번호 변경 후 즉시 신규 토큰으로 지속 세션 유지).

### 최종적으로 맞춰야 할 동작
1. 비밀번호 변경 성공 시 기존 사용자의 모든 활성 토큰(access/refresh) 즉시 무효화
2. 같은 트랜잭션 경계에서 신규 토큰 세트 재발급
3. 응답은 Django 의미 계약과 호환되게 최소 `message` + 새 `token` 포함
   - Spring JWT 체계 특성상 `refresh`도 함께 반환해 내부 인증 일관성 유지
4. 실패 시 “비밀번호는 변경되었지만 토큰 재발급 실패” 같은 부분 성공 상태를 남기지 않도록 원자성 보장

### 수정해야 할 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/UserService.java`
  - `changePassword`에서 토큰 회전 로직 포함
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthService.java`
  - 토큰 발급 로직 재사용 가능한 메서드 노출(또는 전용 메서드 추가)
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/UserController.java`
  - 응답 계약(`token`, 필요시 `refresh`) 반영
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthTokenRepository.java`
  - 사용자 토큰 일괄 revoke 재사용(기존 메서드 활용/보강)
- 테스트 추가/수정
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/user/UserControllerFlowTest.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/user/UserServiceTest.java`
  - 필요 시 auth 쪽 테스트(`AuthServiceRefreshTest`) 보강

### 테스트/검증 포인트
1. 성공 시 계약
   - `PATCH /api/auth/profile/password/` 응답에 새 `token`(및 `refresh`) 포함
2. 기존 토큰 무효화
   - 비밀번호 변경 전 발급된 access 토큰으로 보호 API 호출 시 401/403
3. 신규 토큰 유효성
   - 변경 응답의 새 access 토큰으로 보호 API 호출 성공
4. 원자성
   - 토큰 저장/회전 실패를 강제로 발생시켰을 때 비밀번호 변경이 롤백되는지 확인
5. 회귀
   - 기존 비밀번호 검증/불일치/확인값 오류 케이스의 400 계약 유지

### 완료 기준 (이 차이가 닫혔다고 말할 수 있는 기준)
1. 비밀번호 변경 시 기존 사용자 토큰(access/refresh)이 모두 무효화됨
2. 응답에 신규 인증 토큰이 포함되어 즉시 재인증 없이 이어서 사용 가능
3. 기존 토큰 실패/신규 토큰 성공이 통합 테스트로 재현됨
4. 비밀번호 변경 + 토큰 회전이 원자적으로 동작(부분 성공 없음)

---

## 4) 구현 결과 업데이트 (완료)

### 상태
- 완료

### 반영 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthService.java`
  - `rotateTokenPair(AppUser user)` 추가 (기존 토큰 revoke + 신규 토큰 페어 발급)
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/UserService.java`
  - `changePassword(...)`에 토큰 회전 로직 반영
  - 응답 계약을 `message + token + refresh`로 확장
  - 메시지를 Django 의미에 맞춰 `비밀번호가 성공적으로 변경되었습니다.`로 정렬
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/user/UserServiceTest.java`
  - `changePassword_rotatesTokensAndReturnsNewPair` 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/user/UserPasswordChangeTokenRotationFlowTest.java` (신규)
  - 비밀번호 변경 시 old/new token 동작을 통합 시나리오로 검증

### 실행 테스트
- `./gradlew test --tests "com.lshlabs.prompthubspring.user.UserServiceTest" --tests "com.lshlabs.prompthubspring.user.UserPasswordChangeTokenRotationFlowTest"` 통과

### 완료 기준 충족 여부
1. 비밀번호 변경 시 기존 사용자 토큰(access/refresh)이 모두 무효화됨: 충족
2. 응답에 신규 인증 토큰이 포함되어 즉시 재인증 없이 이어서 사용 가능: 충족
3. 기존 토큰 실패/신규 토큰 성공이 통합 테스트로 재현됨: 충족
4. 비밀번호 변경 + 토큰 회전 원자성(부분 성공 없음): 충족
