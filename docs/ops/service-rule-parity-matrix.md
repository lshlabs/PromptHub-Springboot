# Service Rule Parity Matrix (Django ↔ Spring)

## 목적
- Django source of truth의 서비스 계층 규칙/상태 전이/집계 로직을 Spring 서비스 코드 및 테스트 링크와 1:1로 매핑한다.
- SERVICE-02 완료 기준인 "기능별 규칙표 + 케이스 테스트 링크"를 단일 문서로 고정한다.

## [SR-USER] 사용자 서비스 규칙
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/users/services/*.py`
  - `/Users/mac/Documents/prompthub2/backend/users/views.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/UserService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/UserController.java`
- 핵심 규칙:
  - 프로필 수정 시 username 중복 금지/공백 금지
  - 비밀번호 변경 시 현재 비밀번호 검증 + 토큰 회전
  - 공개 프로필(`public_profile`) 마스킹 규칙 적용
  - 세션 종료/기타 세션 종료 상태 전이
- 테스트 링크:
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserControllerFlowTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserPublicProfileSummaryMaskingFlowTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/user/UserPasswordChangeTokenRotationFlowTest.java`
- 판정 근거:
  - 프로필/세션/비밀번호/마스킹 규칙이 테스트로 고정되어 회귀 가능.

## [SR-POST] 게시글 서비스 규칙
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/posts/services/post_service.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/models.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostUpsertRequest.java`
- 핵심 규칙:
  - 플랫폼/모델/카테고리 조합 검증
  - 만족도 범위 및 0.5 step 검증
  - 태그 개수/빈값/중복 처리 규칙
  - 게시글 수정/삭제 권한 및 상태 전이
- 테스트 링크:
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostServiceValidationTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostUpsertRequestValidationTest.java`
- 판정 근거:
  - DTO/Service/DB 제약 경계가 테스트로 검증되어 도메인 규칙을 유지함.

## [SR-INTERACTION] 상호작용 서비스 규칙
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/posts/services/interaction_service.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/views.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostInteractionRepository.java`
- 핵심 규칙:
  - 좋아요/북마크 토글 시 카운트 정합성 유지
  - 자기 글 상호작용 메시지 계약 유지
  - 조회수 증가 시 유실 업데이트 방지
- 테스트 링크:
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostInteractionConcurrencyTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostInteractionOrderingRepositoryTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/post/PostControllerSection4FlowTest.java`
- 판정 근거:
  - 동시성/정렬/메시지 계약이 테스트로 고정됨.

## [SR-CORE-STATS] 검색/트렌딩/통계 서비스 규칙
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/core/services/*.py`
  - `/Users/mac/Documents/prompthub2/backend/stats/views.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsService.java`
- 핵심 규칙:
  - 검색/필터/정렬 결과 계약
  - 트렌딩 모델 연관 조회 규칙(`related_model`, `use_exact_matching`)
  - 대시보드/유저 통계 집계 계약
- 테스트 링크:
  - `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreSearchContractParityTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreTrendingModelParityTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/core/CoreOptionsContractParityTest.java`
  - `backend/src/test/java/com/lshlabs/prompthubspring/stats/StatsServiceSection6Test.java`
- 판정 근거:
  - 주요 조회/집계 API가 계약 테스트로 연결되어 규칙 추적 가능.
