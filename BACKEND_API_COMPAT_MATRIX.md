# Backend API Compatibility Matrix (Django -> Spring Boot)

Last Updated: 2026-03-26

## Auth
- `POST /api/auth/register/` : implemented
- `POST /api/auth/login/` : implemented
- `POST /api/auth/logout/` : implemented
- `POST /api/auth/token/refresh/` : implemented
- `POST /api/auth/google/` : implemented (Google tokeninfo verification)
- `GET /api/auth/profile/` : implemented
- `PUT/PATCH /api/auth/profile/` : implemented
- `PATCH /api/auth/profile/password/` : implemented
- `GET/PATCH/PUT /api/auth/profile/settings/` : implemented
- `POST /api/auth/profile/avatar/regenerate/` : implemented
- `GET /api/auth/profile/sessions/` : implemented
- `DELETE /api/auth/profile/delete/` : implemented
- `GET /api/auth/users/{username}/summary/` : implemented
- `GET /api/auth/info/` : implemented

## Posts
- `GET /api/posts/` : implemented
- `POST /api/posts/create/` : implemented
- `GET /api/posts/{id}/` : implemented
- `PUT/PATCH /api/posts/{id}/update/` : implemented
- `DELETE /api/posts/{id}/delete/` : implemented
- `POST /api/posts/{id}/like/` : implemented
- `POST /api/posts/{id}/bookmark/` : implemented
- `GET /api/posts/liked-posts/` : implemented
- `GET /api/posts/bookmarked-posts/` : implemented
- `GET /api/posts/my-posts/` : implemented
- `GET /api/posts/platforms/` : implemented
- `GET /api/posts/models/` : implemented
- `GET /api/posts/models/suggest/` : implemented
- `GET /api/posts/platforms/{platformId}/models/` : implemented
- `GET /api/posts/categories/` : implemented
- `GET /api/posts/tags/` : implemented

## Core
- `GET /api/core/health/` : implemented
- `GET /api/core/search/` : implemented (platform/category/satisfaction filtering supported)
- `GET /api/core/sort-options/` : implemented
- `GET /api/core/filter-options/` : implemented (platform/category options populated)
- `GET /api/core/trending/category-rankings/` : implemented (cached, service-based)
- `POST /api/core/trending/refresh-cache/` : implemented
- `GET /api/core/trending/model/{modelName}/posts/` : implemented (service-based)
- `GET /api/core/trending/model/{modelName}/info/` : implemented (service-based)

## Stats
- `GET /api/stats/dashboard/` : implemented (cached, aggregated values)
- `GET /api/stats/user/` : implemented (author-based aggregates)

## Section 7 Quality Gate
- MockMvc smoke tests: `backend/src/test/java/com/lshlabs/prompthubspring/smoke/Section7ApiSmokeTest.java`
- Testcontainers smoke tests: `backend/src/test/java/com/lshlabs/prompthubspring/smoke/Section7ContainerSmokeTest.java`
- Covered critical flow:
  - register/login token flow
  - posts list/detail/create
  - like/bookmark + liked/bookmarked list
  - stats dashboard auth-protected call
- Frontend flow evidence:
  - `docs/SECTION7_FRONTEND_FLOW_EVIDENCE.md`

## Notes
- Response shape is kept compatible-first with existing frontend endpoints.
- Section 6 이후 core/stats의 주요 placeholder는 실집계 기반으로 전환됨.
- Phase 2 표준화는 compatibility-first 정책을 유지한 상태에서 별도 백로그로 관리.
