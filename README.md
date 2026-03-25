# PromptHub

AI 프롬프트/모델 사용 경험을 공유하는 커뮤니티 플랫폼입니다.
백엔드는 Django REST API, 프론트엔드는 Next.js(App Router)로 구성되어 있습니다.

## Live Demo

- Frontend: `https://prompthub-frontend.onrender.com`
- Backend API: `https://prompthub-backend-06wq.onrender.com`
- Health Check: `https://prompthub-backend-06wq.onrender.com/api/core/health/`

## Status

- Production 배포 완료 (Render + Render Postgres + Cloudinary)
- Google 로그인 동작 확인
- 프로필 이미지 업로드(Cloudinary) 동작 확인
- Django 관리자 페이지 접근 확인

## Tech Stack

- Backend: Django 5.2, Django REST Framework
- Frontend: Next.js 15, React, TypeScript, Tailwind CSS
- Auth: Django Token Auth + NextAuth (Google OAuth)
- Infra: Render (Frontend/Backend/Postgres), Cloudinary
- Dev Infra: Docker Compose (frontend + backend)

## Core Features

- 회원가입 / 로그인 / Google 로그인
- 프롬프트 게시글 CRUD
- 좋아요 / 북마크 / 프로필 / 계정 설정
- 검색 / 정렬 / 필터
- 트렌딩 랭킹 + 연관 게시글
- 프로필 이미지 업로드 (Cloudinary)

## Project Structure

```text
.
├── backend/          # Django API
├── frontend/         # Next.js App Router
├── docs/             # 운영/참고 문서
├── scripts/          # local dev scripts
├── ROADMAP.md        # product/engineering roadmap
├── docker-compose.yml
└── README.md
```

## Quick Start (Local)

### 1) Backend

```bash
python3 -m venv backend/venv
source backend/venv/bin/activate
pip install -r backend/requirements.txt
cd backend
python manage.py migrate
python manage.py runserver
```

### 2) Frontend

```bash
# repo root
npm install
cd frontend
npm run dev
```

### 3) Open

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8000`
- Admin: `http://localhost:8000/admin/`
- Health: `http://localhost:8000/api/core/health/`

## Docker (Development)

로컬에서 프론트/백엔드를 컨테이너로 함께 실행할 수 있습니다.

### 1) Root `.env` 준비 (gitignored)

```env
NEXT_PUBLIC_GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
NEXTAUTH_SECRET=replace-with-random-secret
NEXTAUTH_URL=http://localhost:3000
```

값 위치:
- `NEXT_PUBLIC_GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`: Google Cloud Console OAuth Client (또는 `frontend/.env.local` 값)
- `NEXTAUTH_SECRET`: 랜덤 문자열 (`python3 -c "import secrets; print(secrets.token_urlsafe(50))"`)
- `NEXTAUTH_URL`: 로컬 Docker 실행 시 `http://localhost:3000`

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

## Production Deployment (Current)

- Backend: Render Web Service (`backend` root, `gunicorn`)
- Frontend: Render Web Service (`frontend` root, `next build && next start`)
- DB: Render Postgres
- Media: Cloudinary

## Environment Variables (Summary)

### Frontend (Render)

- `NEXT_PUBLIC_API_BASE_URL`
- `NEXT_INTERNAL_API_BASE_URL`
- `NEXTAUTH_URL`
- `NEXTAUTH_SECRET`
- `NEXT_PUBLIC_GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

### Backend (Render)

- `DJANGO_SETTINGS_MODULE=config.settings_prod`
- `DATABASE_URL`
- `SECRET_KEY`
- `DJANGO_SECRET_KEY`
- `DJANGO_ALLOWED_HOSTS`
- `CORS_ALLOWED_ORIGINS`
- `CSRF_TRUSTED_ORIGINS`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

## Useful Commands

- `./scripts/start-dev.sh` - backend + frontend 동시 실행
- `cd frontend && npm run typecheck`
- `cd frontend && npm run build`
- `cd backend && python manage.py test`

## Docs

- Roadmap: `ROADMAP.md`
- Ops (data seeding): `docs/ops/data-seeding.md`

## Notes

- Render Free 플랜 특성상 초기 응답이 느릴 수 있습니다 (cold start).
