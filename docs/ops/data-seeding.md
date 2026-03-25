# Data Seeding (Production DB)

이 문서는 운영/개발자용 초기 데이터 로딩 절차를 정리한 내부 참고 문서입니다.

## 개요

프로덕션 DB 초기 데이터 로딩은 로컬 환경에서 Render Postgres `External Database URL`로 접속해 `manage.py` 명령을 실행하는 방식으로 진행합니다.

## 사전 준비

- `backend/venv` 활성화
- `DJANGO_SETTINGS_MODULE=config.settings_prod`
- `DATABASE_URL`에 Render Postgres **External Database URL** 설정
- 로컬에서 실행 시 SSL 옵션 필요 시 `?sslmode=require` 추가

예시:

```bash
cd /path/to/prompthub2/backend
source venv/bin/activate

export DJANGO_SETTINGS_MODULE=config.settings_prod
export DATABASE_URL='postgresql://<USER>:<PASSWORD>@<HOST>.singapore-postgres.render.com/<DBNAME>?sslmode=require'
export SECRET_KEY='temp-local-seed-key'
export DJANGO_SECRET_KEY="$SECRET_KEY"
export DJANGO_ALLOWED_HOSTS='prompthub-backend-06wq.onrender.com'
export CORS_ALLOWED_ORIGINS='https://prompthub-frontend.onrender.com'
export CSRF_TRUSTED_ORIGINS='https://prompthub-frontend.onrender.com'
export DJANGO_DEBUG='False'
```

## 초기 데이터 로딩 순서

```bash
venv/bin/python manage.py load_categories
venv/bin/python manage.py load_ai_models --file posts/fixtures/platform_models.curated.json
venv/bin/python manage.py load_trending_data
venv/bin/python manage.py link_trending_models
```

## 샘플 사용자 / 게시글 (선택)

더미 사용자 10명 데이터 마이그레이션 적용:

```bash
venv/bin/python manage.py migrate users
```

샘플 게시글 생성 (관리자 제외, 최대 10명 사용자 라운드로빈 배정):

```bash
venv/bin/python manage.py generate_sample_posts --count 10
```

미리 확인만 할 경우:

```bash
venv/bin/python manage.py generate_sample_posts --count 10 --dry-run
```

## 검증

- Backend API
  - `/api/core/trending/category-rankings/`
  - `/api/posts/?page=1`
- Frontend
  - `/trending`
  - `/community`

## 주의사항

- 프로덕션 DB 연결 시 `DATABASE_URL` placeholder 문자열(`\<USER\>`, `\<PASSWORD\>`)을 그대로 쓰지 않기
- 비밀번호/secret은 문서에 기록하지 말고 로컬 env 또는 Render env만 사용
- Render Free 플랜에서는 cold start로 초기 응답이 느릴 수 있음
