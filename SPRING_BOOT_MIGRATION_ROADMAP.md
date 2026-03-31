## PromptHub Spring Boot 마이그레이션 단계별 구현 로드맵 (7섹션)

### 요약
`SPRING_BOOT_MIGRATION_MASTERPLAN`, `BACKEND_MIGRATION_REFERENCE`, `config.toml` 규칙을 기준으로, **프론트 호환 우선 + 점진적 표준화** 전략으로 7개 섹션으로 분할한다.  
기본 모드는 안정 마이그레이션이며, 포트폴리오 스토리 가치가 높은 구간만 **AI baseline review 기반 troubleshooting 시나리오** 가능성을 명시한다.


---

## 섹션 1. 백엔드 공통 골격/운영 기반
- **목표**: Spring 백엔드의 공통 실행 기반 확정
- **이번 섹션에서 만들 파일/구성요소**
- `common` 패키지의 API 응답 envelope, 공통 에러 코드/예외, `@RestControllerAdvice`
- `application.properties`/프로필 분리(`application-local`, `application-test`)
- Security 기본 필터 체인(permit/deny 기본 정책, CORS 기본값)
- 헬스체크/기본 컨트롤러
- **선행조건**
- 현재 `backend` Gradle 프로젝트가 빌드/테스트 가능 상태
- 프론트에서 기대하는 기본 API base URL 확정
- **Definition of Done**
- 애플리케이션 기동 성공
- 공통 에러 응답 포맷이 샘플 API에서 일관되게 반환
- `./gradlew test` 통과
- **Troubleshooting 시나리오 가치**: `낮음` (안정 구현 우선)

---

## 섹션 2. 인증 코어 (Google ID Token 검증 + 내부 JWT 발급)
- **목표**: Google ID Token 서버 검증 후 내부 JWT 발급 파이프라인 구축
- **이번 섹션에서 만들 파일/구성요소**
- `auth` 패키지: `AuthController`, `AuthService`, `GoogleTokenVerifier`, `JwtProvider`
- 로그인/토큰 재발급/로그아웃 DTO
- 토큰 관련 설정/유틸(만료, 서명키, 클레임 정책)
- **선행조건**
- 섹션 1의 공통 에러 처리 및 Security 뼈대 완료
- Google OAuth 환경변수 로컬/배포 값 준비
- **Definition of Done**
- `/api/auth/google` 로그인 성공/실패 케이스 검증
- 잘못된 `aud/iss/sub` 입력 시 명확한 오류 응답
- 내부 Access/Refresh 발급 및 재발급 동작 확인
- **Troubleshooting 시나리오 가치**: `중간 (OAuth/JWT 경계에서 AI baseline review 기반 구조적 결함 서사 가능, 단 기본 구현은 안정 우선)`

---

## 섹션 3. 사용자 프로필/계정 관리 API
- **목표**: users 도메인 마이그레이션
- **이번 섹션에서 만들 파일/구성요소**
- `user` 패키지: `UserController`, `UserService`, `UserRepository`, 관련 Entity/DTO
- 프로필 수정, 비밀번호 변경, 세션 조회/종료, 계정 삭제 API
- 권한 규칙은 Service 계층 우선 + 필요한 곳만 Method Security 보조 적용
- **선행조건**
- 섹션 2 인증 코어 및 사용자 식별 체계 완성
- Django `users/urls.py`, `users/views.py`, `users/serializers.py` 매핑표 준비
- **Definition of Done**
- 인증 사용자 기준 자기 정보 API 정상 동작
- 타인 정보 접근 차단/권한 오류 처리 검증
- 프론트에서 사용하는 사용자 응답 필드 호환 확인
- **Troubleshooting 시나리오 가치**: `중간 (권한 규칙/집계 정확도 측면에서 AI baseline review 서사 가능)`

---

