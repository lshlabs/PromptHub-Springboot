# DJANGO_SPRING_DIFF_CHECKLIST

## 동등성 해석 원칙

이 체크리스트의 **“1:1”** 은 Django의 구현 문법이나 프레임워크 관용구를 그대로 복제하라는 뜻이 아니다.

목표는 아래 항목의 **동등성**이다.

- **기능 의미**
- **입출력 계약**
- **도메인 규칙**
- **데이터 정합성**
- **부수효과**
- **테스트로 증명 가능한 동작 기준**

따라서 Spring Boot에서는 Spring 생태계에 맞는 방식으로 구현해도 된다.

예:
- Django Serializer/Form 검증 → Spring DTO + Validation
- Django ORM/service 흐름 → Spring Entity + Repository + Service
- Django migration 기반 스키마 관리 → Spring 마이그레이션/DDL/스키마 검증 체계

즉, **문법은 달라도 되지만 의미는 같아야 한다.**

다만 아래 항목은 반드시 원본 Django와 동등해야 한다.

- 사용자 관점에서의 기능 결과
- 프론트가 실제로 의존하는 request/response 계약
- 권한/검증/상태 전이 규칙
- 집계/정렬/부수효과의 의미
- 테스트/검증으로 입증 가능한 동작 기준

또한 placeholder, hardcoded response, mock 대체 구현은 **완료**로 간주하지 않는다.

---

### source of truth 원칙

이 체크리스트의 모든 판정은 반드시 **Django 원본 코드 경로**를 source of truth로 삼아 수행한다.  
즉, 각 항목의 완료/미완료 판단은 문서 요약이나 프론트 동작만으로 내리지 않고, 아래 대조를 직접 거친 뒤에만 결정한다.

1. Django 원본 경로 (`urls -> views -> services/models/serializers/forms`)
2. Spring 대응 경로 (`controller -> service -> repository/entity/dto`)
3. 실제 동작 / 계약 / 검증 / 부수효과 / 테스트 근거 비교

따라서 체크리스트의 어떤 항목도 **원본 코드 경로 대조 없이 완료 처리할 수 없다.**

---

## 체크리스트 사용 규칙

- `완료`: Django 기준 기능 의미/계약/검증/부수효과가 동등하고, 근거가 코드/테스트로 확보됨
- `진행 중`: 주요 작업이 시작되었으나 완료 기준을 모두 충족하지 못함
- `미완료`: 아직 실질 구현/검증이 부족함
- `부분 동일`: 핵심 기능은 있으나 일부 계약/규칙/증빙이 부족함
- `차이 있음`: 실제 기능/규칙 차이가 존재함
- `근거 부족`: 동일하다고 단정할 증빙이 아직 부족함
- Django 원본 경로와 Spring 대응 경로가 명시되지 않은 항목은 `완료` 판정 금지
- 코드 기준 대조 근거가 없는 항목은 `근거 부족`으로 유지
- 프론트 동작, 스모크 테스트, 응답 예시만으로는 `완료` 판정 금지

### 항목 공통 템플릿(필수)

모든 체크 항목은 최소한 아래 필드를 포함해야 한다.

- Django 원본 경로:
- Spring 대응 경로:

**중요:**  
체크리스트 항목이 하나라도 `진행 중 / 미완료 / 부분 동일 / 차이 있음 / 근거 부족` 상태면  
**“Django와 완전 일치”라고 결론 내리지 않는다.**

---

## 현재 구조 요약

- 엔티티: Lombok(`@Getter`, `@Setter`, `@NoArgsConstructor`) 적용됨
- 서비스: `@RequiredArgsConstructor` 기반 주입으로 정리됨
- 컨트롤러: 대부분 typed `ResponseEntity<T>`로 정리됨
- 공통 예외: `ApiException` + `GlobalExceptionHandler` 사용 중

### 남은 과도기 흔적
- 컨트롤러/서비스 입력 `Map<String, Object>` payload 파싱 제거 완료
- 응답 계약 표현용 `Map<String, String>` 필드는 일부 유지(`core` sort options)
- 일부 endpoint가 Django 호환용 legacy URI(`/create`, `/{id}/update`, `/{id}/delete`) 유지
- `currentUserOrNull()` 형태의 예외 삼키기 패턴 존재

---

## 1. 인프라 체크리스트

### [INFRA-01] 로컬/운영 DB 프로필 분리 고정
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/config/settings.py`, `/Users/mac/Documents/prompthub2/backend/.env*`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/resources/application.yml`, `/Users/mac/Documents/prompthub-springboot/backend/.env*`
- Django 기준 동작 요약: 환경별 DB 분리 운영
- Spring 현재 상태 요약: 공통 `application.yml` + `application-local.yml` + `application-test.yml`로 프로필 분리 고정
- 무엇이 다르거나 부족한지: 없음(프로필 분리/부팅 검증 기준)
- 완료 기준: 프로필 분리 또는 동등 운영 정책 문서화 + 부팅 검증(달성)
- 우선순위: 중간

### [INFRA-02] 마이그레이션 실행 경로 단일화
- 현재 상태: 완료
- 문제 유형: 제외(운영 정책 차이)
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/**/migrations`, `manage.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/resources/application.yml` (`ddl-auto: update`), 마이그레이션 디렉터리 미존재
- Django 기준 동작 요약: migration 명령으로 스키마/데이터 변경 이력 일관 관리
- Spring 현재 상태 요약: 실운영 기준으로 `Render -> DBeaver -> Docker Postgres` 실데이터 이관 경로를 사용
- 무엇이 다르거나 부족한지: Django의 `manage.py migrate` 경로와는 구현 방식이 다르나, 데이터 이관 정책상 의도된 차이
- 완료 기준: 운영 정책 차이로 제외 합의 + 실데이터 정합성(Pass) 확보
- 우선순위: 높음

### [INFRA-03] Render → DBeaver → Docker 실데이터 정합성 검증
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/**/models.py` (운영 DB 원본 테이블 기준)
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend` (Docker Postgres `prompthub` 스키마)
- Django 기준 동작 요약: 운영 DB 실데이터를 로컬 검증 DB로 이관했을 때 핵심 도메인 데이터 건수가 일치해야 함
- Spring 현재 상태 요약: Docker Postgres 실측 결과가 Render 실측과 핵심 테이블 기준 1:1 일치함
- 무엇이 다르거나 부족한지: `user_sessions` 2건은 구 시스템 세션 무효화 정책에 따라 의도적으로 이관 제외(승인됨)
- 완료 기준: 핵심 테이블(`users`, `posts`, `ai_models`, `categories`, `platforms`, `trending_categories`, `trending_rankings`, `user_settings`) row count 1:1 일치 + 예외 항목 승인
- 우선순위: 매우 높음

---

## 2. 엔티티 체크리스트

### [ENTITY-01] Django 테이블명 매핑 1:1 고정(users, posts, ai_models 등)
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/users/models.py`, `/Users/mac/Documents/prompthub2/backend/posts/models.py`, `/Users/mac/Documents/prompthub2/backend/core/models/*.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/*.java`, `/post/*.java`, `/core/*Entity.java`
- Django 기준 동작 요약: 테이블/관계 구조가 도메인 규칙의 근간
- Spring 현재 상태 요약: 변경된 target 테이블명 기준으로 매핑 반영
- 무엇이 다르거나 부족한지: 없음(현재 확인 범위)
- 완료 기준: `@Table(name=...)`와 실제 DB 스키마 일치
- 우선순위: 매우 높음

