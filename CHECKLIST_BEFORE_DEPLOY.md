# PromptHub 배포 전 최종 점검 체크리스트 (Spring Boot 기준)

작성일: 2026-03-31

## Step 0. 현재 게이트 상태 확인

- [x] `cd backend && ./gradlew clean build -x test` 성공
- [x] `cd frontend && npm run typecheck && npm run build` 성공
- [x] 릴리즈 스모크 3종 성공
  - `ReleaseGateSmokeTest`
  - `TrailingSlashCompatibilitySmokeTest`
  - `Diff06ReleaseGateEvidenceVerificationTest`
- [ ] `cd backend && ./gradlew test` 전체 통과
  - 현재 117개 중 21개 실패 (배포 전 triage 필요)
  - 상세 분류: `docs/ops/test-failure-triage.md`

---

## Step 1. Supabase 운영 DB 스키마 반영

목표: 로컬에서 확정한 제약조건을 운영 DB(Supabase)에도 동일 반영

- [x] 운영 반영용 단일 SQL 준비
  - 파일: `prod_migration_v1.sql`
  - 포함:
    - 핵심 컬럼 `NOT NULL` 강제
    - `categories/platforms` UNIQUE 보장
    - `posts` 도메인 핵심 FK `RESTRICT` 전환 (운영 자산 보호 정책)
- [ ] Supabase SQL Editor에서 `prod_migration_v1.sql` 실행
- [ ] 실행 후 아래 검증 쿼리 확인
  - 제약조건 생성 여부
  - FK `ON DELETE RESTRICT` 적용 여부
  - 기존 데이터 위반 없음

---

## Step 2. Render 백엔드 배포 설정

목표: Spring Boot 서비스 기준으로 정확한 빌드/런타임 설정 적용

- [ ] Render Web Service 생성 (Java)
- [ ] Build Command
  - `./gradlew clean build -x test`
- [ ] Start Command
  - `java -jar build/libs/prompthub-spring-0.0.1-SNAPSHOT.jar`
  - 주의: `prompthubspring-...` 아님

### 필수 환경변수 (Render Backend)

- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `SPRING_DATASOURCE_URL=jdbc:postgresql://<SUPABASE_HOST>:5432/postgres?sslmode=require`
- [ ] `SPRING_DATASOURCE_USERNAME=<username>`
- [ ] `SPRING_DATASOURCE_PASSWORD=<password>`
- [ ] `APP_SECURITY_JWT_SECRET=<long-random-secret>`
- [ ] `BACKEND_GOOGLE_CLIENT_ID=<google-client-id>`

참고:
- 코드 기준 JWT 키는 `app.security.jwt.secret`이며, 운영 env는 `APP_SECURITY_JWT_SECRET`로 주입하는 것이 안전
- `JWT_SECRET` 단일 키는 현재 코드와 직접 매핑되지 않음

---

## Step 3. 프론트엔드 운영 연결값 반영

목표: 프론트가 새 Spring Backend(Render)를 바라보도록 전환

- [ ] 프론트 호스팅 환경변수 수정
  - `NEXT_PUBLIC_API_BASE_URL=https://<render-backend>.onrender.com`
  - `NEXT_INTERNAL_API_BASE_URL=https://<render-backend>.onrender.com`
  - `NEXTAUTH_URL=https://<frontend-domain>`
  - `NEXTAUTH_SECRET=<production-secret>`
  - `NEXT_PUBLIC_GOOGLE_CLIENT_ID=<google-client-id>`
  - `GOOGLE_CLIENT_SECRET=<google-client-secret>`
- [ ] 재배포 후 동작 확인
  - Google 로그인
  - 커뮤니티 목록/상세
  - 좋아요/북마크

---

## Step 4. 배포 차단 기준

아래 중 하나라도 미충족이면 배포 차단:

- [ ] Supabase 스키마 반영 실패/미검증
- [ ] Render env 키 오기입 (`JWT_SECRET`만 넣고 `APP_SECURITY_JWT_SECRET` 누락 등)
- [ ] 프론트 API URL 구키/로컬값 잔존
- [ ] 릴리즈 스모크 실패

---

## Step 5. 권장 릴리즈 순서

1. `prod_migration_v1.sql` Supabase 적용
2. Render backend 환경변수 반영 후 배포
3. 프론트 환경변수 전환/재배포
4. 스모크 플로우 확인 후 오픈

