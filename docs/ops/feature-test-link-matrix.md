# Feature ↔ Test Link Matrix

## 목적
- Django source of truth 기능 경로와 Spring 테스트 경로를 기능 단위로 1:1 연결한다.
- VERIFY-01 완료 기준인 "기능↔테스트 링크 표"를 단일 문서로 고정한다.

## [AUTH] 인증
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/users/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/users/views.py`
  - `/Users/mac/Documents/prompthub2/backend/users/services/oauth_service.py`
- Spring 테스트 경로:
  - `backend/src/test/java/com/lshlabs/prompthubspring/auth/AuthControllerTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/auth/AuthServiceRefreshTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/auth/GoogleTokenVerifierTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserControllerSecurityTest.java`

## [USER] 사용자
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/users/views.py`
  - `/Users/mac/Documents/prompthub2/backend/users/serializers.py`
  - `/Users/mac/Documents/prompthub2/backend/users/services/*.py`
- Spring 테스트 경로:
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserControllerFlowTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserPublicProfileSummaryMaskingFlowTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserInfoContractParityTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserPasswordChangeTokenRotationFlowTest.java`

## [POST-CRUD] 게시글 CRUD/검증
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/posts/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/views.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/services/post_service.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/models.py`
- Spring 테스트 경로:
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostServiceValidationTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostUpsertRequestValidationTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostRepositoryConstraintTest.java`

## [INTERACTION] 좋아요/북마크/조회수
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/posts/views.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/services/interaction_service.py`
- Spring 테스트 경로:
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostInteractionConcurrencyTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostInteractionOrderingRepositoryTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`

## [CORE-STATS] 검색/필터/트렌딩/통계
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/core/views.py`
  - `/Users/mac/Documents/prompthub2/backend/core/services/*.py`
  - `/Users/mac/Documents/prompthub2/backend/stats/views.py`
- Spring 테스트 경로:
  - `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreSearchContractParityTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreOptionsContractParityTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreTrendingModelParityTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreTrendingCacheParityTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/stats/StatsServiceSection6Test.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/stats/StatsServiceSection6PerformanceTest.java`

## [RELEASE-GATE] 스모크/릴리즈 게이트
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend` (실사용 플로우 전체)
- Spring 테스트 경로:
  - `backend/src/test/java/com/lshlabs/prompthubspring/smoke/Section7ApiSmokeTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/smoke/Section7ContainerSmokeTest.java`

## [ENTITY-TRANSACTION] 제약/트랜잭션 회귀
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/posts/models.py`
  - `/Users/mac/Documents/prompthub2/backend/core/models/trending.py`
  - `/Users/mac/Documents/prompthub2/backend/users/services/*.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/services/*.py`
- Spring 테스트 경로:
  - `backend/src/test/java/com/lshlabs/prompthubspring/entity/EntityConstraintParityTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/transaction/ServiceTransactionBoundaryTest.java`
