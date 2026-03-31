# API Contract Snapshots (Django ↔ Spring)

## 목적
- Django source of truth 기준의 주요 API 응답 shape를 Spring 응답과 함께 스냅샷 형태로 고정한다.
- 프론트가 의존하는 핵심 필드(`status`, `data`, `pagination`, `message`)를 회귀 검증 가능한 문서로 유지한다.

## [SNAP-AUTH] 인증
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/users/views.py`
  - `/Users/mac/Documents/prompthub2/backend/users/serializers.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/auth/AuthService.java`
- Endpoint: `POST /api/auth/login`
- Snapshot:
```json
{
  "message": "로그인에 성공했습니다.",
  "token": "<access-token>",
  "refresh": "<refresh-token>",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "username": "tester"
  }
}
```

## [SNAP-USER] 사용자 프로필
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/users/views.py`
  - `/Users/mac/Documents/prompthub2/backend/users/serializers.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/UserController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/user/UserService.java`
- Endpoint: `GET /api/users/profile`
- Snapshot:
```json
{
  "user": {
    "id": 1,
    "email": "user@example.com",
    "username": "tester"
  },
  "settings": {
    "public_profile": true,
    "email_notifications_enabled": true
  },
  "profile_completeness": {
    "percentage": 75.0,
    "completed_fields": 3,
    "total_fields": 4,
    "missing_fields": ["location"]
  }
}
```

## [SNAP-POST-LIST] 게시글 목록
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/posts/views.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/serializers.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
- Endpoint: `GET /api/posts`
- Snapshot:
```json
{
  "status": "success",
  "data": {
    "results": [
      {
        "post_id": 101,
        "title": "Prompt title",
        "like_count": 3,
        "bookmark_count": 1
      }
    ],
    "pagination": {
      "current_page": 1,
      "total_pages": 1,
      "total_count": 1,
      "has_next": false,
      "has_previous": false
    }
  }
}
```

## [SNAP-INTERACTION] 좋아요 토글
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/posts/views.py`
  - `/Users/mac/Documents/prompthub2/backend/posts/services/interaction_service.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/post/PostService.java`
- Endpoint: `POST /api/posts/{postId}/like`
- Snapshot:
```json
{
  "status": "success",
  "message": "좋아요가 등록되었습니다.",
  "data": {
    "is_liked": true,
    "like_count": 4
  }
}
```

## [SNAP-CORE] 트렌딩 카테고리 랭킹
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/core/views.py`
  - `/Users/mac/Documents/prompthub2/backend/core/services/trending_service.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/core/CoreController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/core/CoreService.java`
- Endpoint: `GET /api/core/trending/category-rankings`
- Snapshot:
```json
{
  "status": "success",
  "data": {
    "논리적추론": {
      "title": "논리적 추론 최고 모델",
      "subtitle": "GPQA Diamond 기준 (Best in Reasoning)",
      "icon": "Brain",
      "data": [
        { "rank": 1, "name": "GPT 5.2", "score": "92.4", "provider": "OpenAI" }
      ]
    }
  },
  "cached": true
}
```

## [SNAP-STATS] 대시보드 통계
- Django 원본 경로:
  - `/Users/mac/Documents/prompthub2/backend/stats/views.py`
- Spring 대응 경로:
  - `backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsController.java`
  - `backend/src/main/java/com/lshlabs/prompthubspring/stats/StatsService.java`
- Endpoint: `GET /api/stats/dashboard`
- Snapshot:
```json
{
  "status": "success",
  "data": {
    "total_posts": 10,
    "total_users": 11,
    "active_users": 2,
    "total_interactions": 3
  }
}
```
