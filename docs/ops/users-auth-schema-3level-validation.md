# Users/Auth 도메인 스키마 검증 (3단계 판정)

## 1) 목적 / 판정 체계

이 문서는 `nullable / PK / FK`를 Django 일치 여부만으로 판단하지 않고,
아키텍처/실기능 타당성까지 포함해 아래 3단계로 판정한다.

- `A`: 일치 + 타당 (유지)
- `B`: 일치하지만 개선 필요 (호환 유지 + 개선 과제)
- `C`: 불일치 + 부적합 (즉시 수정 권고)

## 2) 근거 경로 (source of truth)

- Django:
  - `/Users/mac/Documents/prompthub2/backend/users/models.py`
  - `/Users/mac/Documents/prompthub2/backend/users/migrations/0001_initial.py`
  - `/Users/mac/Documents/prompthub2/backend/users/migrations/0005_usersession.py`
  - `/Users/mac/Documents/prompthub2/backend/users/migrations/0004_usersettings.py`
  - Django 토큰: `rest_framework.authtoken.models.Token` (DRF 기본 모델)
- Spring:
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/AppUser.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/UserSettings.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/UserSession.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthToken.java`
- DB 증빙:
  - `information_schema.columns`
  - `information_schema.table_constraints`
  - `information_schema.referential_constraints`
  - DB: `localhost:5432 / prompthub`

## 3) 테이블별 검증 결과

## 3.1 `users`

- 검증 항목: `users.id (PK)`
  - 현재 DB 상태: PK 존재
  - Django 기준: `BigAutoField(primary_key=True)`
  - Spring 기준: `@Id @GeneratedValue`
  - 기능상 영향: 사용자 식별 안정성 확보
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `users.email nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `EmailField(unique=True)` (필수)
  - Spring 기준: `@Column(nullable=false, unique=true)`
  - 기능상 영향: 이메일 없는 사용자 허용 가능(도메인 위반)
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `users.username nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `AbstractUser.username` (필수/unique)
  - Spring 기준: `@Column(nullable=false, unique=true)`
  - 기능상 영향: 사용자명 없는 사용자 허용 가능(도메인 위반)
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `users.password nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `AbstractUser.password` (필수, unusable password라도 값 존재)
  - Spring 기준: `@Column(nullable=false)`
  - 기능상 영향: 비밀번호 컬럼 NULL 허용은 인증 흐름/검증 일관성 저하
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `users.google_sub unique`
  - 현재 DB 상태: UNIQUE 존재 (`ukfla1a4b51hmn2n4kuw14u210i`), nullable `YES`
  - Django 기준: 해당 필드 없음
  - Spring 기준: `@Column(unique=true)` (선택적)
  - 기능상 영향: SSO 사용자 식별 보조 필드, 선택적 유지 가능
  - 판정: `B`
  - 조치 권고: 유지 (Spring 전용 확장 필드로 문서화)

## 3.2 `user_settings`

- 검증 항목: `user_settings.id (PK)`
  - 현재 DB 상태: PK 존재
  - Django 기준: PK 자동 생성
  - Spring 기준: `@Id @GeneratedValue`
  - 기능상 영향: 설정 식별 안정성
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `user_settings.user_id UNIQUE`
  - 현재 DB 상태: UNIQUE 존재 (`user_settings_user_id_key`)
  - Django 기준: `OneToOneField(user)`
  - Spring 기준: `@OneToOne` + `@JoinColumn(unique=true)`
  - 기능상 영향: 사용자 1:1 설정 보장
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `user_settings.user_id FK -> users.id`
  - 현재 DB 상태: FK 존재, delete rule `CASCADE`
  - Django 기준: `on_delete=CASCADE`
  - Spring 기준: `@OneToOne(optional=false)`
  - 기능상 영향: Django와 동등하게 사용자 삭제 시 설정 row 연쇄 삭제
  - 판정: `A`
  - 조치 권고: 유지

## 3.3 `user_sessions`

- 검증 항목: `user_sessions.id (PK)`
  - 현재 DB 상태: PK 존재
  - Django 기준: PK 자동 생성
  - Spring 기준: `@Id @GeneratedValue`
  - 기능상 영향: 세션 식별 안정성
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `user_sessions.session_key UNIQUE`
  - 현재 DB 상태: UNIQUE 존재 (`idx_user_sessions_key`)
  - Django 기준: `key` unique
  - Spring 기준: `@Column(unique=true)`
  - 기능상 영향: 세션 키 충돌 방지
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `user_sessions.user_id FK -> users.id`
  - 현재 DB 상태: FK 존재, delete rule `CASCADE`
  - Django 기준: `on_delete=CASCADE`
  - Spring 기준: `@ManyToOne(optional=false)`
  - 기능상 영향: Django와 동등하게 사용자 삭제 시 세션 연쇄 삭제
  - 판정: `A`
  - 조치 권고: 유지

## 3.4 `auth_tokens`

- 검증 항목: `auth_tokens.id (PK)`
  - 현재 DB 상태: PK 존재
  - Django 기준: DRF Token PK 자동 생성
  - Spring 기준: `@Id @GeneratedValue`
  - 기능상 영향: 토큰 식별 안정성
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `auth_tokens.token UNIQUE`
  - 현재 DB 상태: UNIQUE 존재 (`idx_auth_token_value`)
  - Django 기준: DRF Token `key` unique
  - Spring 기준: `@Column(unique=true)`
  - 기능상 영향: 동일 토큰 중복 방지
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `auth_tokens.user_id FK -> users.id`
  - 현재 DB 상태: FK 존재, delete rule `CASCADE`
  - Django 기준: DRF Token `on_delete=CASCADE`
  - Spring 기준: `@ManyToOne(optional=false)`
  - 기능상 영향: Django와 동등하게 사용자 삭제 시 토큰 연쇄 삭제
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `auth_tokens.token_type CHECK`
  - 현재 DB 상태: CHECK 존재 (`auth_tokens_token_type_check`)
  - Django 기준: 해당 필드 없음 (DRF Token은 단일 타입)
  - Spring 기준: enum 기반 타입 구분
  - 기능상 영향: Spring 내부 정책 확장 필드
  - 판정: `B`
  - 조치 권고: 유지 (Spring 전용 확장으로 문서화)

## 4) B/C 항목 우선순위

- 즉시(릴리즈 전): `C` 전 항목
- 즉시(릴리즈 전): 없음 (이번 동기화에서 해소)
- 후속(개선 필요): `B` 항목 문서화
  - `users.google_sub` (SSO 확장 필드)
  - `auth_tokens.token_type` (Spring 확장 필드)

## 5) 다음 섹션 재사용 템플릿

- 검증 항목 (`table.column` 또는 제약명)
- 현재 DB 상태
- Django 기준
- Spring 기준
- 기능상 영향
- 판정 등급 (`A/B/C`)
- 조치 권고 (`유지/개선/즉시수정`)
- 근거 경로 (Django + Spring + DB)
