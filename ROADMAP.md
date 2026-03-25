# PromptHub Roadmap

## 개요

- 목적: 배포 후 UX/운영 품질을 높이고, 다음 단계 기능(알림/보안/i18n 등)을 구현 가능한 수준으로 정리한다.
- 원칙: 작은 개선은 즉시 반영하고, 큰 기능은 API/DB/UI/테스트 기준까지 먼저 확정한다.

## 현재 배포 상태 (완료)

- Backend(Render) 배포 완료 / Health OK
- Frontend(Render) 배포 완료
- Google OAuth 로그인 동작 확인
- Cloudinary 프로필 이미지 업로드 확인
- 초기 카테고리/모델/트렌딩 데이터 로딩 완료
- 관리자 페이지 및 관리자 계정 생성 완료

## 즉시 개선 (이번 작업 범위)

- 프로필 위치 입력창 우측 GPS 버튼 추가 (브라우저 위치 + 역지오코딩)
- 비로그인 `/community`에서 `리뷰 공유하기` 클릭 시 로그인 모달 유도
- 사이트 아이콘(`icon.svg`) 추가 및 `metadata.icons` 연결
- 프로덕션 noisy 콘솔 로그 정리 (API/auth/community/create-post 핵심 구간)
- 배포 환경 브라우저 수동 재검증 (프로필 GPS / 커뮤니티 로그인 유도 / 파비콘 / 콘솔)

## 최근 반영 사항 (진행 기록)

- 사용자 메타데이터 팝오버(v1) 구현 + Playwright 실브라우저 검증 완료
- `/community` 검색 UX 개선
  - 작성자 검색(`search_type=author`) 지원
  - 검색 완료 뱃지(검색어 + 해제 `x`) 추가
  - 최근 검색어(localStorage) + 드롭다운/삭제
  - 검색 입력 `x` 버튼(원형 UI) 추가
- 팝오버 개선
  - hover + click 트리거(모바일은 click 유지)
  - hover open/close delay 적용
  - 팝오버 내부 클릭 시 카드 클릭 버블링 방지
  - 팝오버 `게시글` 클릭 → `/community` 작성자 검색 이동
- `/community` 전용 우측 하단 최상단 이동 버튼 추가
- 로딩 UX 정리
  - `/community` 상단 메타데이터 카드 skeleton
  - `/community` action 영역 skeleton
  - `/community` 게시글 카드 skeleton
  - `/trending` 랭킹/게시글 로딩 skeleton
  - 게시글 상세 페이지 skeleton
  - `/profile` 통계/게시글 영역 skeleton 스타일 정리
- 헤더 인증 영역 초기 로딩 UX 조정
  - auth pending 시 guest 버튼 플래시 억제
  - 헤더 우측 영역 지연 표시(로고 섹션은 즉시 표시)

## 중기 기능 로드맵 (TODO 5~11)

### 5) 사용자 메타데이터 간단 **팝오버**

#### 상태

- `v1 구현 완료` (백엔드 API + 공통 팝오버 + 주요 적용 지점 + Playwright 검증)

#### 목표

- 다른 사용자의 프로필 아이콘/닉네임 클릭 시 간단 정보 **팝오버** 표시

#### 백엔드 (v1)

- 신규 공개 API: `GET /api/auth/users/<username>/summary/`
- 응답 필드:
  - `username`
  - `bio` (공개 허용 범위 내)
  - `avatar_url`, `avatar_color1`, `avatar_color2`
  - `created_at`
  - `post_count`
  - `total_views`
  - `total_likes_received`
  - `total_bookmarks_received`

#### 백엔드 진행 현황

- `GET /api/auth/users/<username>/summary/` 구현 완료
- 존재하지 않는 사용자 `404` 처리 확인
- 테스트 추가 완료 (`users.tests`)

#### 프론트 (v1)

- `UserSummaryPopover` 공통 컴포넌트 추가
- 적용 순서:
  1. `PostCard`
  2. 게시글 상세 작성자 영역
  3. 기타 사용자 노출 영역

