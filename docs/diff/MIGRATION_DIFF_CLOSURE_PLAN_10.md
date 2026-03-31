# Django ↔ Spring 차이 제거 계획 (10차)

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

최우선(겉보기만 구현됨):
- 없음

다음(릴리즈 전 필수 보완):
- 없음

마지막(backlog 허용):
1. 세션 `device/browser/os` 상세 파싱
2. 정렬/필터 옵션 응답 shape 세부 동등화

---

## 2) 지금 가장 먼저 닫아야 할 차이 1개 추천

추천 항목: `users 세션 device/browser/os 상세 파싱 동등화`

선정 이유:
1. 현재 Spring은 로그인/Google 로그인 시 세션 메타를 `Unknown`으로 고정 저장해 정보 가치가 거의 없음
2. Django는 실제 `User-Agent`를 파싱해 `device/browser/os`를 저장하므로, 보안/운영 관측 품질이 명확히 다름
3. 수정 범위가 인증 세션 생성 경계로 좁고, 테스트로 재현 가능한 규칙이 분명해 작업 대비 완료 효과가 큼

---

## 3) 차이 1개 닫기 상세 계획

### Django 원본 경로
- `/Users/mac/Documents/prompthub2/backend/users/views.py`
  - `_parse_device_info(request)`
  - `_create_session(request, user)`
- `/Users/mac/Documents/prompthub2/backend/users/serializers.py`
  - `UserSessionSerializer` (`device`, `browser`, `os` 필드 계약)

### Django 기준 실제 동작
1. 로그인/Google 로그인 시 `HTTP_USER_AGENT`를 파싱하여 세션 저장
2. 파싱 규칙:
  - 모바일/태블릿/PC/Bot 여부에 따라 `device` 분기
  - `browser = "{family} {version_string}"`
  - `os = "{family} {version_string}"`
3. user-agent가 없거나 파싱 실패면 `None` 허용(또는 최소 정보)
4. 세션 API 응답에서 `device`, `browser`, `os`가 실제 파싱 결과로 노출

### Spring 현재 동작
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthService.java`
  - `createSession()`에서 아래 값을 고정 저장
    - `device = "Unknown"`
    - `browser = "Unknown"`
    - `os = "Unknown"`
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/UserMapper.java`
  - 세션 직렬화는 있으나 저장값이 고정이라 의미가 비어 있음

### 정확히 다른 점
1. Django는 요청 헤더 기반 실 파싱, Spring은 하드코딩 고정값
2. Django는 사용자 환경 추적이 가능, Spring은 추적/분석 불가
3. API shape는 같아도 도메인 의미(운영/보안 진단 정보)가 다름

### 왜 이 항목을 지금 먼저 닫아야 하는지
- 남은 항목 중 구현 경계가 명확하고, 하드코딩 제거 효과가 직접적이며, 로그인/세션 관리 품질을 즉시 올릴 수 있기 때문.

### 최종적으로 어떤 동작으로 맞춰야 하는지
1. Spring도 로그인/Google 로그인 시 user-agent를 파싱해 `device/browser/os`를 저장
2. Django 분기 규칙(모바일/태블릿/PC/Bot/Unknown)과 문자열 조합 규칙을 최대한 동일 반영
3. 세션 응답 계약은 유지하고 값 의미를 동등화
4. 파싱 불가 시 fallback은 Django와 의미상 일치하도록 처리

### 수정해야 할 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthService.java`
  - `createSession()`에서 고정값 제거, 파싱 결과 반영
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/auth/` 하위 신규 유틸(권장)
  - 예: `UserAgentParser.java` (Django `_parse_device_info` 규칙 대응)
- 필요 시 빌드 설정
  - `/Users/mac/Documents/prompthub-springboot/backend/build.gradle` (UA 파싱 라이브러리 추가가 필요할 경우)
- 테스트
  - `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/auth/` 하위 테스트 신규
  - 로그인/구글로그인 서비스 테스트 또는 통합 테스트에서 세션 필드 검증

### 테스트/검증 포인트
1. 단위 테스트(파서):
  - 모바일 UA → `device=브랜드/패밀리`, browser/os 버전 포함
  - 태블릿 UA, PC UA, Bot UA 각각 분기 확인
  - 빈 UA/null UA fallback 확인
2. 서비스 테스트:
  - `login`, `loginWithGoogle` 호출 후 저장된 `UserSession.device/browser/os` 값 검증
3. API 계약 검증:
  - 세션 조회 응답에 `device/browser/os` 필드가 non-placeholder 값으로 반환되는지 확인
4. 회귀:
  - 기존 세션 키, ip, user_agent 저장/반환 계약 유지

### 완료 기준(이 차이가 닫혔다고 말할 수 있는 기준)
1. `AuthService.createSession()`의 `Unknown` 하드코딩 제거
2. Django와 동등한 UA 파싱 분기 규칙 적용
3. login/google-login 경로에서 저장되는 세션 `device/browser/os`가 테스트로 증빙됨
4. 세션 API 응답 shape 유지 + 값 의미 동등화

---

## 4) 구현 결과 업데이트 (완료)

### 상태
- 완료

### 반영 파일
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/auth/UserAgentParser.java`
  - Django `_parse_device_info`를 기준으로 모바일/태블릿/PC/Bot/Unknown 분기 + browser/os 문자열 생성 규칙 반영
- `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthService.java`
  - `createSession()`에서 `Unknown` 하드코딩 제거
  - `User-Agent` 파싱 결과를 `device/browser/os`에 저장하도록 변경
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/auth/UserAgentParserTest.java`
  - 모바일/PC/Bot/blank UA 케이스 단위 테스트 추가
- `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/com/lshlabs/prompthubspring/auth/AuthServiceSessionParsingTest.java`
  - `login` 경로 세션 필드 저장 검증
  - `loginWithGoogle` 경로 세션 필드 저장 검증
  - UA 누락 시 null fallback 검증

### 하드코딩/placeholder 제거 여부
- 제거 완료
  - `createSession()`의 `device/browser/os = "Unknown"` 고정값 제거

### Django와 동일하게 맞춘 핵심 동작
1. 로그인/Google 로그인 시 `User-Agent`를 기반으로 세션 환경 정보 저장
2. 분기 축 동등화: Mobile / Tablet / PC / Bot / Unknown
3. `browser`, `os`를 family + version 형태 문자열로 저장
4. UA 누락 시 Django의 `None` 동작에 맞춰 null 저장

### 아직 남은 차이
- 본 항목 기준 핵심 차이 없음
- 잔여는 별도 backlog 1건(정렬/필터 옵션 응답 shape 세부 동등화)

### 실행한 테스트
- `./gradlew test --tests "com.lshlabs.prompthubspring.auth.UserAgentParserTest" --tests "com.lshlabs.prompthubspring.auth.AuthServiceSessionParsingTest" --tests "com.lshlabs.prompthubspring.auth.AuthControllerTest"`
- 재검증:
  - `./gradlew test --tests "com.lshlabs.prompthubspring.auth.UserAgentParserTest" --tests "com.lshlabs.prompthubspring.auth.AuthServiceSessionParsingTest"`

### 완료 기준 4개 충족 여부
1. `Unknown` 하드코딩 제거: 충족
2. Django 동등 분기 규칙 적용: 충족
3. login/google-login 세션 필드 테스트 증빙: 충족
4. 세션 응답 shape 유지 + 의미 동등화: 충족
