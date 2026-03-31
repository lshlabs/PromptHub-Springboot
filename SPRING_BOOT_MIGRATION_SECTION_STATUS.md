# Spring Boot Migration Section Status Report

Last Updated: 2026-03-26
Reference: `SPRING_BOOT_MIGRATION_ROADMAP.md`

## 섹션 1. 백엔드 공통 골격/운영 기반

- 상태: `완료`
- 반영된 파일:
  - `backend/src/main/resources/application.properties`
  - `backend/src/main/resources/application-local.properties`
  - `backend/src/main/resources/application-test.properties`
  - `backend/src/main/java/com/lshlabs/prompthubspring/common/GlobalExceptionHandler.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/config/SecurityConfig.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/config/AppConfig.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java` (`/api/core/health/`)
- 구현된 범위:
  - 프로필 분리(local/test), 공통 예외 처리, 기본 Security/CORS, 헬스체크
- 아직 비어 있는 부분:
  - `ApiEnvelope` 전 API 일괄 적용(현재는 Map 기반 응답 혼용)
- 테스트 여부:
  - `./gradlew test` 통과
- DoD 충족 여부:
  - 충족
- troubleshooting 후보:
  - `아니오` (안정 구현 우선 구간)

## 섹션 2. 인증 코어 (Google ID Token + 내부 JWT)

- 상태: `완료`
- 완료 판정: `완료` (완료/부분 완료/미완료 기준)
- 반영된 파일:
  - `backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/auth/GoogleTokenVerifier.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/auth/JwtProvider.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthToken.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthTokenRepository.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/security/TokenAuthFilter.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/auth/AuthServiceRefreshTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/auth/JwtProviderTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/auth/GoogleTokenVerifierTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/auth/AuthControllerTest.java`
- 구현된 범위:
  - `/api/auth/google/`, `/register/`, `/login/`, `/logout/`, `/token/refresh/` 엔드포인트
  - Google tokeninfo 기반 `aud/iss/sub` 검증 로직 및 오류 응답 경계 고정
  - 내부 JWT 생성 및 Token 헤더 인증 필터
  - refresh rotation(재발급 시 기존 refresh revoke), 입력 누락/유효성/소유자 불일치 검증 보강
- 아직 비어 있는 부분:
  - 실 Google ID Token 기반 외부 연동 E2E 검증 (운영 검증 backlog)
  - 테스트 어노테이션 deprecation 경고 정리 (리팩터링 backlog)
- 테스트 여부:
  - `./gradlew test --tests "com.lshlabs.prompthubspring.auth.*"` 통과
  - `./gradlew test` 통과
- DoD 충족 여부:
  - 충족
- troubleshooting 후보:
  - `예 (OAuth/JWT 경계 + refresh 수명주기 무결성 관점의 Human Architect review 서사 후보)`

## 섹션 3. 사용자 프로필/계정 관리 API

