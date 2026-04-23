# PromptHub (Spring Boot Migration)

AI 프롬프트/모델 사용 경험을 공유하는 커뮤니티 플랫폼입니다.  
프론트엔드는 Next.js(App Router), 백엔드는 Spring Boot REST API로 구성되어 있습니다.

## Migration Context

- Original project: `https://github.com/lshlabs/PromptHub`
- 이 저장소는 기존 프로젝트의 백엔드를 Spring Boot로 마이그레이션한 버전입니다.
- 목표: 기존 API 계약과 핵심 사용자 흐름의 동작 호환성 유지

## Deployment

- Frontend: `https://prompthub-sb-frontend.onrender.com`
- Backend API: `https://prompthub-sb-backend.onrender.com`
- Health Check: `https://prompthub-sb-backend.onrender.com/api/core/health`

## Tech Stack

- Frontend: Next.js 15, React 18, TypeScript, Tailwind CSS
- Backend: Spring Boot 3.5, Spring Security, Spring Data JPA
- Database: PostgreSQL (local/dev)
- Auth:
  - Frontend: NextAuth (Google OAuth)
  - Backend: JWT + Spring Security
- Infra: Render, Supabase, Cloudinary

## Project Structure

```text
.
├── backend/                     # Spring Boot API
│   ├── src/main/resources/      # application*.yml
│   └── compose.yaml             # local postgres
├── frontend/                    # Next.js App Router
└── README.md
```

## Prerequisites

- Java 21
- Node.js 20+
- Docker (선택: 로컬 PostgreSQL 실행 시)

## Quick Start (Local)

### 1) 환경 변수 설정

루트(`./.env`) 또는 `backend/.env`에 백엔드용 변수 추가:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/prompthub
SPRING_DATASOURCE_USERNAME=myuser
SPRING_DATASOURCE_PASSWORD=secret
APP_JWT_SECRET=replace-with-strong-secret
BACKEND_GOOGLE_CLIENT_ID=your-google-client-id
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
```

`frontend/.env.local`에 프론트엔드용 변수 추가:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8000
NEXT_INTERNAL_API_BASE_URL=http://localhost:8000
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=replace-with-random-secret
NEXT_PUBLIC_GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
```

### 2) PostgreSQL 실행 (선택)

```bash
docker compose -f backend/compose.yaml up -d
```

### 3) Backend 시작

```bash
cd backend
./gradlew bootRun
```

### 4) Frontend 시작

```bash
cd frontend
npm install
npm run dev
```

### 5) 동작 확인

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8000`
- Health: `http://localhost:8000/api/core/health`

## Test & Build

- Backend 테스트: `cd backend && ./gradlew test`
- Frontend 타입체크: `cd frontend && npm run typecheck`
- Frontend 빌드: `cd frontend && npm run build`

## Notes

- `NEXT_PUBLIC_API_BASE_URL`는 필수입니다. 값이 없으면 프론트가 API 호출 시 에러를 발생시킵니다.
- Render/Supabase Free 플랜 특성상 초기 응답이 느릴 수 있습니다 (cold start).

## AI Collaboration (Codex)

- 본 마이그레이션은 Codex 기반 AI 협업으로 진행했습니다.
