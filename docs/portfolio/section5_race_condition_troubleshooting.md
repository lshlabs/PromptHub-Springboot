File Path: /portfolio/section5_race_condition_troubleshooting.md

# Section 5 Troubleshooting: AI Baseline 검토 과정에서 발견한 상호작용/조회수 정합성 위험

## 문제 정의

이번 Spring Boot 마이그레이션에서는 기능 구현 속도를 높이기 위해 AI를 활용해 상호작용 API의 baseline 코드를 먼저 작성했다.  
이 baseline은 좋아요/북마크 토글, 내 활동 조회, 조회수 증가 기능을 빠르게 연결하는 데는 유리했지만, **아키텍처 관점에서 동시성 하의 정합성 위험을 내포한 구조**였다.

즉, 이번 문제는 운영 중 실제 장애가 먼저 발생해서 발견한 것이 아니라,  
**AI가 빠르게 작성한 baseline을 검토하는 과정에서 사람이 구조적 결함을 선제적으로 식별한 사례**다.

---

## Before: AI가 빠르게 작성한 초기 Baseline

AI는 프론트 연동과 기능 동작을 우선하기 위해 다음과 같은 구조의 baseline을 제안했다.

- `PostService.toggleLike`, `PostService.toggleBookmark`에서 interaction 상태를 변경
- `Post.likeCount`, `Post.bookmarkCount`를 서비스 계층에서 직접 증감
- 조회수는 `PostRepository.incrementViewCount` 원자 쿼리로 별도 증가
- `liked-posts`, `bookmarked-posts`, `my-posts` API를 빠르게 연결

이 구조는 **단일 요청 기준으로는 충분히 동작**했고, 프론트 호환 API를 빠르게 맞추는 데도 도움이 되었다.  
즉 baseline의 목적은 “완성형 설계”가 아니라 **빠른 기능 가시화와 이관 속도 확보**에 있었다.

---

## Problem: Architect 관점에서 확인한 숨겨진 구조적 결함

baseline을 리뷰하면서 가장 먼저 확인한 문제는 **상태 변경과 집계 변경이 분리되어 있다는 점**이었다.

### 핵심 구조적 위험
- interaction row의 실제 상태와 post의 카운트 필드가 별도로 관리됨
- 두 요청이 거의 동시에 들어오면 같은 기존 상태를 기준으로 서로 다른 증감 계산을 수행할 수 있음
- 조회수는 원자 증가 쿼리로 분리했지만, 좋아요/북마크 카운트는 읽기-수정-쓰기 흐름에 가까워 drift 가능성이 남음
- 트랜잭션은 있어도 충돌 상황을 명시적으로 제어하는 전략(락, 버전, 보정 정책)이 없음

즉, baseline은 **“기능은 돌아가지만 장기적으로 정합성 drift가 생길 수 있는 구조”**였다.

이 문제는 단순 버그라기보다,  
**AI가 빠르게 만든 기능 중심 baseline이 동시성·정합성 관점까지는 충분히 반영하지 못한 전형적인 구조적 한계**라고 판단했다.

---

## 왜 이 문제가 중요한가

이번 프로젝트의 목적은 단순히 Django 기능을 Java 문법으로 옮기는 것이 아니라,  
**Spring 기반 백엔드 구조에서 정합성과 확장성을 고려한 재설계 경험을 쌓는 것**이다.

따라서 단일 요청 기준으로 동작한다는 이유만으로 baseline을 그대로 확정하면,
- 데이터 정합성에 대한 책임이 불분명해지고
- 상호작용 상태와 집계 필드 간 불일치가 누적될 수 있으며
- 이후 Redis 캐시, 트렌딩 집계, 통계 API와 연결될 때 더 큰 구조적 문제로 확장될 수 있다

즉, 이 문제를 초기에 구조적으로 정리하는 것은 **단순 버그 수정이 아니라 아키텍처 통제의 문제**였다.

---

## After: Human Architect가 재설계한 방향

AI baseline을 그대로 확정하지 않고, 아래 기준으로 리팩터링 방향을 다시 잡았다.