- 상태: `완료`
- 완료 판정: `완료` (완료/부분 완료/미완료 기준)
- 반영된 파일:
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/UserController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/UserService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/AppUser.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/UserSettings.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/UserSession.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/AppUserRepository.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/UserSettingsRepository.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/UserSessionRepository.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthTokenRepository.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/UserMapper.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserControllerFlowTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserControllerSecurityTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserServiceTest.java`
- 구현된 범위:
  - 프로필 조회/수정, 비밀번호 변경, 설정 조회/수정, 세션 조회/종료, 계정 삭제, 요약/정보 API
  - 인증 사용자 기준 profile/settings/info/sessions/summary/delete 흐름 통합 검증 반영
  - 타인 세션 종료 시도 차단 검증 반영
  - 계정 삭제 시 연관 데이터(auth token/session/settings) 정리 후 사용자 삭제 안정화
  - summary 응답 null 안전성 보강(500 오류 방지)
- 아직 비어 있는 부분:
  - `[후속 backlog]` 프론트 실연동 E2E 기반 사용자 플로우 증빙 문서화
  - `[후속 backlog]` 사용자 API 부가 시나리오(비밀번호 변경/아바타 재생성/all-session 종료) 확장 테스트
- 테스트 여부:
  - `UserControllerFlowTest.authenticatedProfileSettingsInfoAndSessionsFlow_works`
  - `UserControllerFlowTest.deletingOtherUsersSession_isRejected`
  - `UserControllerFlowTest.summary_returnsFrontendContractFields`
  - `UserControllerFlowTest.deleteAccount_removesUserAndBlocksFollowUpProfileAccess`
  - `UserControllerSecurityTest.profile_requiresAuthentication`
  - `UserControllerSecurityTest.protectedUserEndpoints_requireAuthentication`
  - `UserServiceTest.summary_returnsAggregatedCounts`
  - `UserServiceTest.endSession_throwsNotFound_whenSessionNotOwnedByCurrentUser`
  - `UserServiceTest.endSession_revokesTargetSession`
  - `UserServiceTest.endOtherSessions_returnsRevokedCount`
  - `UserServiceTest.deleteAccount_removesUserRelatedRowsBeforeDeletingUser`
  - `./gradlew test --tests "com.lshlabs.prompthubspring.user.*"` 통과
  - `./gradlew test` 통과
- DoD 충족 여부:
  - 충족
- troubleshooting 후보:
  - `예 (권한 검증/요약 집계 정확도 측면의 AI baseline review 서사 후보)`

## 섹션 4. 게시글 CRUD + 도메인 3중 검증

- 상태: `완료`
- 완료 판정: `완료` (완료/부분 완료/미완료 기준)
- 반영된 파일:
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/Post.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostUpsertRequest.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/Platform.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/AiModel.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/Category.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/*Repository.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/config/BootstrapDataConfig.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostUpsertRequestValidationTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostServiceValidationTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostRepositoryConstraintTest.java`
- 구현된 범위:
  - 목록/상세/생성/수정/삭제, 플랫폼/모델/카테고리/태그 조회 API 유지
  - 생성/수정 입력을 `Map`에서 `PostUpsertRequest` + `@Valid`로 전환
  - Service 교차 필드 검증 강화:
    - 모델-플랫폼 일치 여부
    - `기타` 모델/카테고리 선택 시 `model_etc`/`category_etc` 필수
    - 비활성/폐기 모델 차단
  - DB 제약 보강: `satisfaction` 범위 CHECK 제약 추가
  - CRUD 전경로 컨트롤러 통합(MockMvc) 시나리오 검증 추가
    - create/list/detail/update/delete
    - 비작성자 update/delete 권한 차단
  - 목록/상세 응답 계약 필드 호환 검증 추가(필수 키 존재 확인)
  - DTO/Service/DB 3중 검증 테스트 근거 보강
- 아직 비어 있는 부분:
  - `[후속 backlog]` 프론트 실연동 E2E 기반 계약 필드 증빙 문서화
  - `[후속 backlog]` 계약 필드 변동 추적 문서/리포트 자동화
- 테스트 여부:
  - `PostUpsertRequestValidationTest` (DTO 유효성)
  - `PostServiceValidationTest` (교차 필드 규칙/권한 규칙)
  - `PostControllerSection4FlowTest.crud_fullFlow_and_contractFields`
  - `PostControllerSection4FlowTest.update_and_delete_forbidden_for_nonAuthor`
  - `PostControllerSection4FlowTest.create_rejects_invalidCombinations_fromDtoAndServiceValidation`
  - `PostRepositoryConstraintTest.save_throwsDataIntegrityViolation_whenSatisfactionOutOfRange`
  - `./gradlew test --tests "com.lshlabs.prompthubspring.post.*"` 통과
  - `./gradlew test` 통과
- DoD 충족 여부:
  - 충족
