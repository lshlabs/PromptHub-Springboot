# PromptHub

AI 프롬프트/모델 사용 경험을 공유하는 커뮤니티 플랫폼입니다.  
프론트엔드는 Next.js(App Router), 백엔드는 Spring Boot REST API로 구성되어 있습니다.

## Live Demo

- Frontend: `https://prompthub-frontend.onrender.com`
- Backend API: `https://prompthub-backend-06wq.onrender.com`
- Health Check: `https://prompthub-backend-06wq.onrender.com/api/core/health/`

## Architecture

- Frontend: Next.js 15, React, TypeScript, Tailwind CSS
- Backend: Spring Boot 3.x, Spring Data JPA, PostgreSQL
- Auth:
  - Frontend 세션 오케스트레이션: NextAuth (Google OAuth)
  - Backend 인증/인가: Spring Security + 내부 토큰 검증 체계
- Infra: Render (Frontend/Backend), PostgreSQL, Cloudinary

## Core Features

- 이메일 로그인 / Google 로그인
- 프롬프트 게시글 CRUD
- 좋아요 / 북마크 / 프로필 / 계정 설정
- 검색 / 정렬 / 필터
- 트렌딩 랭킹 + 연관 게시글
- 프로필 이미지 업로드 (Cloudinary)

## Project Structure

```text
.
├── backend/          # Spring Boot API
├── frontend/         # Next.js App Router
├── docs/             # 운영/검증/참고 문서
├── scripts/          # local dev scripts
├── ROADMAP.md
├── docker-compose.yml
└── README.md
```

## Quick Start (Local)

### 1) Backend

```bash
cd backend
./gradlew bootRun
```

### 2) Frontend

```bash
cd frontend
npm install
npm run dev
```

### 3) Open

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8000`
- Health: `http://localhost:8000/api/core/health/`

## Docker (Development)

### 1) Root `.env` 준비 (gitignored)

```env
NEXT_PUBLIC_GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
NEXTAUTH_SECRET=replace-with-random-secret
NEXTAUTH_URL=http://localhost:3000
```

### 2) 실행

```bash
docker compose up --build
```

### 3) 정지

```bash
docker compose down --remove-orphans
```

Docker 실행 후 접속 주소:
- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8000`
- Health: `http://localhost:8000/api/core/health/`

## Environment Variables (Summary)

### Frontend

- `NEXT_PUBLIC_API_BASE_URL`
- `NEXT_INTERNAL_API_BASE_URL`
- `NEXTAUTH_URL`
- `NEXTAUTH_SECRET`
- `NEXT_PUBLIC_GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

### Backend

- `SPRING_PROFILES_ACTIVE`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_SECURITY_JWT_SECRET`
- `BACKEND_GOOGLE_CLIENT_ID` (또는 `NEXT_PUBLIC_GOOGLE_CLIENT_ID` fallback)

## Useful Commands

- `./scripts/start-dev.sh` - backend + frontend 동시 실행
- `cd frontend && npm run typecheck`
- `cd frontend && npm run build`
- `cd backend && ./gradlew test`

## Docs

- Roadmap: `ROADMAP.md`
- Ops (data seeding): `docs/ops/data-seeding.md`

## Notes

- Render Free 플랜 특성상 초기 응답이 느릴 수 있습니다 (cold start).
