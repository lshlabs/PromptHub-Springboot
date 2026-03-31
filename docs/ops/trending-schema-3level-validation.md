# Trending 도메인 스키마 검증 (3단계 판정)

## 1) 목적 / 판정 체계

이 문서는 `nullable / PK / FK`를 Django 일치 여부만으로 판단하지 않고,
아키텍처/실기능 타당성까지 포함해 아래 3단계로 판정한다.

- `A`: 일치 + 타당 (유지)
- `B`: 일치하지만 개선 필요 (호환 유지 + 개선 과제)
- `C`: 불일치 + 부적합 (즉시 수정 권고)

## 2) 근거 경로 (source of truth)

- Django:
  - `/Users/mac/Documents/prompthub2/backend/core/models/trending.py`
  - `/Users/mac/Documents/prompthub2/backend/core/migrations/0001_initial.py`
  - `/Users/mac/Documents/prompthub2/backend/core/migrations/0002_add_related_model_to_trending.py`
- Spring:
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/TrendingCategoryEntity.java`
  - `/Users/mac/Documents/prompthub-springboot/backend/src/main/java/com/lshlabs/prompthubspring/core/TrendingRankingEntity.java`
- DB 증빙:
  - `information_schema.columns`
  - `information_schema.table_constraints`
  - `information_schema.referential_constraints`
  - DB: `localhost:5432 / prompthub`

## 3) 테이블별 검증 결과

## 3.1 `trending_categories`

- 검증 항목: `trending_categories.id (PK)`
  - 현재 DB 상태: PK 존재
  - Django 기준: PK 자동 생성
  - Spring 기준: `@Id @GeneratedValue`
  - 기능상 영향: 카테고리 식별 안정성
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `trending_categories.name nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `CharField(max_length=50, unique=True)` (필수)
  - Spring 기준: `@Column(nullable=false, unique=true, length=50)`
  - 기능상 영향: 이름 없는 트렌딩 카테고리 허용(도메인 위반)
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `trending_categories.title nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `CharField(max_length=100)` (필수)
  - Spring 기준: `@Column(nullable=false, length=100)`
  - 기능상 영향: 표시 제목 누락 시 UI 카드/랭킹 영역 깨짐
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `trending_categories.subtitle nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `CharField(max_length=200)` (필수)
  - Spring 기준: `@Column(nullable=false, length=200)`
  - 기능상 영향: 서브타이틀 누락 시 설명 UI 손실
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `trending_categories.icon_name nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `CharField(max_length=50)` (필수)
  - Spring 기준: `@Column(nullable=false, length=50)`
  - 기능상 영향: 아이콘 미지정 시 UI 깨짐
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `trending_categories.order nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `PositiveIntegerField(default=0)` (필수)
  - Spring 기준: `@Column(name="\"order\"", nullable=false)`
  - 기능상 영향: 정렬 기준 안정성
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `trending_categories.is_active nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `BooleanField(default=True)` (필수)
  - Spring 기준: `@Column(nullable=false)`
  - 기능상 영향: 노출/비노출 제어 안정성
  - 판정: `A`
  - 조치 권고: 유지

## 3.2 `trending_rankings`

- 검증 항목: `trending_rankings.id (PK)`
  - 현재 DB 상태: PK 존재
  - Django 기준: PK 자동 생성
  - Spring 기준: `@Id @GeneratedValue`
  - 기능상 영향: 랭킹 식별 안정성
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `trending_rankings.category_id FK -> trending_categories.id`
  - 현재 DB 상태: FK 존재, delete rule `CASCADE`
  - Django 기준: `on_delete=CASCADE`
  - Spring 기준: `@ManyToOne(optional=false)`
  - 기능상 영향: Django와 동등하게 카테고리 삭제 시 랭킹 연쇄 삭제
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `trending_rankings.related_model_id FK -> ai_models.id`
  - 현재 DB 상태: FK 존재, delete rule `SET NULL`, nullable `YES`
  - Django 기준: `on_delete=SET_NULL`, `null=True, blank=True`
  - Spring 기준: `@ManyToOne(optional=true)` (nullable)
  - 기능상 영향: Django와 동등하게 모델 삭제 시 랭킹 레코드 유지
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `trending_rankings.rank nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `PositiveIntegerField(validators=1..10)` (필수)
  - Spring 기준: `@Column(nullable=false)`
  - 기능상 영향: 순위 값 필수 보장
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `trending_rankings.name nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `CharField(max_length=100)` (필수)
  - Spring 기준: `@Column(nullable=false, length=100)`
  - 기능상 영향: 랭킹 표기명 누락 가능
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `trending_rankings.score nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `CharField(max_length=50)` (필수)
  - Spring 기준: `@Column(nullable=false, length=50)`
  - 기능상 영향: 점수 표기 누락 가능
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `trending_rankings.provider nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `CharField(max_length=50)` (필수)
  - Spring 기준: `@Column(nullable=false, length=50)`
  - 기능상 영향: 제공업체 표기 누락 가능
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `trending_rankings.use_exact_matching nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `BooleanField(default=False)` (필수)
  - Spring 기준: `@Column(nullable=false)`
  - 기능상 영향: 매칭 규칙 플래그 안정성
  - 판정: `A`
  - 조치 권고: 유지

- 검증 항목: `trending_rankings.model_detail_contains nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `CharField(blank=True)` (null=False 기본)
  - Spring 기준: `@Column(nullable=false, length=100)` default `""`
  - 기능상 영향: 필터 조건 공백 처리 불일치 가능
  - 판정: `A`
  - 조치 권고: 유지 (빈문자열 정책 고정)

- 검증 항목: `trending_rankings.model_etc_contains nullable`
  - 현재 DB 상태: `NO`
  - Django 기준: `CharField(blank=True)` (null=False 기본)
  - Spring 기준: `@Column(nullable=false, length=100)` default `""`
  - 기능상 영향: 필터 조건 공백 처리 불일치 가능
  - 판정: `A`
  - 조치 권고: 유지 (빈문자열 정책 고정)

- 검증 항목: `trending_rankings (category_id, rank) UNIQUE`
  - 현재 DB 상태: UNIQUE 존재 (`uk_trending_category_rank`)
  - Django 기준: `unique_together=['category','rank']`
  - Spring 기준: `@UniqueConstraint(category_id, rank)`
  - 기능상 영향: 카테고리 내 순위 중복 방지
  - 판정: `A`
  - 조치 권고: 유지

## 4) B/C 항목 우선순위

- 즉시(릴리즈 전): 없음 (이번 동기화에서 해소)
- 후속(개선 필요): 없음 (`B` 항목 없음)

## 5) 다음 섹션 재사용 템플릿

- 검증 항목 (`table.column` 또는 제약명)
- 현재 DB 상태
- Django 기준
- Spring 기준
- 기능상 영향
- 판정 등급 (`A/B/C`)
- 조치 권고 (`유지/개선/즉시수정`)
- 근거 경로 (Django + Spring + DB)
- 검증 항목: `trending_categories.name UNIQUE`
  - 현재 DB 상태: UNIQUE 존재 (`trending_categories_name_key`)
  - Django 기준: `unique=True`
  - Spring 기준: `@Column(unique=true)`
  - 기능상 영향: 중복 카테고리 생성 방지
  - 판정: `A`
  - 조치 권고: 유지