- troubleshooting 후보:
  - `예 (도메인 정합성/검증 파이프라인의 AI baseline review 서사 후보)`

## 섹션 5. 상호작용 API + 조회수 동시성

- 상태: `완료`
- 완료 판정: `완료` (완료/부분 완료/미완료 기준)
- 반영된 파일:
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostInteraction.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostInteractionRepository.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostRepository.java` (`incrementViewCount`, `findByIdForUpdate`)
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostInteractionConcurrencyTest.java`
- 구현된 범위:
  - like/bookmark 토글, liked/bookmarked/my-posts API
  - 조회수 증가 원자적 update 쿼리
  - 토글 구간 비관적 락(PESSIMISTIC_WRITE) 적용으로 동시 토글 직렬화
  - interaction 상태 count 기반 정합성 검증 쿼리(`countByPostAndLikedTrue`, `countByPostAndBookmarkedTrue`) 반영
- 아직 비어 있는 부분:
  - 극단적 고부하 환경에서의 성능 지표(TPS/P95/락 대기시간) 측정 문서화
- 테스트 여부:
  - `PostInteractionConcurrencyTest.concurrentToggle_keepsLikeAndBookmarkCountsConsistent`
  - `PostInteractionConcurrencyTest.concurrentDetail_incrementsViewCountWithoutLoss`
  - `PostInteractionConcurrencyTest.likedAndBookmarkedEndpoints_matchInteractionState`
  - `./gradlew test` 통과
- DoD 충족 여부:
  - 충족
- troubleshooting 후보:
  - `예(높음, AI baseline의 상태 변경/집계 변경 분리 구조를 Human Architect가 리뷰하며 발견한 정합성 위험 서사에 적합)`

## 섹션 6. 검색/필터/트렌딩 + 캐시

