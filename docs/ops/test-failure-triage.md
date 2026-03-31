# Backend Test Failure Triage (Deploy Gate)

작성일: 2026-03-31  
기준 실행: `cd backend && ./gradlew test`  
결과: 117 tests, 21 failed

## 배포 차단(Blocking)

다음은 사용자 핵심 플로우/계약에 직접 영향이 있어 배포 전 수정 권장:

1. `Section7ApiSmokeTest#criticalFrontendFlow_login_list_detail_like_bookmark_smoke`
2. `PostControllerSection4FlowTest` 계열
   - `crud_fullFlow_and_contractFields`
   - `create_rejects_invalidCombinations_fromDtoAndServiceValidation`
   - `selfLikeAndBookmark_followCompatibilityContract_messageAndNoCountIncrease`
   - `postsList_supportsCompatibilityQueryContract_sortSearchFilterExclude`
   - `suggestModels_appliesLegacyScoring_andReturnsSlugContract`
   - `update_and_delete_forbidden_for_nonAuthor`
3. `UserControllerSecurityTest` 계열
   - `profile_requiresAuthentication`
   - `protectedUserEndpoints_requireAuthentication`
4. `UserInfoContractParityTest` 계열
   - `userInfo_matchesCompatibilityContractFields`
   - `userInfo_requiresAuthentication`
   - `userInfo_returnsNullAvatarUrl_whenProfileImageMissing`
5. `UserControllerFlowTest` 계열
   - `authenticatedProfileSettingsInfoAndSessionsFlow_works`
   - `deletingOtherUsersSession_isRejected`
   - `endOtherSessions_matchesCompatibilityMessageAndCount`
   - `summary_returnsFrontendContractFields`
   - `deleteAccount_removesUserAndBlocksFollowUpProfileAccess`
6. `UserPasswordChangeTokenRotationFlowTest#passwordChange_revokesOldTokensAndIssuesNewTokens`
7. `UserPublicProfileSummaryMaskingFlowTest#summary_masksBio_whenPublicProfileDisabled_and_exposesWhenEnabledOrMissing`

## 비차단(Non-Blocking, 단 추적 필요)

1. `Section7ContainerSmokeTest#postgresAndRedisContainer_smokeFlow`
   - 로컬 개발/CI 인프라 차이에 따른 실패 가능성이 높음
   - 운영 배포 자체 차단으로는 보지 않되, 인프라 검증 파이프라인 보강 필요

2. `UserServiceTest#endOtherSessions_returnsRevokedCount`
   - 서비스 단위 검증 이슈로 기능 전체 중단과는 거리 있으나, 회귀 위험이 있어 보완 권장

## 권장 액션

1. 먼저 `Section7ApiSmokeTest` + `PostControllerSection4FlowTest` + `UserControllerSecurityTest`를 1차 복구
2. 계약 테스트(UserInfo/UserFlow/UserPasswordChange/UserPublicProfile) 2차 복구
3. 마지막으로 Container/서비스 단위 테스트 안정화

