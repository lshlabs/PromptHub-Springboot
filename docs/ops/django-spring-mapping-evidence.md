# Django ↔ Spring Mapping Evidence

## 목적
- VERIFY-03 완료 기준인 `Django 원본 경로 ↔ Spring 대응 경로 ↔ 테스트 링크 ↔ 판정 근거`를 기능별 단일 문서로 고정한다.

## [MAP-AUTH] 인증
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/users/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/users/views.py`
  - `/Users/mac/Documents/prompthub2/backend/users/services/oauth_service.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/auth/GoogleTokenVerifier.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/config/SecurityConfig.java`
- 테스트 링크:
  - `backend/src/test/java/com/lshlabs/prompthubspring/auth/AuthControllerTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/auth/GoogleTokenVerifierTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserControllerSecurityTest.java`
- 판정 근거:
  - 로그인/리프레시/Google 토큰 검증/보호 경로 인증 경계가 테스트로 연결됨.

## [MAP-USER] 사용자
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/users/views.py`
  - `/Users/mac/Documents/prompthub2/backend/users/serializers.py`
  - `/Users/mac/Documents/prompthub2/backend/users/services/*.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/UserController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/UserService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/UserMapper.java`
- 테스트 링크:
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserControllerFlowTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserPublicProfileSummaryMaskingFlowTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserInfoContractParityTest.java`
- 판정 근거:
  - 프로필/설정/세션/공개 프로필 마스킹/응답 계약이 기능 테스트로 매핑됨.

## [MAP-POST-CRUD] 게시글 CRUD
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/posts/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/views.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/services/post_service.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostUpsertRequest.java`
- 테스트 링크:
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostServiceValidationTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostUpsertRequestValidationTest.java`
- 판정 근거:
  - 생성/수정/삭제/검증 경계값/계약이 모두 테스트로 고정됨.

## [MAP-INTERACTION] 좋아요/북마크/조회수
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/posts/services/interaction_service.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/views.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostInteractionRepository.java`
- 테스트 링크:
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostInteractionConcurrencyTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostInteractionOrderingRepositoryTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`
- 판정 근거:
  - 토글/정렬/동시성 및 자기 글 상호작용 계약이 테스트로 연결됨.

## [MAP-CORE-STATS] 검색/필터/트렌딩/통계
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/core/views.py`
  - `/Users/mac/Documents/prompthub2/backend/core/services/*.py`
  - `/Users/mac/Documents/prompthub2/backend/stats/views.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsService.java`
- 테스트 링크:
  - `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreSearchContractParityTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreOptionsContractParityTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreTrendingModelParityTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/stats/StatsServiceSection6Test.java`
- 판정 근거:
  - 검색/필터/트렌딩/통계 핵심 API 계약과 결과 의미를 테스트로 매핑.

## [MAP-RELEASE] 호환성/릴리즈 게이트
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend` (실사용 플로우 전반)
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/**`
  - `frontend/app/**`
- 테스트 링크:
  - `backend/src/test/java/com/lshlabs/prompthubspring/smoke/Section7ApiSmokeTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/smoke/Section7ContainerSmokeTest.java`
- 판정 근거:
  - 릴리즈 전 게이트의 API/컨테이너 스모크 기준이 문서와 테스트로 연결됨.
