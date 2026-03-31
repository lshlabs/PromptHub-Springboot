File Path: /portfolio/section6_cache_query_troubleshooting.md

# Section 6 Troubleshooting: AI Baseline 검토 과정에서 발견한 조회/캐시 경계 위험

## 문제 정의

이번 Spring Boot 마이그레이션에서 AI baseline은 `core/stats` 엔드포인트를 빠르게 연결해 조회 기능을 동작시켰다.  
하지만 Human Architect 리뷰에서 **검색/집계 쿼리 책임과 캐시 책임이 컨트롤러 중심으로 섞여 있고, placeholder 응답이 실제 계약처럼 고정될 수 있는 구조적 위험**이 확인됐다.

즉, 이 이슈는 구현 중 우연히 생긴 버그가 아니라,  
**AI가 빠르게 만든 baseline을 리뷰하는 과정에서 선제적으로 식별한 아키텍처 결함**이다.

## Before

AI baseline이 빠르게 해결한 것:

- `/api/core/search/`, `/sort-options/`, `/filter-options/`, `/trending/*` 경로 연결
- `/api/stats/dashboard/`, `/api/stats/user/` API 골격 및 캐시 어노테이션 부착
- 프론트 연동을 위한 기본 응답 형태 유지

baseline의 목적은 완성형 성능 설계가 아니라 **연동 가능한 조회 기능의 신속한 가시화**였다.

## Problem

Human Architect 리뷰에서 확인한 숨겨진 구조적 결함:

- 캐시/조회 로직이 컨트롤러에 집중되어 책임 경계가 불명확함
- 통계/트렌딩 핵심 값이 placeholder에 머물러 계약 신뢰도를 떨어뜨림
- 검색 필터 조합(플랫폼/카테고리/만족도) 확장 시 쿼리 기준점이 부족함
- 트렌딩 캐시는 붙어 있으나 key/동시 miss 제어 기준이 명시되지 않아 stampede 여지가 있음

즉 baseline은 기능은 동작해도 **성능/정합성 기준이 약한 상태**였다.

## 왜 이 문제가 중요한가

섹션6의 핵심은 단순 조회 API 추가가 아니라,  
**조회 비용과 캐시 효율을 통제 가능한 구조로 고정하는 것**이다.

이 경계를 정리하지 않으면:

- 조회 트래픽 증가 시 응답 지연과 DB 부하가 누적되고
- placeholder 기반 계약이 프론트/백엔드 양쪽에서 기술부채가 되며
- 후속 최적화(QueryDSL, Redis 고도화) 시 리팩터링 범위가 과도하게 커진다

따라서 이는 기능 이슈가 아니라 **확장성 통제 이슈**다.

## After

Human Architect는 baseline을 그대로 확정하지 않고 다음 방향으로 재설계했다.

- 컨트롤러에서 로직을 분리해 `CoreService`, `StatsService` 중심으로 조회/캐시 책임을 이동
- 검색 API에 플랫폼/카테고리/만족도 범위 필터를 반영 가능한 쿼리 경로 구축
- 트렌딩/대시보드 통계를 placeholder에서 실제 집계 기반으로 전환
- 캐시는 `@Cacheable(sync = true)`를 적용해 동시 miss 구간의 stampede 리스크를 1차 완화
- 리포지토리에 집계 쿼리(합계/평균/분포/최근활동)와 모델 키워드 조회 경로를 명시

## 테스트로 어떻게 증명할지

- `CoreServiceSection6Test.search_supportsFiltersAndSatisfactionRange`
  - 검색 필터 조합이 실제 결과를 제한하는지 검증
- `CoreServiceSection6Test.categoryRankings_populatesTrendingCache`
  - 트렌딩 캐시 생성(hit 준비) 검증
- `StatsServiceSection6Test.dashboard_returnsAggregatedValuesAndCachesResult`
  - 대시보드 집계값 및 stats 캐시 생성 검증
- `StatsServiceSection6Test.userStats_returnsAuthorBasedAggregates`
  - 사용자 통계 집계/최근활동 필드 검증
- 최종적으로 `./gradlew test` 통과 확인

## 이번 사례에서 강조할 점

핵심은 “조회 API가 돌아간다”에서 멈추지 않았다는 점이다.

- AI는 빠르게 기능 baseline을 만들었다
- Human은 책임 경계(컨트롤러/서비스/리포지토리)와 캐시 전략을 재정의했다
- placeholder 계약을 실제 집계로 전환하고 테스트로 회귀 방지 기준을 세웠다

즉, **속도는 AI가 만들고, 확장성 기준은 Human Architect가 고정**한 사례다.

## 면접에서 어떻게 설명할지

> 섹션6에서는 AI가 만든 조회 API baseline을 그대로 확정하지 않고, 컨트롤러에 섞여 있던 캐시/집계 로직을 서비스 계층으로 분리했습니다. 이후 검색 필터 쿼리와 통계 집계를 실제 데이터 기반으로 전환하고, 캐시 동작을 테스트로 검증해 성능·정합성 기준을 명시적으로 확보했습니다.