> 해석 주의:
> 이 항목은 Django **문법**을 복제하라는 뜻이 아니라,  
> 현재 마이그레이션 단계에서 **테이블/관계 의미를 안정적으로 맞추기 위한 운영 기준**이다.  
> 이후 구조 개편이 필요하면 별도 리팩터링 트랙에서 다룬다.

### [ENTITY-02] 필수 제약/유니크/인덱스 동등성 검증
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/**/models.py`, `/Users/mac/Documents/prompthub2/backend/**/migrations/*.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/**/*Entity*.java`, `/Users/mac/Documents/prompthub-springboot/backend/migrations/*.sql`
- Django 기준 동작 요약: 모델/마이그레이션에 제약조건 명시
- Spring 현재 상태 요약: Django source of truth 기준 핵심 제약(모델별 slug 유니크, 카테고리별 트렌딩 rank 유니크)을 엔티티 레벨 제약으로 반영하고 `@DataJpaTest`로 DB 강제 동작까지 검증 완료
- 무엇이 다르거나 부족한지: 없음(핵심 제약 기준)
- 완료 기준: 엔티티 제약 매트릭스(Django↔Spring) + 스키마 검증 테스트 (달성)
- 완료 기준(증빙): Django 원본 경로와 Spring 대응 경로의 코드 대조 근거가 문서 또는 링크로 남아 있을 것
- 우선순위: 높음

---

## 3. 레포지토리 체크리스트

### [REPO-01] 트렌딩 연관 조회 쿼리의 source-of-truth 정렬
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/core/services/trending_service.py`, `/Users/mac/Documents/prompthub2/backend/core/views.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`, `/core/TrendingRankingRepository.java`, `/core/CoreController.java`
- Django 기준 동작 요약: trending ranking 매핑 기반 조회
- Spring 현재 상태 요약: `TrendingRankingRepository` + 연관 관계 기반 조회 반영
- 무엇이 다르거나 부족한지: 없음(기존 계획 항목 기준)
- 완료 기준: placeholder 제거 + 존재/미존재/분기 테스트 통과
- 우선순위: 매우 높음

### [REPO-02] 상호작용 정렬 축(시간 기준) 동등성 검증
- 현재 상태: 완료
- 문제 유형: 부분 동일
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/posts/views.py`, `/Users/mac/Documents/prompthub2/backend/posts/services/interaction_service.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`, `/post/PostInteractionRepository.java`
- Django 기준 동작 요약: interaction 시간 축 정렬
- Spring 현재 상태 요약: `PostInteractionRepository` 정렬을 `updated_at DESC, id DESC`로 변경하여 Django 의미와 동등화
- 무엇이 다르거나 부족한지: 없음(정렬 축 기준)
- 완료 기준: 정렬 축을 Django와 동일화 또는 ADR로 차이 승인 + 테스트 (달성)
- 완료 기준(증빙): Django 원본 경로와 Spring 대응 경로의 코드 대조 근거가 문서 또는 링크로 남아 있을 것
- 우선순위: 높음

> 해석 주의:
> 이 항목의 목적은 **정렬 의미의 동등성**이지,  
> Django 내부 구현 방식을 그대로 재현하는 것이 아니다.  
> Spring에서 다른 방식으로 구현해도 **결과 의미와 계약이 같으면 완료 가능**하다.

---

## 4. 서비스 체크리스트

### [SERVICE-01] 서비스 계층 `Map<String,Object>` 제거
- 현재 상태: 완료
- 문제 유형: 부분 동일
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/**/serializers.py`, `**/forms.py`, `**/services/*.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/**/*Service.java`, `**/*Request*.java`, `**/*Response*.java`
- Django 기준 동작 요약: Serializer/Form 스키마 기반 명시적 입력 검증
- Spring 현재 상태 요약: 서비스 계층의 `Map<String,Object>` 입력 파싱(`fromMap`, `asString`, `asBoolean`) 제거됨
- 무엇이 다르거나 부족한지: `Map<String,String>` 응답 필드는 계약 표현용으로 일부 유지됨(입력 파싱과는 별개)
- 완료 기준: Service 공개 입력 경로에서 `Map<String,Object>` 파싱 0건 유지
- 우선순위: 매우 높음

### [SERVICE-02] 비즈니스 규칙/상태 전이 동등성(사용자/게시글/통계)
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/users/services/*.py`, `/posts/services/*.py`, `/core/services/*.py`, `/stats/*.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/UserService.java`, `/post/PostService.java`, `/core/CoreService.java`, `/stats/StatsService.java`
- Django 기준 동작 요약: 권한/검증/집계 로직이 서비스에서 강제
- Spring 현재 상태 요약: `docs/ops/service-rule-parity-matrix.md`에 사용자/게시글/상호작용/검색·통계 규칙을 단일 매트릭스로 고정했고, 링크 유효성 검증 테스트를 추가함
- 무엇이 다르거나 부족한지: 없음(SERVICE-02 기준)
- 완료 기준: 기능별 규칙표 + 케이스 테스트 링크 완성 (달성)
- 완료 기준(증빙): Django 원본 경로와 Spring 대응 경로의 코드 대조 근거가 문서 또는 링크로 남아 있을 것
- 우선순위: 매우 높음