#### 프론트 진행 현황 (v1 완료 범위)

- `UserSummaryPopover` 공통 컴포넌트 구현 완료
- 적용 완료:
  1. `PostCard`
  2. 게시글 상세 작성자 영역
- 상호작용 개선:
  - 데스크톱 `hover + click`, 모바일 `click`
  - hover delay(open/close) 적용
  - 팝오버 내부 클릭 버블링 차단
  - 팝오버 `게시글` CTA → `/community` 작성자 검색 연동

#### 테스트

- 비로그인/로그인 상태에서 **팝오버** 오픈 가능 여부
- 존재하지 않는 username 처리 (404)
- 빈 bio/기록 없는 사용자 처리

#### 검증 현황

- Playwright 실브라우저로 팝오버 오픈/이동 동작 확인
- API 응답(200/404) 확인

---

### 6) 알림 기능 (DB + 폴링, v1)

#### 목표

- 내 게시글에 대한 좋아요/북마크/인기 게시글 알림 제공
- 우하단 토스트 + 프로필 페이지 누적 알림 목록 + 삭제
- 설정 페이지의 앱 내 알림 토글 연동

#### 구현 방식 (확정)

- **DB 저장 + 폴링**
- 웹소켓(실시간 푸시)은 후속 단계

#### 백엔드 설계

- 신규 모델 `Notification`
  - `recipient_user`
  - `actor_user` (nullable)
  - `event_type` (`like`, `bookmark`, `popular_post`)
  - `post` (nullable)
  - `payload` (JSON, optional)
  - `is_read`, `read_at`
  - `created_at`
  - `deleted_at` (soft delete) 또는 hard delete
- API
  - `GET /api/notifications/`
  - `GET /api/notifications/unread-count/`
  - `POST /api/notifications/<id>/read/`
  - `POST /api/notifications/read-all/`
  - `DELETE /api/notifications/<id>/`
- 생성 트리거
  - 좋아요/북마크 성공 시
  - 인기 게시글 조건 충족 시 (중복 알림 방지)

#### 프론트 설계

- `useNotifications` 훅 (폴링 30~60초, 탭 활성 시만)
- 신규 토스트 표시 (새 알림 1회)
- 프로필 페이지 알림 영역 (목록/읽음/삭제)
- `in_app_notifications_enabled` 토글과 동작 연동

#### 기본 인기 기준 (초안)

- `likes >= 10` 또는 `views >= 1000`
- 동일 게시글/이벤트는 1회만 발행

---

### 7) 이메일 전송 기능 (SMTP 기반, 무료 우선)

#### 목표

- 가입 축하 이메일
- 중요 알림 이메일

#### 구현 방식 (확정 방향)

- Django SMTP 이메일 백엔드
- 무료 우선: **Brevo SMTP free** 권장 (대안: Gmail App Password, Resend 후속)

#### 백엔드 설계

- env
  - `EMAIL_BACKEND`
  - `EMAIL_HOST`
  - `EMAIL_PORT`
  - `EMAIL_HOST_USER`
  - `EMAIL_HOST_PASSWORD`
  - `EMAIL_USE_TLS`
  - `DEFAULT_FROM_EMAIL`
- 유틸
  - `backend/core/email.py` 또는 `backend/users/email_service.py`
- 트리거
  - 회원가입 성공 후 welcome email
  - 중요 알림 생성 시 + `email_notifications_enabled=True`
- 실패 처리
  - 메일 발송 실패가 핵심 요청을 막지 않도록 예외 분리/로그 기록
  - v1은 sync, 후속 Celery 도입 검토

---

### 8) Google AdSense 도입 검토

#### 목표

- 여백 영역 활용 수익화 가능성 검토

#### 정책/UX 선행 조건

- 개인정보/쿠키 고지 강화
- 광고/분석 동의 UX 필요 여부 검토
- CLS 방지용 광고 슬롯 고정 높이 확보

#### 후보 위치

- `/community`: 리스트 중간/하단, 사이드 여백
- `/profile`: 하단 여백 영역

#### 계획

