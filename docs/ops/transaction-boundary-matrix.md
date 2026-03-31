# Transaction Boundary Matrix (Django ↔ Spring)

## 목적
- Django source of truth 기준의 상태 변경(write) 경계를 Spring `@Transactional` 경계와 1:1 의미로 매핑한다.
- 읽기(read) API와 상태 변경(write) API를 분리해 회귀 검증 기준을 고정한다.

## Source of Truth
- Django:
  - `/Users/mac/Documents/prompthub2/backend/users/services/*.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/services/*.py`
  - `/Users/mac/Documents/prompthub2/backend/core/services/*.py`
- Spring:
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthService.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/user/UserService.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`

## Write Path Matrix

### AuthService
- `register` -> write path (`users`, `auth_tokens`) -> `@Transactional`
- `login` -> write path (`auth_tokens`, `user_sessions`) -> `@Transactional`
- `loginWithGoogle` -> write path (`users`, `auth_tokens`, `user_sessions`) -> `@Transactional`
- `refresh` -> write path (`auth_tokens`) -> `@Transactional`
- `logout` -> write path (`auth_tokens`, `user_sessions`) -> `@Transactional`
- `rotateTokenPair` -> write path (`auth_tokens`) -> `@Transactional`

### UserService
- `updateProfile` -> write path (`users`) -> `@Transactional`
- `changePassword` -> write path (`users`, `auth_tokens`) -> `@Transactional`
- `updateSettings` -> write path (`user_settings`) -> `@Transactional`
- `endSession` -> write path (`user_sessions`) -> `@Transactional`
- `endOtherSessions` -> write path (`user_sessions`) -> `@Transactional`
- `regenerateAvatar` -> write path (`users`) -> `@Transactional`
- `deleteAccount` -> write path (`auth_tokens`, `user_sessions`, `user_settings`, `users`) -> `@Transactional`

### PostService
- `detail` -> write path (`posts.view_count`) -> `@Transactional`
- `create` -> write path (`posts`) -> `@Transactional`
- `update` -> write path (`posts`) -> `@Transactional`
- `delete` -> write path (`posts`) -> `@Transactional`
- `toggleLike` -> write path (`post_interactions`, `posts.like_count`) -> `@Transactional`
- `toggleBookmark` -> write path (`post_interactions`, `posts.bookmark_count`) -> `@Transactional`