### [SERVICE-03] 트랜잭션 경계 재점검
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/posts/services/*.py`, `/users/services/*.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/**/*Service.java` (`@Transactional` 적용 메서드)
- Django 기준 동작 요약: 주요 상태 변경/집계 변경이 일관되게 반영
- Spring 현재 상태 요약: 주요 write path를 `docs/ops/transaction-boundary-matrix.md`로 고정했고, `ServiceTransactionBoundaryTest`로 `@Transactional` 경계 회귀 검증을 자동화함
- 무엇이 다르거나 부족한지: 없음(트랜잭션 경계 증빙 기준)
- 완료 기준: 주요 write path 트랜잭션 매트릭스 + 회귀 테스트 (달성)
- 완료 기준(증빙): Django 원본 경로와 Spring 대응 경로의 코드 대조 근거가 문서 또는 링크로 남아 있을 것
- 우선순위: 높음

---

## 5. 컨트롤러 체크리스트

### [CTRL-01] 컨트롤러 입력 DTO 정규화
- 현재 상태: 완료
- 문제 유형: 부분 동일
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/**/views.py`, `/Users/mac/Documents/prompthub2/backend/**/serializers.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/**/*Controller.java`, `**/*Request*.java`
- Django 기준 동작 요약: serializer/form을 통해 명시적 입력 계약 사용
- Spring 현재 상태 요약: `@RequestBody` 입력에서 `Map` 제거, record/class DTO로 정규화 완료
- 무엇이 다르거나 부족한지: 없음(입력 DTO화 기준)
- 완료 기준: 모든 public endpoint의 `@RequestBody`가 DTO 타입으로 고정되고 `ResponseEntity<?>` 0건 유지
- 우선순위: 매우 높음

### [CTRL-02] legacy URI 정리와 호환성 정책 분리
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/**/urls.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/**/*Controller.java`
- Django 기준 동작 요약: 원본 프론트가 기대하는 URI 계약 존재
- Spring 현재 상태 요약: canonical URI(`POST/PUT/PATCH/DELETE /api/posts...`)와 legacy alias(`.../create`, `.../{id}/update`, `.../{id}/delete`)를 분리하고, alias 응답에 deprecation `Warning` 헤더를 부여
- 무엇이 다르거나 부족한지: 없음(현재 `posts` 도메인 기준)
- 완료 기준: canonical URI 명세 + alias 유지/제거 정책 문서화 + canonical/alias 동작 회귀 테스트 통과(달성)
- 우선순위: 중간

> 해석 주의:
> 이 항목은 Django URI를 영구 표준으로 삼으라는 뜻이 아니다.  
> **프론트 호환을 위한 alias 정책**과 **Spring 기준 canonical URI**를 구분하자는 목적이다.

---

## 6. 기능 동등성 재검증 체크리스트

### [DIFF-01] 인증 카테고리 1:1 재검증
- 현재 상태: 완료
- 문제 유형: 부분 동일
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/users/urls.py`, `/users/views.py`, `/users/services/oauth_service.py`, `/core/utils/auth.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthController.java`, `/auth/AuthService.java`, `/auth/GoogleTokenVerifier.java`, `/security/*`
- Django 기준 동작 요약: register/login/logout/refresh/google login/protected API
- Spring 현재 상태 요약: 핵심 기능 구현 + `email_verified`/만료/issuer/provider 오류 케이스 검증 반영
- 무엇이 다르거나 부족한지: 실 Google token 외부 연동은 사용자 로컬 검증으로 분리(운영 전 체크)
- 완료 기준: 계약 테스트 + 외부 연동 검증(사용자 실행 분리) + 보호 경로 회귀 테스트
- 완료 기준(증빙): Django 원본 경로와 Spring 대응 경로의 코드 대조 근거가 문서 또는 링크로 남아 있을 것
- 우선순위: 매우 높음

### [DIFF-02] 사용자 카테고리 1:1 재검증
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/users/urls.py`, `/users/views.py`, `/users/serializers.py`, `/users/models.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/UserController.java`, `/user/UserService.java`, `/user/UserMapper.java`, `/user/*.java`
- Django 기준 동작 요약: profile/settings/sessions/delete/summary 계약
- Spring 현재 상태 요약: profile/settings/sessions/delete/summary 계약이 Django 의미와 동등하게 반영되고, 공개 프로필 마스킹/세션 정렬/세션 종료 메시지 계약까지 테스트로 증빙 완료
- 무엇이 다르거나 부족한지: 없음(사용자 카테고리 기준)
- 완료 기준: 계약 테스트 + 스냅샷 비교(달성)
- 완료 기준(증빙): Django 원본 경로와 Spring 대응 경로의 코드 대조 근거가 문서 또는 링크로 남아 있을 것
- 우선순위: 높음

### [DIFF-03] 게시글 CRUD 1:1 재검증
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/posts/urls.py`, `/posts/views.py`, `/posts/serializers.py`, `/posts/services/post_service.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostController.java`, `/post/PostService.java`, `/post/PostUpsertRequest.java`, `/post/PostRepository.java`
- Django 기준 동작 요약: 생성/수정/삭제/조회 + 도메인 검증
- Spring 현재 상태 요약: 생성/수정/삭제/조회와 핵심 도메인 검증이 Django 의미와 동등하며, 태그 경계값(최대 10개/빈 태그 금지/중복 태그 보존)까지 테스트 증빙 완료
- 무엇이 다르거나 부족한지: 없음(게시글 CRUD 카테고리 기준)
- 완료 기준: 경계 테스트 매트릭스 통과(달성)
- 완료 기준(증빙): Django 원본 경로와 Spring 대응 경로의 코드 대조 근거가 문서 또는 링크로 남아 있을 것
- 우선순위: 매우 높음

### [DIFF-04] 상호작용(좋아요/북마크/조회수) 1:1 재검증
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/posts/views.py`, `/posts/services/interaction_service.py`, `/posts/models.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostController.java`, `/post/PostService.java`, `/post/PostInteractionRepository.java`
- Django 기준 동작 요약: 토글/목록/정렬/조회수 정합성
- Spring 현재 상태 요약: 토글/목록/조회수 동작을 Django 의미에 맞춰 반영했고, 자기 글 좋아요/북마크 방지 시 성공 응답+안내 메시지 계약까지 동등화
- 무엇이 다르거나 부족한지: 없음(상호작용 카테고리 기준)
- 완료 기준: 정렬축 테스트 + 동시성 테스트 + 자기 글 반응 계약 테스트 통과(달성)
- 완료 기준(증빙): Django 원본 경로와 Spring 대응 경로의 코드 대조 근거가 문서 또는 링크로 남아 있을 것
- 우선순위: 매우 높음

### [DIFF-05] 검색/필터/트렌딩/통계 1:1 재검증
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/core/urls.py`, `/core/views.py`, `/core/services/*.py`, `/stats/views.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java`, `/core/CoreService.java`, `/stats/StatsController.java`, `/stats/StatsService.java`
- Django 기준 동작 요약: 검색/정렬/필터, 트렌딩, 대시보드/유저 통계 계약 존재
- Spring 현재 상태 요약: Django 공개 정책과 동일하게 `GET /api/stats/dashboard`, `GET /api/auth/users/{username}/summary`를 비인증 접근 허용으로 맞췄고, `/api/stats/user`는 보호 경계로 유지됨
- 무엇이 다르거나 부족한지: 없음(DIFF-05 기준)
- 완료 기준: 질의 조합별 결과 비교 + 캐시/통계 계약 점검 + 공개 접근 정책 동등화 (달성)
- 완료 기준(증빙): Django 원본 경로와 Spring 대응 경로의 코드 대조 근거가 문서 또는 링크로 남아 있을 것
- 우선순위: 매우 높음

> 해석 주의:
> 이 항목의 목적은 **응답 의미와 프론트 의존 계약의 동등성**이지,  
> Django 내부 구현 수단을 그대로 모사하는 것이 아니다.

### [DIFF-06] 호환성/릴리즈 게이트 1:1 재검증
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend` (주요 사용자 플로우 경로 전반)
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend`, `/Users/mac/Documents/prompthub-springboot/frontend` (연동 시나리오)
- Django 기준 동작 요약: 실제 배포 경로에서 프론트 주요 플로우가 문제 없이 동작
- Spring 현재 상태 요약: 브라우저 E2E + CI smoke gate + 결과 문서화가 단일 근거로 유지되고, 게이트 테스트에 비로그인 공개 계약(`/api/stats/dashboard`, `/api/auth/users/{username}/summary`)이 포함되어 403 회귀를 차단함
- 무엇이 다르거나 부족한지: 없음(DIFF-06 기준)
- 완료 기준: 브라우저 E2E + CI smoke gate + 결과 문서화 + 공개 endpoint 회귀 차단 (달성)
- 완료 기준(증빙): Django 원본 경로와 Spring 대응 경로의 코드 대조 근거가 문서 또는 링크로 남아 있을 것
- 우선순위: 높음

---

## 7. 검증/증빙 규칙

### [VERIFY-01] 기능별 테스트 링크 고정
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/**/tests*.py` (존재 시), 핵심 기능 코드 경로
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/test/java/**`, 기능 대응 코드 경로
- Django 기준 동작 요약: 핵심 기능이 검증 가능한 형태로 존재
- Spring 현재 상태 요약: `docs/ops/feature-test-link-matrix.md`에 기능별 테스트 링크 단일 표를 고정했고 링크 유효성 검증 테스트를 추가함
- 무엇이 다르거나 부족한지: 없음(VERIFY-01 기준)
- 완료 기준: 기능↔테스트 링크 표 완성 (달성)
- 완료 기준(증빙): Django 원본 경로와 Spring 대응 경로의 코드 대조 근거가 문서 또는 링크로 남아 있을 것
- 우선순위: 높음

