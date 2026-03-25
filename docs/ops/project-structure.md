# 프로젝트 구조 기준 (Prompthub)

이 문서는 "어떤 파일을 어디에 둘지"에 대한 최소 기준을 정리한 운영/개발자용 규칙입니다.
목표는 파일 수를 줄이는 것이 아니라, 탐색 비용을 줄이고 임시 파일/중복 파일이 루트에 쌓이지 않게 하는 것입니다.

## 기본 원칙

- `backend/`: Django 서버 코드, 모델/뷰/API/관리 명령
- `frontend/`: Next.js UI 코드, 훅, 타입, 공용 유틸
- `docs/`: 운영/설계/내부 참고 문서
- 루트(`/`): 프로젝트 진입 문서와 실행 설정만 유지

## 프론트 구조 기준 (통일 전략)

현재 프로젝트는 `**components` 중심 전략으로 통일**합니다.

- `frontend/components/`
  - 화면/도메인 UI 컴포넌트
  - 예: `community/`, `profile/`, `posts/`, `trending/`, `layout/`, `common/`
- `frontend/components/ui/`
  - 공용 UI primitive (버튼, 입력, 다이얼로그 등)
- `frontend/app/`
  - 라우트 페이지, 레이아웃, 페이지 조합 로직
- `frontend/hooks/`
  - 재사용 가능한 훅
- `frontend/lib/`
  - 공용 유틸, API 클라이언트, 로거, 헬퍼
- `frontend/types/`
  - 타입 정의

### `features/` 정책

- `frontend/features/`는 현재 사용하지 않으므로 제거
- 앞으로도 새로운 기능을 추가할 때 기본 위치는 `components + hooks + lib + app` 조합으로 구성
- feature 단위로 다시 분리해야 할 정도로 커지면, 별도 문서 합의 후 도입

## 백엔드 구조 기준

- `backend/users/`, `backend/posts/`, `backend/core/`, `backend/stats/`
  - Django 앱 단위 책임 유지
- `backend/*/management/commands/`
  - 운영/시드/데이터 로딩 명령
- `backend/*/migrations/`
  - 스키마/데이터 마이그레이션
- `backend/config/`
  - settings/urls/wsgi/asgi 등 프로젝트 설정

## 루트 디렉터리 정책 (최소 유지)

루트에는 아래만 두는 것을 기본으로 합니다.

- `README.md`
- `ROADMAP.md`
- `docker-compose.yml`
- `docker-compose.override.yml`
- `.env.example` (필요 시)
- 패키지 매니저/런타임 파일 (`package.json`, `package-lock.json`, `.python-version`)
- 프로젝트 루트 실행용 디렉터리 (`backend/`, `frontend/`, `docs/`, `scripts/`)

### 루트에 두지 않는 것

- 임시 파일 (`tmp-`*, `*.txt` 실험 파일)
- 다이어그램 산출물/이미지 (필요 시 `docs/` 하위)
- 내부 진행 메모/툴 잔재 (필요 시 `docs/` 이동)
- 테스트 업로드 이미지/로컬 미디어 파일

### 현재 예외 (임시 유지)

- `PORTFOLIO_CORE_FEATURES.md`
  - 사용자가 나중에 읽기 위해 일시 유지
  - 장기적으로는 `docs/` 하위로 이동 권장

## 문서 배치 기준

- `README.md`: 외부 공개용 요약 (프로젝트 소개, 실행법, 배포 URL)
- `ROADMAP.md`: 중장기 기능 계획
- `docs/ops/`: 운영/배포/시드/구조 기준 문서
- `docs/`의 상세 문서는 README에서 링크만 제공 (README 비대화 방지)

## 환경변수 파일 기준

- `/.env` : Docker Compose 로컬 실행용 (gitignore)
- `backend/.env` : Django 로컬 실행용 (gitignore)
- `frontend/.env.local` : Next.js 로컬 개발용 (gitignore)
- `backend/.env.example`, `frontend/.env.example` : 공유 가능한 템플릿 (git tracked)

## 생성물/캐시/로컬 파일 정책

- `frontend/.next/`, `node_modules/`, `__pycache__/`, `*.tsbuildinfo`는 생성물로 취급
- 로컬 업로드 파일/미디어는 버전관리 대상에서 제외 (`backend/media/`)
- 정리 대상인지 애매하면 먼저 "코드 참조 여부"를 확인 후 삭제

## 정리 체크리스트 (작업 후)

- 새 파일이 올바른 디렉터리에 있는가?
- 임시/실험 파일이 루트에 생기지 않았는가?
- `public/` 정적 자산은 실제 참조되는가?
- 템플릿 env 파일과 실제 시크릿 파일을 구분했는가?
- README에 넣을지, `docs/`로 보낼지 판단했는가?