## 섹션 4. 게시글 CRUD + 도메인 3중 검증
- **목표**: posts 핵심 기능과 비즈니스 규칙 강제
- **이번 섹션에서 만들 파일/구성요소**
- `post` 패키지: `PostController`, `PostService`, `PostRepository`, `PostEntity`, CRUD DTO
- DTO 검증(`@Valid`) + 서비스 교차 필드 검증 + DB 제약조건
- 카테고리/태그/플랫폼/모델 관련 기본 조회 API
- **선행조건**
- 섹션 3 사용자/권한 정보 안정화
- Django `posts/models.py`, `posts/services/post_service.py` 규칙 분석 완료
- **Definition of Done**
- CRUD 전 경로 정상 동작
- 비정상 조합 데이터가 DTO/Service/DB 중 하나에서 반드시 차단
- 목록/상세 응답이 프론트 계약 필드와 호환
- **Troubleshooting 시나리오 가치**: `높음 (DTO/Service/DB 검증 파이프라인에서 AI baseline review 기반 정합성 서사 적합)`

---

## 섹션 5. 상호작용 API + 조회수 동시성 제어
- **목표**: 좋아요/북마크/내 활동 API와 조회수 증가 정합성 보장
- **이번 섹션에서 만들 파일/구성요소**
- `post` 내 interaction 컴포넌트(`Like/Bookmark` 서비스/리포지토리)
- 조회수 증가용 원자적 업데이트 쿼리
- `liked-posts`, `bookmarked-posts`, `my-posts` API
- **선행조건**
- 섹션 4의 게시글/사용자 관계 모델 확정
- 트랜잭션 경계 정책 확정
- **Definition of Done**
- 좋아요/북마크 토글 일관성 보장
- 동시 요청에서 조회수 유실(Lost Update) 방지 확인
- 관련 단위/통합 테스트 통과
- **Troubleshooting 시나리오 가치**: `매우 높음 (AI baseline의 상태 변경/집계 변경 분리 구조를 Human Architect가 리뷰하며 발견하는 동시성/정합성 서사에 적합)`

---

## 섹션 6. 검색/필터/트렌딩 + 캐시 계층
- **목표**: 조회성 기능을 성능 중심으로 이관
- **이번 섹션에서 만들 파일/구성요소**
- `core` 패키지 검색 API(정렬/필터 옵션 포함), QueryDSL 기반 동적 조회
- 트렌딩 랭킹 서비스 + 캐시(`@Cacheable`, 키 전략, TTL)
- `stats` 대시보드/유저 통계 API
- **선행조건**
- 섹션 4~5 데이터 모델/인덱스 초안 확정
- Redis 사용 여부(로컬/운영) 설정 준비
- **Definition of Done**
- 검색/필터/정렬 API 기능 동등성 확보
- 트렌딩 캐시 hit/miss 동작 검증
- 성능 병목 쿼리(N+1/풀스캔 위험) 식별 및 1차 개선 완료
- **Troubleshooting 시나리오 가치**: `매우 높음 (AI baseline review 기반 N+1, cache stampede, query 튜닝 서사에 적합)`

---

## 섹션 7. 호환성 정리/품질 게이트/2차 표준화 준비
- **목표**: 1차 호환 마이그레이션 완료 선언 + 2차 REST 표준화 준비
- **이번 섹션에서 만들 파일/구성요소**
- Django vs Spring API 비교표 문서
- 통합 테스트 시나리오 보강(Testcontainers: Postgres/Redis)
- 운영 관측 기본(Micrometer/Actuator) 및 체크리스트
- 2차 표준화 대상 endpoint 목록(호환 유지/즉시 표준화 구분)
- **선행조건**
- 섹션 1~6 기능 완료
- 프론트 연동 스모크 테스트 가능 상태
- **Definition of Done**
- `./gradlew test` + 핵심 API 스모크 테스트 통과
- 프론트 핵심 플로우(로그인, 목록, 상세, 좋아요/북마크) 연동 성공
- 2차 REST 표준화 백로그가 결정 완료 상태
- **Troubleshooting 시나리오 가치**: `중간 (회고/리팩터링 정리 서사 가치 있음)`

---

## 기본 운영 원칙 (config.toml 반영)
- 기본은 **정상 마이그레이션 구현**
- 시나리오 주입은 섹션 5/6 중심으로만 선택 적용
- 인증 코어/보안/배포/시크릿은 의도적 결함 주입 금지
- baseline flaw가 있는 경우, **최종 완료 판정은 Human Architect의 리팩터링과 테스트 검증 후** 판단
- troubleshooting 문서는 **구현 중 버그 회고**보다 **AI baseline review 과정에서 발견한 구조적 결함 문서화**를 우선한다