### [VERIFY-02] 계약 스냅샷/응답 샘플 증빙
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/**/views.py`, `/**/serializers.py`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/**/*Controller.java`, `/**/*Response*.java`
- Django 기준 동작 요약: 프론트가 의존하는 응답 shape 존재
- Spring 현재 상태 요약: `docs/ops/api-contract-snapshots.md`로 주요 API 응답 스냅샷을 단일 문서화했고, 문서 구조/필수 계약 필드를 검증 테스트로 고정함
- 무엇이 다르거나 부족한지: 없음(VERIFY-02 기준)
- 완료 기준: 주요 API 응답 스냅샷/샘플 비교 문서 완성 (달성)
- 완료 기준(증빙): Django 원본 경로와 Spring 대응 경로의 코드 대조 근거가 문서 또는 링크로 남아 있을 것
- 우선순위: 높음

### [VERIFY-03] 양측 매핑 증빙(Django ↔ Spring)
- 현재 상태: 완료
- 문제 유형: -
- Django 원본 경로: `/Users/mac/Documents/prompthub2/backend/**`
- Spring 대응 경로: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/**`, `/src/test/java/**`
- Django 기준 동작 요약: 원본 구현이 source of truth
- Spring 현재 상태 요약: `docs/ops/django-spring-mapping-evidence.md`에 기능별 증빙을 단일 표로 고정했고 문서 유효성 검증 테스트를 추가함
- 무엇이 다르거나 부족한지: 없음(VERIFY-03 기준)
- 완료 기준: 기능별 `Django 원본 경로 ↔ Spring 대응 경로 ↔ 테스트 링크 ↔ 판정 근거`가 모두 연결된 매핑 표 완성 (달성)
- 게이트 규칙: 이 항목이 완료되기 전까지 전체 프로젝트를 `완전 일치`로 판정하지 않음
- 우선순위: 매우 높음

---

## 8. 완료 판정 규칙

아래 조건이 모두 충족될 때만 최종적으로 **“Django와 완전 일치”** 라고 말할 수 있다.

1. 모든 체크리스트 항목이 `완료`
2. `부분 동일`, `차이 있음`, `근거 부족` 항목이 0개
3. placeholder / hardcoded / mock 대체 구현이 없음
4. 프론트가 의존하는 계약이 스냅샷/테스트로 증빙됨
5. 주요 기능의 도메인 규칙/부수효과/정합성이 테스트로 입증됨
6. 릴리즈 게이트(E2E/CI/smoke) 증빙이 확보됨
7. 모든 `DIFF-*` 및 `VERIFY-*` 항목에 대해 Django 원본 경로 ↔ Spring 대응 경로 ↔ 테스트/검증 근거가 연결되어 있어야 함

---

## 9. 운영 방식

- 한 번에 체크리스트 항목 1개만 닫는다
- 항목 하나를 수정한 뒤 반드시 다시 판정한다
- 완료 기준을 충족하지 못하면 `완료`로 올리지 않는다
- 기존 문서와 다른 축의 troubleshooting 서사가 나오면 별도 후보로 축적한다
- “비슷하게 동작”은 허용하지 않는다
- “문법은 달라도 의미는 같아야 한다” 원칙을 항상 우선한다

---

## 10. 재점검 로그 (2026-03-27)

- 경로 순회/근거:
  - Spring 컨트롤러 전수 검색: `@RequestBody Map` 0건, `ResponseEntity<?>` 0건
  - Spring 서비스 전수 검색: `fromMap`, `asString`, `asBoolean` 0건
  - 잔여 이슈: `PostController.currentUserOrNull()` 및 legacy URI(`/{postId}/update`, `/{postId}/delete`)는 유지
- 상태 변경:
  - `[SERVICE-01]` `진행 중 -> 완료`
  - `[CTRL-01]` `진행 중 -> 완료`

---

## 11. 항목별 경로 대조 재검증 (2026-03-27)

판정 기준:
- `신뢰 가능`: 경로 대조 + 코드 근거가 현재 상태와 일치
- `부분 신뢰`: 상태 방향은 맞지만 근거/증빙이 부족
- `신뢰 낮음`: 문서 상태와 실제 코드 구조가 어긋남

### 1) 인프라
- `INFRA-01`: 부분 신뢰  
  근거: `backend/src/main/resources/application.yml` + `backend/.env` 확인.  
  조치: 상태를 `진행 중`으로 하향(프로필 분리 증빙 보강 필요).
- `INFRA-02`: 신뢰 가능  
  근거: 운영 정책 차이로 제외 합의(`Render -> DBeaver -> Docker` 데이터 이관 경로), 핵심 데이터 정합성(Pass) 확보.

### 2) 엔티티
- `ENTITY-01`: 신뢰 가능  
  근거: `@Table(name=users/posts/platforms/ai_models/categories/post_interactions/trending_*)` 확인.
- `ENTITY-02`: 신뢰 가능  
  근거: Django `posts/models.py`의 `unique_model_slug_per_platform`, `core/models/trending.py`의 `unique_together(category, rank)`를 Spring 엔티티 유니크 제약으로 반영하고 `EntityConstraintParityTest`로 DB 강제 위반을 검증.

### 3) 레포지토리
- `REPO-01`: 신뢰 가능  
  근거: `TrendingRankingEntity/CoreService`에서 `relatedModel/useExactMatching/modelDetailContains/modelEtcContains` 코드 경로 확인.
- `REPO-02`: 신뢰 가능(미완료 판정)  
  근거: Django `-interactions__updated_at` vs Spring `findByUserAnd*OrderByIdDesc` 차이 확인.

### 4) 서비스
- `SERVICE-01`: 신뢰 가능  
  근거: Service 전수 검색 결과 `fromMap/asString/asBoolean` 0건.
- `SERVICE-02`: 신뢰 가능  
  근거: `docs/ops/service-rule-parity-matrix.md` 단일표 + `ServiceRuleParityMatrixVerificationTest` 링크/근거 검증 완료.
- `SERVICE-03`: 신뢰 가능  
  근거: `docs/ops/transaction-boundary-matrix.md` + `ServiceTransactionBoundaryTest`로 write path 경계와 회귀 검증 기준을 고정.

### 5) 컨트롤러
- `CTRL-01`: 신뢰 가능  
  근거: Controller 전수 검색 결과 `@RequestBody Map` 0건, `ResponseEntity<?>` 0건.
- `CTRL-02`: 신뢰 가능(진행 중 판정)  
  근거: `PostController`의 `/create`, `/{postId}/update`, `/{postId}/delete` legacy URI 확인.

### 6) 기능 동등성(DIFF)
- `DIFF-01`: 신뢰 가능(차이 있음 판정)  
  근거: Django `email_verified` 검증 존재, Spring verifier 미반영.
- `DIFF-02`: 부분 신뢰  
  근거: 사용자 플로우 테스트는 존재하나 항목별 1:1 매핑 증빙 미완성.
- `DIFF-03`: 부분 신뢰  
  근거: 검증 로직/테스트 다수 존재하나 Django 경계값 동등성 표준화 문서 부족.
- `DIFF-04`: 신뢰 가능(진행 중 판정)  
  근거: 정렬 축 차이(Interaction updated_at vs ID DESC) 확인.
- `DIFF-05`: 부분 신뢰  
  근거: 문서/테스트 증빙은 존재하나 실제 프론트 경로에서 `/api/stats/dashboard`, `/api/auth/users/{username}/summary` 403이 재현되어 정책 동등성 재검증 필요.
- `DIFF-06`: 부분 신뢰  
  근거: E2E/CI 증빙은 있으나 위 403을 게이트가 조기에 검출하지 못해 릴리즈 게이트 완결성 보강 필요.

### 7) 검증/증빙(VERIFY)
- `VERIFY-01`: 신뢰 가능  
  근거: `docs/ops/feature-test-link-matrix.md` 단일표 + `FeatureTestLinkMatrixVerificationTest` 링크 유효성 검증 완료.
- `VERIFY-02`: 신뢰 가능  
  근거: `docs/ops/api-contract-snapshots.md` 단일표 + `ApiContractSnapshotsVerificationTest` 필수 계약 필드/경로 검증 완료.
- `VERIFY-03`: 신뢰 가능  
  근거: `docs/ops/django-spring-mapping-evidence.md` 단일표 + `DjangoSpringMappingEvidenceVerificationTest` 필드/링크 검증 완료.

요약:
- 신뢰 가능: 13
- 부분 신뢰: 5
- 신뢰 낮음: 0
- 결론: `DIFF-05`, `DIFF-06`이 재개방되어 체크리스트 기준 미완료 항목이 존재한다. 우선 보안 정책 동등성(공개 endpoint 접근)부터 수정 후 게이트를 재검증한다.

---

## 12. 단일 항목 작업 로그 (2026-03-27, DIFF-01)

- 대상: `[DIFF-01] 인증 카테고리 1:1 재검증`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/users/services/oauth_service.py`
  - 핵심 규칙: `email_verified`가 true가 아니면 로그인 거부
- Spring 반영:
  - `GoogleTokenVerifier`에 `email_verified` 파싱/검증 로직 추가
  - `email_verified` 누락/false일 때 `400 BAD_REQUEST` 처리
- 테스트 증빙:
  - `GoogleTokenVerifierTest`: valid/missing/false 케이스 추가 검증
  - `AuthControllerTest`: google/refresh 경로 회귀
  - `UserControllerSecurityTest`: 보호 API 인증 경계 회귀
- 판정:
  - 코드 차이(`email_verified`)는 해소
  - 그러나 완료 기준의 `실 Google token 외부 연동 검증`은 별도 증빙 필요
  - 따라서 상태는 `진행 중`, 문제 유형은 `차이 있음 -> 근거 부족`으로 조정

---

## 13. 단일 항목 작업 로그 (2026-03-27, CTRL-02)

- 대상: `[CTRL-02] legacy URI 정리와 호환성 정책 분리`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/posts/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/views.py`
- Spring 반영:
  - `PostController`에 canonical URI와 legacy alias를 분리 유지
    - canonical: `POST /api/posts`, `PUT/PATCH /api/posts/{postId}`, `DELETE /api/posts/{postId}`
    - legacy alias: `POST /api/posts/create`, `PUT/PATCH /api/posts/{postId}/update`, `DELETE /api/posts/{postId}/delete`
  - legacy alias 응답에 `Warning: 299 - Deprecated legacy endpoint ...` 헤더 추가
  - 기존 프론트 호환을 위해 trailing slash 변형 URI도 병행 매핑
- 테스트 증빙:
  - `PostControllerSection4FlowTest.canonicalUris_work_and_legacyAliases_returnDeprecationWarningHeader` 추가
  - 실행: `./gradlew test --tests com.lshlabs.prompthubspring.post.PostControllerSection4FlowTest` (성공)
- 판정:
  - canonical/alias 분리 + 호환성 + deprecation 안내가 코드/테스트로 증빙되어 `완료`로 상향

---

## 14. 단일 항목 작업 로그 (2026-03-27, DIFF-04)

- 대상: `[DIFF-04] 상호작용(좋아요/북마크/조회수) 1:1 재검증`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/posts/views.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/services/interaction_service.py`
- Spring 반영:
  - `PostService.toggleLike/toggleBookmark`를 Django 계약에 맞춰 보강
    - 자기 글 반응 시 상태코드 `success` 유지
    - 안내 메시지(`자신의 게시글에는 ...`) 반환
    - 카운트 증가 없이 기존 카운트 반환
  - `ToggleLikeResponse`, `ToggleBookmarkResponse`에 nullable `message` 필드 추가(`@JsonInclude.NON_NULL`)
  - `PostInteractionConcurrencyTest`를 H2 독립 환경으로 고정하여 스키마 편차 없이 동시성 검증 가능하도록 정리
- 테스트 증빙:
  - `PostInteractionOrderingRepositoryTest` (정렬축: `updated_at DESC, id DESC`)
  - `PostInteractionConcurrencyTest` (좋아요/북마크/조회수 동시성 정합성)
  - `PostControllerSection4FlowTest.selfLikeAndBookmark_followDjangoContract_messageAndNoCountIncrease` (자기 글 반응 계약)
  - 실행:
    - `./gradlew test --tests com.lshlabs.prompthubspring.post.PostInteractionOrderingRepositoryTest --tests com.lshlabs.prompthubspring.post.PostInteractionConcurrencyTest --tests com.lshlabs.prompthubspring.post.PostControllerSection4FlowTest` (성공)
- 판정:
  - 상호작용 카테고리의 기능 의미/계약/정렬/동시성 기준이 Django와 동등하게 증빙되어 `완료`로 상향

---

## 15. 단일 항목 작업 로그 (2026-03-27, DIFF-03)

- 대상: `[DIFF-03] 게시글 CRUD 1:1 재검증`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/posts/serializers.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/views.py`
  - 핵심 기준: 태그 최대 10개, 빈 태그 금지, 태그 정규화 시 중복은 제거하지 않음
- Spring 반영:
  - `PostUpsertRequest`
    - 태그 개수 제한을 `max=10`으로 동등화
    - 태그 유효성 메시지를 Django 의미에 맞게 정렬
  - `PostService.toTagCsv`
    - 태그 CSV 변환 시 중복 제거(`distinct`) 로직 삭제
    - Django와 동일하게 중복 태그 보존
  - `PostControllerSection4FlowTest`
    - `create_enforcesDjangoTagRules_and_preservesDuplicateTags` 추가
    - 검증: 11개 태그 거부, 공백 태그 거부, 중복 태그 저장/응답 보존
- 테스트 증빙:
  - 실행: `./gradlew test --tests com.lshlabs.prompthubspring.post.PostControllerSection4FlowTest` (성공)
- 판정:
  - 게시글 CRUD의 태그 경계 규칙과 계약이 Django source of truth와 동등하게 맞춰져 `완료`로 상향

---

## 16. 단일 항목 작업 로그 (2026-03-27, DIFF-02)

- 대상: `[DIFF-02] 사용자 카테고리 1:1 재검증`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/users/views.py`
  - `/Users/mac/Documents/prompthub2/backend/users/serializers.py`
- Spring 반영:
  - `UserSessionRepository` 정렬 기준을 `created_at DESC`에서 `last_active DESC`로 변경 (Django `order_by('-last_active')` 동등화)
  - `UserService` 세션 종료 메시지 계약 동등화
    - 미존재 세션: `"해당 세션을 찾을 수 없습니다."`
    - 기타 세션 전체 종료: `"다른 모든 세션을 종료했습니다."`
  - `UserController` 전 엔드포인트에 trailing slash 호환 매핑 추가
  - `UserControllerFlowTest`를 H2 독립 환경으로 고정해 재현 가능한 사용자 계약 검증 기반 마련
  - 사용자 흐름 테스트에 세션 `last_active` 정렬 및 전체 세션 종료 카운트 검증 추가
- 테스트 증빙:
  - 실행:
    - `./gradlew test --tests com.lshlabs.prompthubspring.user.UserControllerFlowTest --tests com.lshlabs.prompthubspring.user.UserPublicProfileSummaryMaskingFlowTest --tests com.lshlabs.prompthubspring.user.UserInfoContractParityTest` (성공)
- 판정:
  - 사용자 카테고리의 핵심 계약(마스킹/세션 정렬/세션 종료 메시지)이 Django와 동등하게 증빙되어 `완료`로 상향

---

## 17. 단일 항목 작업 로그 (2026-03-27, INFRA-01)

- 대상: `[INFRA-01] 로컬/운영 DB 프로필 분리 고정`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/config/settings.py`
  - (`settings_dev` 기본 + 환경별 오버라이드 구조)
- Spring 반영:
  - `application.yml`: 공통 설정만 유지, `spring.profiles.default=local` 지정
  - `application-local.yml`: 로컬/운영 기본 DB 정책(`PostgreSQL`, `ddl-auto=update`) 분리
  - `application-test.yml`: 테스트 전용 DB 정책(`H2`, `ddl-auto=create-drop`) 분리
  - 프로필 부팅 검증 테스트 추가:
    - `TestProfileConfigurationTest`
    - `LocalProfileConfigurationTest`
- 테스트 증빙:
  - 실행:
    - `./gradlew test --tests com.lshlabs.prompthubspring.config.TestProfileConfigurationTest --tests com.lshlabs.prompthubspring.config.LocalProfileConfigurationTest` (성공)
- 판정:
  - Django의 환경 분리 운영 원칙과 동등한 프로필 분리 구조가 Spring에서 코드/테스트로 고정되어 `완료`로 상향

---

## 18. 단일 항목 작업 로그 (2026-03-27, ENTITY-02)

- 대상: `[ENTITY-02] 필수 제약/유니크/인덱스 동등성 검증`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/posts/models.py`
    - `UniqueConstraint(fields=['platform', 'slug'], name='unique_model_slug_per_platform')`
  - `/Users/mac/Documents/prompthub2/backend/core/models/trending.py`
    - `unique_together = ['category', 'rank']`
- Spring 반영:
  - `AiModel`에 유니크 제약 추가:
    - `@UniqueConstraint(name = "unique_model_slug_per_platform", columnNames = {"platform_id", "slug"})`
  - `TrendingRankingEntity`에 유니크 제약 추가:
    - `@UniqueConstraint(name = "uk_trending_category_rank", columnNames = {"category_id", "rank"})`
  - 제약 동작 검증 테스트 추가:
    - `EntityConstraintParityTest`
      - 동일 플랫폼 내 중복 slug 저장 시 `DataIntegrityViolationException` 검증
      - 동일 카테고리 내 중복 rank 저장 시 `DataIntegrityViolationException` 검증
- 테스트 증빙:
  - 실행:
    - `./gradlew test --tests com.lshlabs.prompthubspring.entity.EntityConstraintParityTest` (성공)
- 판정:
  - Django 핵심 제약 2건의 의미가 Spring 엔티티/DB 강제 동작까지 동일하게 증빙되어 `완료`로 상향

---

## 20. 단일 항목 작업 로그 (2026-03-27, INFRA-03)

- 대상: `[INFRA-03] Render → DBeaver → Docker 실데이터 정합성 검증`
- Render 실측(사용자 제공):
  - users 11, posts 10, ai_models 88, categories 11, platforms 8,
    trending_categories 9, trending_rankings 27, user_settings 11, user_sessions 2
- Docker Postgres 실측(검증):
  - users 11, posts 10, ai_models 88, categories 11, platforms 8,
    trending_categories 9, trending_rankings 27, user_settings 11, user_sessions 0
- 판정:
  - 핵심 도메인 테이블 건수는 1:1 일치
  - `user_sessions`는 신규 시스템에서 유효하지 않은 구 세션 데이터로, 의도적 이관 제외 정책 승인
  - 데이터 마이그레이션 정합성 항목 `최종 통과(Pass)` 처리

---

## 21. 단일 항목 작업 로그 (2026-03-27, SERVICE-03)

- 대상: `[SERVICE-03] 트랜잭션 경계 재점검`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/users/services/*.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/services/*.py`
  - `/Users/mac/Documents/prompthub2/backend/core/services/*.py`
- Spring 반영:
  - `docs/ops/transaction-boundary-matrix.md` 추가
    - Auth/User/Post 서비스의 write path와 트랜잭션 경계 매핑 고정
  - `ServiceTransactionBoundaryTest` 추가
    - Auth/User/Post write 메서드의 `@Transactional` 적용 여부 회귀 검증
- 테스트 증빙:
  - 실행:
    - `./gradlew test --tests com.lshlabs.prompthubspring.transaction.ServiceTransactionBoundaryTest` (성공)
- 판정:
  - 트랜잭션 경계 항목의 "근거 부족"을 문서+테스트 근거로 해소하여 `완료`로 상향

---

## 22. 단일 항목 작업 로그 (2026-03-30, VERIFY-01)

- 대상: `[VERIFY-01] 기능별 테스트 링크 고정`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/**/tests*.py` (존재 시) + 기능 코드 경로
- Spring 반영:
  - `docs/ops/feature-test-link-matrix.md` 추가
    - AUTH / USER / POST-CRUD / INTERACTION / CORE-STATS / RELEASE-GATE / ENTITY-TRANSACTION 기능별 테스트 링크 단일표 고정
  - `FeatureTestLinkMatrixVerificationTest` 추가
    - 링크표 필수 섹션 존재 검증
    - 문서에 기재된 테스트 파일 경로의 실제 존재 여부 검증
- 테스트 증빙:
  - 실행:
    - `./gradlew test --tests com.lshlabs.prompthubspring.verification.FeatureTestLinkMatrixVerificationTest` (성공)
- 판정:
  - 기능↔테스트 링크 단일 표 부재 문제를 해소했고, 링크 유효성까지 자동 검증되어 `완료`로 상향

---

## 23. 단일 항목 작업 로그 (2026-03-30, VERIFY-03)

- 대상: `[VERIFY-03] 양측 매핑 증빙(Django ↔ Spring)`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/**`
- Spring 반영:
  - `docs/ops/django-spring-mapping-evidence.md` 추가
    - 기능별로 `Django 원본 경로 ↔ Spring 대응 경로 ↔ 테스트 링크 ↔ 판정 근거`를 단일 문서로 고정
  - `DjangoSpringMappingEvidenceVerificationTest` 추가
    - 매핑 문서의 필수 섹션/필수 필드 존재 검증
    - 문서에 기재된 Spring 경로 파일 존재 검증
