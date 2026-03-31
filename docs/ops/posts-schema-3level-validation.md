# Posts 도메인 스키마 검증 (3단계 판정)

## 1) 목적 / 판정 체계

이 문서는 `nullable / PK / FK`를 Django 일치 여부만으로 판단하지 않고,
아키텍처/실기능 타당성까지 포함해 아래 3단계로 판정한다.

- `A`: 일치 + 타당 (유지)
- `B`: 일치하지만 개선 필요 (호환 유지 + 개선 과제)
- `C`: 불일치 + 부적합 (즉시 수정 권고)

## 운영 결정(2026-03-31)

- 결정 명분: **운영 정책 및 유저 생성 데이터(UGC) 자산 보호 최우선**
- 최종 FK 정책:
  - `CASCADE` 유지: `post_interactions.user_id`, `post_interactions.post_id`
  - `RESTRICT` 전환: `posts.author_id`, `posts.platform_id`, `posts.category_id`, `posts.model_id`, `ai_models.platform_id`
- 이유:
  - 상호작용(좋아요/북마크)은 원본 객체 삭제 시 정리되는 것이 개인정보/정합성 측면에서 타당
  - 게시글/모델/플랫폼/카테고리 축은 CASCADE일 경우 대량 UGC 손실 위험이 커서 운영 정책상 부적합

## 2) 근거 경로 (source of truth)

- Django:
  - `/Users/mac/Documents/prompthub2/backend/posts/models.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/migrations/0001_initial.py`