- 상태: `완료`
- 완료 판정: `완료` (완료/부분 완료/미완료 기준)
- 반영된 파일:
  - `backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/config/AppConfig.java` (`@EnableCaching`)
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostRepository.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostInteractionRepository.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreServiceSection6Test.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/stats/StatsServiceSection6Test.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreServiceSection6PerformanceTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/stats/StatsServiceSection6PerformanceTest.java`
  - `backend/SECTION6_PERFORMANCE_REPORT.md`
- 구현된 범위:
  - `core`/`stats` 로직을 컨트롤러에서 서비스(`CoreService`, `StatsService`)로 분리
  - 검색 API에 플랫폼/카테고리/만족도 범위 필터 조합 지원
  - 트렌딩 카테고리 랭킹 및 모델 정보/게시글 조회를 실제 데이터 기반으로 전환
  - 대시보드/사용자 통계를 placeholder에서 집계 기반 응답으로 전환
  - `@Cacheable(sync=true)` 기반 캐시 동작(트렌딩/대시보드) 반영
  - TPS/P95/cache hit ratio 정량 측정 테스트 추가 및 결과 리포트 반영
- 아직 비어 있는 부분:
  - `[후속 backlog]` QueryDSL 동적 조회 전환(현 단계는 JPA Specification 기반)
  - `[후속 backlog]` Redis 캐시 매니저/TTL 운영 프로파일 정교화
  - `[후속 backlog]` 실운영(Postgres/Redis) 환경 기준 외부 부하 도구(k6/JMeter) 검증
- 테스트 여부:
  - `CoreServiceSection6Test.search_supportsFiltersAndSatisfactionRange`
  - `CoreServiceSection6Test.categoryRankings_populatesTrendingCache`
  - `CoreServiceSection6Test.filterOptions_containsPlatformAndCategoryOptions`
  - `CoreServiceSection6PerformanceTest.search_reportsTpsAndP95Metrics`
  - `CoreServiceSection6PerformanceTest.trendingCache_reportsHitRatioAndWarmP95`
  - `StatsServiceSection6Test.dashboard_returnsAggregatedValuesAndCachesResult`
  - `StatsServiceSection6Test.userStats_returnsAuthorBasedAggregates`
  - `StatsServiceSection6PerformanceTest.dashboardCache_reportsHitRatioAndWarmP95`
  - `StatsServiceSection6PerformanceTest.userStats_reportsTpsAndP95Metrics`
  - `./gradlew test --tests "com.lshlabs.prompthubspring.core.CoreServiceSection6*" --tests "com.lshlabs.prompthubspring.stats.StatsServiceSection6*"` 통과
  - `./gradlew test` 통과
- DoD 충족 여부:
  - 충족
- troubleshooting 후보:
  - `예(높음, AI baseline review 기반 N+1 / cache stampede / query tuning 서사 적합)`

## 섹션 7. 호환성 정리/품질 게이트/2차 표준화 준비

- 상태: `완료`
- 완료 판정: `완료` (완료/부분 완료/미완료 기준)
- 반영된 파일:
  - `BACKEND_API_COMPAT_MATRIX.md`
  - `BACKEND_SECTION7_BACKLOG.md`
  - `docs/SECTION7_FRONTEND_FLOW_EVIDENCE.md`
  - `backend/src/test/java/com/lshlabs/prompthubspring/smoke/Section7ApiSmokeTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/smoke/Section7ContainerSmokeTest.java`
- 구현된 범위:
  - Django vs Spring API 호환 매트릭스를 섹션6/7 반영 상태로 갱신
  - 2차 표준화 후보 및 하드닝 백로그를 품질 게이트 기준으로 정리
  - MockMvc 기반 핵심 API 스모크 테스트 추가
    - register/login/refresh
    - posts list/detail/create
    - like/bookmark + liked/bookmarked
    - stats dashboard 보호 경로
  - Testcontainers(Postgres/Redis) 기반 스모크 테스트 보강
  - 프론트 핵심 플로우 API 레벨 증빙 문서 추가
  - 전체 테스트 통과 상태 유지(`./gradlew test`)
- 아직 비어 있는 부분:
  - `[후속 backlog]` 브라우저 UI 레벨 E2E(Playwright/Cypress) 증빙 강화
  - `[후속 backlog]` CI에서 스모크 스위트 분리 실행 및 품질 게이트 자동화
- 테스트 여부:
  - `Section7ApiSmokeTest.criticalFrontendFlow_login_list_detail_like_bookmark_smoke`
  - `Section7ApiSmokeTest.refreshTokenFlow_smoke`
  - `Section7ContainerSmokeTest.postgresAndRedisContainer_smokeFlow`
  - `./gradlew test --tests "com.lshlabs.prompthubspring.smoke.Section7ApiSmokeTest"` 통과
  - `./gradlew test --tests "com.lshlabs.prompthubspring.smoke.Section7ContainerSmokeTest"` 통과
  - `./gradlew test` 통과
- DoD 충족 여부:
  - 충족 (섹션7 릴리즈 게이트의 핵심 백엔드 증빙 항목 완료)
- troubleshooting 후보:
  - `예 (리팩터링/회고 정리 서사 후보)`

---

## 운영 규칙 고정 (다음 작업부터 적용)
- 다음 작업은 **현재 섹션 1개만** 진행
- 각 섹션 종료 시 아래 항목으로 완료 보고 후 사용자 확인 대기
  - 반영 파일
  - 구현 범위
  - 미완료 항목
  - 테스트 결과
  - DoD 충족 여부
- **사용자 승인 전 다음 섹션으로 진행 금지**
- 기본 모드는 정상 마이그레이션 유지
- troubleshooting/포트폴리오 문서는 사용자 명시 요청 또는 섹션 가치가 높은 경우에만 별도 생성
- troubleshooting 문서는 **구현 중 우연히 생긴 문제**보다 **AI baseline review 중 발견한 구조적 결함** 중심으로 작성