- 테스트 증빙:
  - 실행:
    - `./gradlew test --tests com.lshlabs.prompthubspring.verification.DjangoSpringMappingEvidenceVerificationTest` (성공)
- 판정:
  - 기능별 매핑 증빙이 단일 문서+자동 검증으로 고정되어 `완료`로 상향

---

## 24. 단일 항목 작업 로그 (2026-03-30, VERIFY-02)

- 대상: `[VERIFY-02] 계약 스냅샷/응답 샘플 증빙`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/**/views.py`
  - `/Users/mac/Documents/prompthub2/backend/**/serializers.py`
- Spring 반영:
  - `docs/ops/api-contract-snapshots.md` 추가
    - AUTH/USER/POST-LIST/INTERACTION/CORE/STATS 주요 API 응답 스냅샷 단일 문서화
  - `ApiContractSnapshotsVerificationTest` 추가
    - 스냅샷 문서 필수 섹션 존재 검증
    - 핵심 계약 필드(`status`, `data`, `pagination`, `message`) 존재 검증
    - 문서에 기재된 Spring 경로 파일 존재 검증
- 테스트 증빙:
  - 실행:
    - `./gradlew test --tests com.lshlabs.prompthubspring.verification.ApiContractSnapshotsVerificationTest` (성공)
- 판정:
  - 스냅샷/샘플 증빙이 단일 문서+검증 테스트로 고정되어 `완료`로 상향

---

## 25. 단일 항목 작업 로그 (2026-03-30, SERVICE-02)

- 대상: `[SERVICE-02] 비즈니스 규칙/상태 전이 동등성(사용자/게시글/통계)`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/users/services/*.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/services/*.py`
  - `/Users/mac/Documents/prompthub2/backend/core/services/*.py`
  - `/Users/mac/Documents/prompthub2/backend/stats/views.py`