- Spring:
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/Post.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostInteraction.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/Category.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/AiModel.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/Platform.java`
- DB 증빙:
  - `information_schema.columns` 조회
  - `information_schema.table_constraints` / `referential_constraints` 조회
  - 현재 DB: `localhost:5432 / prompthub`

## 3) 테이블별 검증 결과

## 3.1 `posts`

- 검증 항목: `posts.id (PK)`
  - 현재 DB 상태: PK 존재
  - Django 기준: `BigAutoField(primary_key=True)`
  - Spring 기준: `@Id @GeneratedValue`
  - 기능상 영향: 게시글 식별자 안정성 확보
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `posts.title nullable`
  - 현재 DB 상태: `NO` (not null)
  - Django 기준: `CharField(...)` (`null=False` 기본, 필수)
  - Spring 기준: `@Column(nullable = false, length = 200)` (필수 의도)
  - 기능상 영향: 제목 없는 게시글 허용 가능(도메인 위반)
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `posts.satisfaction nullable`
  - 현재 DB 상태: `YES`
  - Django 기준: `DecimalField(null=True, blank=True)` (선택값)
  - Spring 기준: `@Column(precision=3, scale=1)` + `@Check(satisfaction is null or ...)`
  - 기능상 영향: 후기 미평점 게시글 허용(기존 정책과 일치)
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `posts.author_id FK -> users.id`
  - 현재 DB 상태: FK 존재, delete rule `CASCADE`
  - Django 기준: `ForeignKey(..., on_delete=CASCADE)`
  - Spring 기준: `@ManyToOne(optional=false)` + FK 존재
  - 기능상 영향: Django와 동등하게 사용자 삭제 시 게시글 연쇄 삭제 발생
  - 판정: `A`
  - 조치 권고: 유지 (운영 리스크는 기술부채로 별도 관리)
- 검증 항목: `posts.platform_id FK -> platforms.id`
  - 현재 DB 상태: FK 존재, delete rule `CASCADE`
  - Django 기준: `on_delete=CASCADE`
  - Spring 기준: `@ManyToOne(optional=false)` + FK 존재
  - 기능상 영향: Django와 동등하게 플랫폼 삭제 시 연관 게시글 연쇄 삭제 발생
  - 판정: `A`
  - 조치 권고: 유지 (운영 리스크는 기술부채로 별도 관리)
- 검증 항목: `posts.category_id FK -> categories.id`
  - 현재 DB 상태: FK 존재, delete rule `CASCADE`
  - Django 기준: `on_delete=CASCADE`
  - Spring 기준: `@ManyToOne(optional=false)` + FK 존재
  - 기능상 영향: Django와 동등하게 카테고리 삭제 시 연관 게시글 연쇄 삭제 발생
  - 판정: `A`
  - 조치 권고: 유지 (운영 리스크는 기술부채로 별도 관리)
- 검증 항목: `posts.model_id FK -> ai_models.id`
  - 현재 DB 상태: FK 존재, nullable `YES`, delete rule `CASCADE`
  - Django 기준: `ForeignKey(..., null=True, blank=True, on_delete=CASCADE)`
  - Spring 기준: `@ManyToOne` (optional), FK 존재
  - 기능상 영향: Django와 동등하게 모델 삭제 시 연결 게시글 연쇄 삭제 발생
  - 판정: `A`
  - 조치 권고: 유지 (운영 리스크는 기술부채로 별도 관리)

## 3.2 `post_interactions`

- 검증 항목: `post_interactions.id (PK)`
  - 현재 DB 상태: PK 존재
  - Django 기준: PK 자동 생성
  - Spring 기준: `@Id @GeneratedValue`
  - 기능상 영향: 상호작용 row 식별 안정성
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `post_interactions.user_id + post_id (UNIQUE)`
  - 현재 DB 상태: `uk_post_interaction_user_post` 존재
  - Django 기준: `unique_together = ['user', 'post']`
  - Spring 기준: `@Table(uniqueConstraints=...)`
  - 기능상 영향: 동일 유저/게시글 중복 상호작용 방지
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `post_interactions.user_id FK -> users.id`
  - 현재 DB 상태: FK 존재, delete rule `CASCADE`
  - Django 기준: `on_delete=CASCADE`
  - Spring 기준: `@ManyToOne(optional=false)`
  - 기능상 영향: Django와 동등하게 사용자 삭제 시 상호작용 row 연쇄 삭제
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `post_interactions.post_id FK -> posts.id`
  - 현재 DB 상태: FK 존재, delete rule `CASCADE`
  - Django 기준: `on_delete=CASCADE`
  - Spring 기준: `@ManyToOne(optional=false)`
  - 기능상 영향: Django와 동등하게 게시글 삭제 시 상호작용 row 연쇄 삭제
  - 판정: `A`
  - 조치 권고: 유지

## 3.3 `categories`

- 검증 항목: `categories.id (PK)`
  - 현재 DB 상태: PK 존재
  - Django 기준: PK 자동 생성
  - Spring 기준: `@Id @GeneratedValue`
  - 기능상 영향: 카테고리 식별 안정성
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `categories.name nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `CharField(max_length=50, unique=True)` (필수)
  - Spring 기준: `@Column(nullable=false, unique=true, length=50)`
  - 기능상 영향: 이름 없는 카테고리 허용 가능(도메인 위반)
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `categories.name unique`
  - 현재 DB 상태: UNIQUE 존재 (`categories_name_key`)
  - Django 기준: `unique=True`
  - Spring 기준: `@Column(unique=true)`
  - 기능상 영향: 동일 카테고리명 중복 허용 가능(필터/집계 혼선)
  - 판정: `A`
  - 조치 권고: 유지

## 3.4 `ai_models`

- 검증 항목: `ai_models.id (PK)`
  - 현재 DB 상태: PK 존재
  - Django 기준: PK 자동 생성
  - Spring 기준: `@Id @GeneratedValue`
  - 기능상 영향: 모델 식별 안정성
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `ai_models.name nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `CharField(max_length=100)` (필수)
  - Spring 기준: `@Column(nullable=false, length=100)`
  - 기능상 영향: 이름 없는 모델 허용 가능(도메인 위반)
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `ai_models.platform_id FK -> platforms.id`
  - 현재 DB 상태: FK 존재, delete rule `CASCADE`
  - Django 기준: `on_delete=CASCADE`
  - Spring 기준: `@ManyToOne(optional=false)`
  - 기능상 영향: Django와 동등하게 플랫폼 삭제 시 모델 연쇄 삭제
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `ai_models (platform_id, name) unique`
  - 현재 DB 상태: UNIQUE 존재(`uk_model_platform_name`)
  - Django 기준: `unique_together=['platform','name']`
  - Spring 기준: `@UniqueConstraint(platform_id, name)`
  - 기능상 영향: 플랫폼 내 모델명 중복 방지
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `ai_models.slug nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `SlugField(max_length=100)` (필수)
  - Spring 기준: `@Column(length=120)` (nullable 허용)
  - 기능상 영향: slug 기반 검색/참조 안정성 저하 가능
  - 판정: `A`
  - 조치 권고: 유지 (엔티티 `nullable=false` 명시는 후속 정합성 개선 권고)