- v1 구현 보류 (트래픽/승인/정책 준비 후)
- 기능 플래그로 단계적 활성화

---

### 9) 2단계 인증 (TOTP 우선)

#### 목표

- 설정 페이지의 2FA 토글을 실제 기능과 연결

#### 구현 방식 (확정)

- **TOTP (Authenticator 앱 기반)**
- SMS/전화번호 인증은 후속

#### 백엔드 설계

- `UserSettings` 확장 또는 별도 모델
  - `two_factor_enabled`
  - `totp_secret` (암호화 저장)
  - `recovery_codes` (해시 저장)
- API
  - `POST /api/auth/profile/2fa/setup/`
  - `POST /api/auth/profile/2fa/verify/`
  - `POST /api/auth/profile/2fa/disable/`
  - `POST /api/auth/profile/2fa/recovery-codes/regenerate/`
- 로그인 흐름
  - 1차 인증(비밀번호) 성공 후 2FA 필요 상태 응답
  - OTP 검증 후 최종 토큰 발급

#### 프론트 설계

- 설정 > 보안 탭 단계형 UI
  - QR 표시
  - OTP 입력/검증
  - 비활성화
  - 복구코드 표시/재생성

---

### 10) 로그인 CAPTCHA (Cloudflare Turnstile)

#### 목표

- 로그인/회원가입 자동화 공격 완화

#### 구현 방식 (확정)

- **Cloudflare Turnstile**

#### 백엔드 설계

- env
  - `TURNSTILE_SECRET_KEY`
- 로그인 API에서 Turnstile 토큰 검증 후 로그인 진행
- 필요 시 회원가입/비밀번호 변경에도 확장

#### 프론트 설계

- env
  - `NEXT_PUBLIC_TURNSTILE_SITE_KEY`
- 로그인 폼에 Turnstile 위젯 추가
- Google 로그인 버튼과 충돌 없는 레이아웃 배치

---

### 11) 다크모드 + i18n (한국어/영어 1차)

#### 목표

- 헤더 모달의 테마/언어 설정을 실제 기능과 연결

#### 테마 (v1)

- 라이브러리: `next-themes` 권장
- `light/dark/system` 적용
- `html` class 기반 전역 테마 처리
- 기존 `localStorage theme` 값 마이그레이션

#### i18n (v1)

- 범위: **한국어/영어 2개**
- 라이브러리: `next-intl` 권장 (App Router 친화)
- 1차 대상
  - 헤더/푸터
  - 로그인/회원가입 핵심 문구
  - 프로필/커뮤니티 핵심 버튼/레이블

#### 후속

- 일본어/중국어 확장
- 번역 키 구조 정리 및 자동 검증

## 우선순위 / 단계 제안

### P0 (짧게)

- 프로덕션 콘솔 로그 추가 정리 (잔여 noisy 로그)
- 배포환경 UX 점검 및 회귀 테스트

### P1

- 사용자 메타데이터 팝오버 (`v1 완료`, 기타 사용자 노출 영역 확대 적용 잔여)
- 알림 기능 v1 (DB+폴링)
- 가입 축하 이메일

### P2

- Turnstile CAPTCHA
- TOTP 2FA

### P3

- 다크모드/언어 전환(ko/en)
- AdSense 검토/실험

## 테스트/검증 체크리스트

- 프로필 GPS 버튼 권한 허용/거부/타임아웃 케이스
- 비로그인 커뮤니티 `리뷰 공유하기` → 로그인 모달 → 로그인 성공 후 작성 다이얼로그
- 파비콘 표시 (`/icon.svg`) 및 탭 아이콘 확인
- 프로덕션 콘솔에서 디버그 로그 억제 확인
- 트렌딩/커뮤니티/프로필 기본 배포 동작 회귀 테스트

## 운영/보안 TODO

- Render Postgres 비밀번호 rotate (이미 노출 이력 있음)
- Google OAuth Client Secret rotate (노출 이력 있음)
- 메일 발송용 SMTP 계정/도메인 정책 정리
- 로그 정책 정리 (PII/민감정보 비출력)