### 1) 상태 변경과 집계 변경의 책임 분리 재검토
- interaction 테이블의 상태를 source of truth로 볼지
- post 카운트 필드를 최종 집계값으로 유지할지
- 두 구조를 함께 가져갈 경우 어떤 보정 정책이 필요한지 먼저 정의

### 2) 충돌 제어 전략 명시
- `PostRepository.findByIdForUpdate`(PESSIMISTIC_WRITE)로 토글 구간 직렬화
- 토글 처리 시 `PostInteraction` 상태 변경과 `Post` 집계 변경을 같은 트랜잭션 경계에서 수행
- 조회수는 `incrementViewCount` 원자 쿼리 경로를 유지해 Lost Update를 방지

### 3) 조회수 증가와 상호작용 카운트 전략 분리
- 조회수는 DB 원자적 update 유지
- 좋아요/북마크는 단순 증감이 아니라 정합성 기준을 먼저 설계
- 정합성 검증용으로 interaction 기준 count 쿼리를 도입해 카운트 드리프트를 검출 가능하게 구성

### 4) 테스트를 구조 검증 수단으로 사용
- 기능이 동작하는지만 보는 것이 아니라
- 동시성 상황에서 실제 interaction 상태와 카운트가 항상 일치하는지를 검증하는 방향으로 테스트 설계

즉, 사람의 역할은 AI가 만든 코드를 미세 수정하는 것이 아니라,  
**정합성의 기준과 충돌 제어 전략을 먼저 정의하고 그 기준에 맞게 baseline을 재구성하는 것**이었다.

---

## 테스트로 어떻게 증명할지

### 1. 동시성 정합성 테스트
- `PostInteractionConcurrencyTest.concurrentToggle_keepsLikeAndBookmarkCountsConsistent`
- 병렬 토글 후 `interaction 실제 count`와 `post like/bookmark count`가 항상 일치하는지 검증

### 2. 조회수 Lost Update 방지 테스트
- `PostInteractionConcurrencyTest.concurrentDetail_incrementsViewCountWithoutLoss`
- 병렬 상세 조회 후 `view_count`가 호출 수와 동일한지 검증

### 3. 회귀 테스트
- `PostInteractionConcurrencyTest.likedAndBookmarkedEndpoints_matchInteractionState`
- `liked/bookmarked` API 결과가 실제 interaction 상태와 일치하는지 검증

---

## 이번 사례에서 강조할 점

이 사례의 핵심은 **“구현하다가 문제가 터져서 고쳤다”**가 아니다.

핵심은 다음과 같다.

- AI는 빠르게 기능 중심 baseline을 만들었다
- 사람은 그 baseline을 그대로 신뢰하지 않고 구조를 검토했다
- 그 과정에서 동시성 하의 정합성 위험을 선제적으로 발견했다
- 이후 정합성 기준, 충돌 제어 전략, 테스트 기준을 직접 설계했다

즉, 이 사례는  
**AI를 활용해 개발 속도를 높이되, 최종 구조와 데이터 무결성의 책임은 사람이 진다**는 이번 프로젝트의 핵심 서사를 가장 잘 보여주는 예시다.

---

## 면접에서 어떻게 설명할지

> 이번 마이그레이션에서는 AI를 활용해 상호작용 API의 baseline을 빠르게 만들었습니다. 다만 기능이 동작한다고 바로 확정하지 않고, 동시성 하에서 interaction 상태와 집계 카운트가 분리된 구조가 정합성 위험을 만든다고 판단했습니다. 그래서 저는 AI가 만든 초안을 그대로 사용하지 않고, 원자적 업데이트, 충돌 제어 전략, 정합성 검증 테스트 기준을 먼저 정의한 뒤 리팩터링 방향을 다시 설계했습니다.

### 답변 포인트
- 문제를 “기능 버그”보다 **구조적 결함**으로 정의
- AI의 역할은 빠른 초안 작성
- 인간의 역할은 아키텍처 판단과 데이터 무결성 책임
- 리팩터링 후에는 반드시 테스트로 증명