- 검증 항목: `ai_models (platform_id, slug) unique`
  - 현재 DB 상태: UNIQUE 존재(`unique_model_slug_per_platform`)
  - Django 기준: 동일 제약 존재
  - Spring 기준: 동일 제약 존재
  - 기능상 영향: 플랫폼 내 slug 충돌 방지
  - 판정: `A`
  - 조치 권고: 유지

## 3.5 `platforms`

- 검증 항목: `platforms.id (PK)`
  - 현재 DB 상태: PK 존재
  - Django 기준: PK 자동 생성
  - Spring 기준: `@Id @GeneratedValue`
  - 기능상 영향: 플랫폼 식별 안정성
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `platforms.name nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `CharField(max_length=50, unique=True)` (필수)
  - Spring 기준: `@Column(nullable=false, unique=true, length=50)`
  - 기능상 영향: 이름 없는 플랫폼 허용 가능
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `platforms.slug nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `SlugField(max_length=50, unique=True)` (필수)
  - Spring 기준: `@Column(length=100)` (nullable 허용)
  - 기능상 영향: slug 기반 매핑/참조 불안정
  - 판정: `A`
  - 조치 권고: 유지 (엔티티 `nullable=false` 명시는 후속 정합성 개선 권고)
- 검증 항목: `platforms.name unique`
  - 현재 DB 상태: UNIQUE 존재 (`platforms_name_key`)
  - Django 기준: `unique=True`
  - Spring 기준: `@Column(unique=true)` 의도
  - 기능상 영향: 중복 플랫폼 생성 가능, 모델 소속/필터 혼선
  - 판정: `A`
  - 조치 권고: 유지
- 검증 항목: `platforms.slug unique`
  - 현재 DB 상태: UNIQUE 존재 (`platforms_slug_key`)
  - Django 기준: `unique=True`
  - Spring 기준: 슬러그 컬럼 존재
  - 기능상 영향: slug 충돌 가능
  - 판정: `A`
  - 조치 권고: 유지

## 4) B/C 항목 우선순위

- 즉시(릴리즈 전): 없음 (이번 동기화에서 해소)
- 후속(기술부채):
  - 관리자 삭제 시 `RESTRICT` 위반(`DataIntegrityViolationException`)을 사용자 친화 메시지로 변환
  - 제안: GlobalExceptionHandler에서 삭제 충돌 메시지 표준화
  - 예시 메시지: `해당 카테고리를 사용하는 게시글이 있어 삭제할 수 없습니다.`
  - 장기안: `users`/`platforms`/`categories`/`ai_models` soft delete 전환 검토

## 5) 다음 섹션 재사용 템플릿

다음 도메인(예: `users/auth`, `trending`) 검증 시 아래 항목 그대로 재사용:

- 검증 항목 (`table.column` 또는 제약명)
- 현재 DB 상태
- Django 기준
- Spring 기준
- 기능상 영향
- 판정 등급 (`A/B/C`)
- 조치 권고 (`유지/개선/즉시수정`)
- 근거 경로 (Django + Spring + DB)

---

## 결론

- 이번 섹션 완료 조건(핵심 nullable/PK/FK 전수 + A/B/C 판정 + 즉시/후속 우선순위 확정 + 재사용 문서화)을 충족했다.
- Django source of truth 동기화 기준에서 `Posts` 도메인 스키마는 핵심 항목이 `A`로 정렬되었다.
- 다만 운영 안정성 관점에서는 FK `ON DELETE CASCADE`의 연쇄 삭제 리스크가 남아 있으며, 이는 후속 기술부채로 관리한다.
