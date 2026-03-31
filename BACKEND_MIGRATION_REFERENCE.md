# Backend Migration Reference

## 1) 목적
이 문서는 Django 기반 백엔드를 Spring Boot로 마이그레이션할 때,
"어디를 원본으로 보고", "무엇을 어떤 순서로 재설계할지"를 고정하기 위한 작업 기준서다.

## 2) 원본 참조 위치 (Source of Truth)
- 원본 프로젝트 루트: `/Users/mac/Documents/prompthub2`
- 원본 백엔드 루트: `/Users/mac/Documents/prompthub2/backend`
- API 엔트리 포인트: `/Users/mac/Documents/prompthub2/backend/config/urls.py`
- 앱별 라우팅:
  - `/Users/mac/Documents/prompthub2/backend/users/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/core/urls.py`
  - `/Users/mac/Documents/prompthub2/backend/stats/urls.py`

## 3) 타겟 구현 위치 (Spring)
- 타겟 백엔드 루트: `/Users/mac/Documents/prompthub-springboot/backend`
- 현재 진입점: `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/PrompthubSpringApplication.java`

권장 패키지 구조(초안):
- `com.lshlabs.prompthubspring.auth`
- `com.lshlabs.prompthubspring.user`
- `com.lshlabs.prompthubspring.post`
- `com.lshlabs.prompthubspring.core`
- `com.lshlabs.prompthubspring.stats`
- `com.lshlabs.prompthubspring.common` (예외/응답/유틸)

## 4) 기능 매핑 기준 (Django -> Spring)
- URL/View -> `@RestController`
- Serializer -> Request/Response DTO + Bean Validation
- Model -> JPA Entity
- Service (Python) -> Domain/Application Service (Java)
- 권한 검사/분기 로직 -> 기본은 Service 계층에서 처리하고, 필요한 경우 Method Security를 보조적으로 적용

## 5) API 군 우선순위 (구현 순서)
1. 인증/사용자 (`/api/auth/*`)
- Google OAuth 검증, 로그인/로그아웃, 프로필 조회/수정

2. 게시글 도메인 (`/api/posts/*`)
- 목록/상세/생성/수정/삭제
- 좋아요/북마크/내 글/내 좋아요/내 북마크

3. 탐색/트렌딩 (`/api/core/*`, `/api/stats/*`)
- 검색/필터/정렬 옵션
- 트렌딩 캐시 및 랭킹
- 대시보드/유저 통계

## 6) URL 매핑 초안
- Django `api/auth/google/` -> Spring `POST /api/auth/google`
- Django `api/posts/` -> Spring `GET /api/posts`
- Django `api/posts/create/` -> Spring `POST /api/posts`
- Django `api/posts/<post_id>/` -> Spring `GET /api/posts/{postId}`
- Django `api/posts/<post_id>/update/` -> Spring `PATCH /api/posts/{postId}`
- Django `api/posts/<post_id>/delete/` -> Spring `DELETE /api/posts/{postId}`
- Django `api/core/search/` -> Spring `GET /api/core/search`

참고: 1차 목표는 프론트 연동 성공이다. 기존 프론트가 실제 사용 중인 endpoint/response field는 우선 호환 유지한다.
REST 표준형 URI는 2차 리팩터링 대상으로 관리하며, 호환 유지 endpoint와 즉시 표준화 가능한 endpoint를 구분해서 진행한다.

## 7) 공통 규칙 (반드시 준수)
- 응답 포맷 통일: 성공/실패 envelope 정책 문서화 후 전 API 일괄 적용
- 예외 처리 통일: `@RestControllerAdvice`로 에러 코드/메시지 표준화
- 인증 통일: Google ID Token은 서버에서 검증하고, 이후 내부 인증은 Spring Security + 자체 JWT(Access/Refresh) 체계로 처리
- 페이징 통일: `page,size,sort` 또는 Cursor 방식 중 하나로 고정
- N+1 방지: 조회 API는 DTO projection/fetch 전략을 설계 후 구현

## 8) 데이터/스키마 이관 체크포인트
- Django `users/models.py`, `posts/models.py`, `stats/models.py` 기준으로 엔티티 작성
- 필수 제약조건(UNIQUE, NOT NULL, CHECK) 누락 금지
- 마이그레이션 전 DDL 설계 검토 문서 별도 유지

## 9) 검증(Definition of Done)
- 동일 기능 기준으로 Django vs Spring API 결과 비교표 작성
- 핵심 시나리오 테스트 통과:
  - 인증 성공/실패
  - 게시글 생성/수정 권한 검증
  - 좋아요/북마크 토글 정합성
  - 트렌딩/검색 성능 및 응답 형식
- 최소 품질 게이트:
  - `./gradlew test` 통과
  - 프론트 연동 smoke test 통과
- 단, 포트폴리오용 troubleshooting 시나리오에서 의도적으로 남긴 baseline flaw는 별도 리팩터링 단계에서 해결하며, 최종 완료 기준은 리팩터링 후 결과를 기준으로 판단한다.

## 10) 첫 구현 시작점 (이번 주)
1. Auth 도메인부터 시작
- 원본 참조: `users/urls.py`, `users/views.py`, `users/services/oauth_service.py`, `users/serializers.py`
- 산출물: `AuthController`, `GoogleTokenVerifier`, `AuthService`, `JwtProvider`

2. Post 목록/상세 API 연결
- 원본 참조: `posts/urls.py`, `posts/views.py`, `posts/services/post_service.py`
- 산출물: `PostController`, `PostService`, `PostRepository`, 조회 DTO

## 11) 변경 관리 규칙
- 프론트 계약(API Path/필드) 변경 시 반드시 문서 업데이트 + 프론트 동시 수정
- 결정사항은 이 문서에 날짜와 함께 누적 기록

---
Last Updated: 2026-03-25
Owner: PromptHub Spring Migration
