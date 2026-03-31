# Spring 아키텍처 마이그레이션에 따른 동작 확장 명세서

## 1) 목적

이 문서는 Django 원본과의 1:1 동일성보다 **정확도/안정성/동시성 안전성**을 우선하는
Spring Boot 환경의 **의도적 동작 확장**을 공식 스펙으로 확정한다.

## 2) 확장 항목

### 2.1 사용자 통계 집계: total_likes / total_bookmarks

- **대상 API**: `/api/stats/user`
- **Django 기준**: `Post.like_count`, `Post.bookmark_count` 합계 (denormalized counter)
- **Spring 기준**: `PostInteraction` 기반 실데이터 집계
- **변경 명분**:
  - Denormalized counter는 동시성/트랜잭션 경합 시 데이터 드리프트 발생 가능
  - Interaction 기반 집계는 실제 사용 행위를 정확히 반영
- **운영 리스크**:
  - 대량 데이터 환경에서 집계 쿼리 비용 증가 가능
  - 다만 대시보드 캐시(60초) 적용으로 완화됨
- **결정**: **Spring 방식 채택 (정확도 우선)**
- **검증 포인트**:
  - interaction 기준 집계 결과가 실제 UI 반영과 일치하는지 확인
  - 캐시 히트 비율 및 집계 응답 시간 모니터링

## 3) 유지/확장 판단 기준

- 기능 정확도와 데이터 정합성 개선이 명확할 경우 Spring 개선안을 채택한다.
- 개선안 채택 시 Django 동일성 체크리스트에서는 `B`로 분류하되,
  본 명세서에 근거와 운영 리스크를 기록해 **합의된 의도적 차이**로 고정한다.