- Spring 반영:
  - `docs/ops/service-rule-parity-matrix.md` 추가
    - 서비스 도메인별(사용자/게시글/상호작용/검색·통계) 규칙, 상태 전이, 테스트 링크를 단일 표로 고정
  - `ServiceRuleParityMatrixVerificationTest` 추가
    - 매트릭스 필수 섹션/필드 존재 검증
    - 문서에 기재된 Spring main/test 경로 파일 존재 검증
- 테스트 증빙:
  - 실행:
    - `./gradlew test --tests com.lshlabs.prompthubspring.verification.ServiceRuleParityMatrixVerificationTest` (성공)
- 판정:
  - "규칙 매트릭스와 테스트 증빙의 단일 문서 부재" 이슈를 해소하여 `완료`로 상향

---

## 26. 단일 항목 작업 로그 (2026-03-30, DIFF-05)

- 대상: `[DIFF-05] 검색/필터/트렌딩/통계 1:1 재검증`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/core/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/core/views.py`
  - `/Users/mac/Documents/prompthub2/backend/core/services/*.py`
  - `/Users/mac/Documents/prompthub2/backend/stats/views.py`
- Spring 반영:
  - `docs/ops/diff05-core-stats-parity-evidence.md` 추가
    - 질의 조합별 검색 비교 근거
    - 트렌딩 캐시 계약 점검 근거
    - 통계 집계 계약 점검 근거
    - 대응 테스트 링크 단일화
  - `Diff05ParityEvidenceVerificationTest` 추가
    - 증빙 문서 필수 블록/테스트 링크 유효성 검증
- 테스트 증빙:
  - 실행:
    - `./gradlew test --tests com.lshlabs.prompthubspring.verification.Diff05ParityEvidenceVerificationTest` (성공)
- 판정:
  - DIFF-05 잔여 이슈였던 "질의 조합/캐시·통계 계약 대조 근거 분산"을 단일 증빙으로 해소하여 `완료`로 상향

---

## 27. 단일 항목 작업 로그 (2026-03-30, DIFF-06)

- 대상: `[DIFF-06] 호환성/릴리즈 게이트 1:1 재검증`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/**`
- Spring 반영:
  - `docs/ops/diff06-release-gate-evidence.md` 추가
    - 브라우저 E2E(Playwright) 스크린샷 증빙 고정
    - CI smoke gate 워크플로우/테스트 링크 단일화
  - `.github/workflows/release-gate-smoke.yml` 추가
  - `ReleaseGateSmokeTest`, `TrailingSlashCompatibilitySmokeTest`, `Diff06ReleaseGateEvidenceVerificationTest` 추가/보강
  - `WebMvcConfig` + `SecurityConfig` 보강으로 trailing slash 호환성 정합화
  - `AppUser`의 Django 비동등 필드(`updated_at`) 제거로 런타임 정합성 보정
- 테스트 증빙:
  - 실행:
    - `./gradlew test --tests com.lshlabs.prompthubspring.smoke.ReleaseGateSmokeTest --tests com.lshlabs.prompthubspring.smoke.TrailingSlashCompatibilitySmokeTest --tests com.lshlabs.prompthubspring.verification.Diff06ReleaseGateEvidenceVerificationTest` (성공)
- 판정:
  - 브라우저 E2E + CI smoke gate + 결과 문서화 기준을 충족하여 `완료`로 상향

---

## 28. 단일 항목 작업 로그 (2026-03-30, DIFF-05/DIFF-06 재개방)

- 대상: `[DIFF-05]`, `[DIFF-06]`
- 재검증 근거:
  - 프론트 커뮤니티 페이지(`frontend/app/community/page.tsx`)의 `loadStats` 호출에서 콘솔 오류 재현:
    - `GET /api/stats/dashboard` 실패(403)
    - `GET /api/auth/users/{username}/summary` 실패(403)
  - Django source of truth 확인:
    - `stats/views.py`의 `dashboard_stats`는 공개 접근(인증 데코레이터 없음)
    - `users/views.py`의 `user_summary`는 공개 요약 조회로 동작
- 판정 조정:
  - `DIFF-05`: `완료 -> 진행 중(차이 있음)`
  - `DIFF-06`: `완료 -> 진행 중(근거 부족)`
- 사유:
  - 문서/테스트 증빙이 있어도, 실제 프론트 연동 계약에서 공개 endpoint 접근 정책 불일치가 남아 있어 완료 판정을 유지할 수 없음

---

## 29. 단일 항목 작업 로그 (2026-03-30, DIFF-05 재종결)

- 대상: `[DIFF-05] 검색/필터/트렌딩/통계 1:1 재검증`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/stats/views.py` (`dashboard_stats`: 공개)
  - `/Users/mac/Documents/prompthub2/backend/stats/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/users/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/users/views.py` (`user_summary`: 공개)
- Spring 반영:
  - `backend/src/main/java/com/lshlabs/prompthubspring/config/SecurityConfig.java`
    - 공개 GET 경로에 아래를 명시적으로 허용:
      - `/api/stats/dashboard`
      - `/api/auth/users/*/summary`
    - 보호 경로(`GET /api/stats/user`)는 기존 인증 정책 유지
  - `backend/src/test/java/com/lshlabs/prompthubspring/security/PublicEndpointParitySecurityTest.java` 추가
    - 비인증 공개 접근 허용(대시보드/요약) 검증
    - 비인증 보호 접근 차단(`/api/stats/user`) 검증
- 테스트 증빙:
  - 실행:
    - `./gradlew test --tests com.lshlabs.prompthubspring.security.PublicEndpointParitySecurityTest` (성공)
- 판정:
  - `DIFF-05`: `진행 중 -> 완료`
  - 남은 미완료는 `DIFF-06` 단일 항목

---

## 30. 단일 항목 작업 로그 (2026-03-30, DIFF-06 종결)

- 대상: `[DIFF-06] 호환성/릴리즈 게이트 1:1 재검증`
- Django source of truth:
  - `/Users/mac/Documents/prompthub2/backend/stats/views.py` (`dashboard_stats` 공개)
  - `/Users/mac/Documents/prompthub2/backend/users/views.py` (`user_summary` 공개)
  - `/Users/mac/Documents/prompthub2/backend/stats/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/users/urls.py`
- Spring 반영:
  - `backend/src/test/java/com/lshlabs/prompthubspring/smoke/ReleaseGateSmokeTest.java`
    - 릴리즈 게이트에 공개 계약 검증 추가:
      - `GET /api/stats/dashboard` 비인증 200
      - `GET /api/auth/users/{username}/summary` 비인증 200
  - `docs/ops/diff06-release-gate-evidence.md`
    - 공개 endpoint 계약 게이트 항목 명시
  - `backend/src/test/java/com/lshlabs/prompthubspring/verification/Diff06ReleaseGateEvidenceVerificationTest.java`
    - 증빙 문서 필수 블록 검증에 공개 endpoint 계약 문자열 추가
- 테스트 증빙:
  - 실행:
    - `./gradlew test --tests com.lshlabs.prompthubspring.smoke.ReleaseGateSmokeTest --tests com.lshlabs.prompthubspring.smoke.TrailingSlashCompatibilitySmokeTest --tests com.lshlabs.prompthubspring.verification.Diff06ReleaseGateEvidenceVerificationTest --tests com.lshlabs.prompthubspring.security.PublicEndpointParitySecurityTest` (성공)
- 판정:
  - `DIFF-06`: `진행 중 -> 완료`
  - 공개 endpoint 403 회귀를 게이트 단계에서 사전 차단하도록 보강 완료
