File Path: /portfolio/section7_release_gate_troubleshooting.md

# Section 7 Troubleshooting: AI Baseline 검토 과정에서 발견한 완료 기준/품질 게이트 경계 위험

## 문제 정의

섹션7에서 AI baseline은 호환 매트릭스와 백로그 문서를 빠르게 작성해 “마무리 단계”를 가시화했다.  
하지만 Human Architect 리뷰에서 **완료 선언 기준(Release Gate)과 리팩터링 백로그 경계가 느슨해, 기능이 동작해도 종료 판단이 모호해지는 구조적 위험**이 확인됐다.

즉, 이 문제는 구현 중 우연히 발생한 실수가 아니라,  
**AI가 만든 baseline을 사람이 운영 관점으로 리뷰하면서 선제 식별한 구조 설계 이슈**다.

## Before

AI baseline이 빠르게 해결한 것:

- `BACKEND_API_COMPAT_MATRIX.md` 작성으로 엔드포인트 구현 현황 정리
- `BACKEND_SECTION7_BACKLOG.md` 작성으로 후속 과제 목록화
- `./gradlew test` 통과 상태 기반의 1차 안정성 확인

baseline의 목적은 “완전한 종료판정”이 아니라 **가시적인 정리와 인수인계 속도 확보**였다.

## Problem

Human Architect 리뷰에서 드러난 핵심 구조적 결함:

- “구현됨” 표기와 “운영 준비 완료”의 기준이 분리되지 않아 완료 판정이 과대평가될 수 있음
- 핵심 사용자 플로우가 단위/서비스 테스트 중심으로만 검증되고, 실제 호출 흐름 스모크 게이트가 약함
- 표준화 후보와 호환 유지 대상의 우선순위가 문서상 분리돼도 검증 근거와 직접 연결되지 않음

즉 baseline은 문서는 정리됐지만 **출시 가능성 판단 기준이 불충분한 상태**였다.

## 왜 이 문제가 중요한가

섹션7은 “문서 작성”이 아니라 **최종 종료 기준을 잠그는 단계**다.  
여기서 기준이 느슨하면:

- 미완료 리스크가 완료로 오인되고
- 이후 장애/회귀가 발생했을 때 책임 경계가 흐려지며
- 포트폴리오/면접에서도 “무엇이 검증됐는지”를 설명하기 어려워진다

따라서 이 문제는 단순 문서 품질이 아니라 **릴리즈 거버넌스의 정확성 문제**다.

## After

Human Architect는 baseline을 다음과 같이 재정렬했다.

- 핵심 플로우를 `MockMvc` 스모크 테스트로 명시적으로 고정
  - register/login/refresh
  - posts list/detail/create
  - like/bookmark + liked/bookmarked
  - stats dashboard 보호 경로
- 호환 매트릭스와 백로그를 테스트 근거와 연결해 “구현/검증/잔여”를 분리
- “지금 완료된 것”과 “2차 표준화/고도화 잔여”를 명확히 구분

## 테스트로 어떻게 증명할지

- `Section7ApiSmokeTest.criticalFrontendFlow_login_list_detail_like_bookmark_smoke`
- `Section7ApiSmokeTest.refreshTokenFlow_smoke`
- 전체 회귀: `./gradlew test` 통과
- 문서 검증: 호환 매트릭스/백로그가 테스트 결과와 모순 없이 정렬되는지 확인

## 이번 사례에서 강조할 점

핵심은 “기능이 된다”가 아니라 “완료 기준이 검증된다”로 전환한 것이다.

- AI는 빠르게 정리 문서를 만들었다
- Human은 종료 판정 기준을 테스트와 연결해 잠갔다
- 결과적으로 완료/부분완료/미완료 경계가 명확해졌다

즉, **AI는 정리 속도, Human Architect는 종료 기준의 신뢰성**을 책임진 사례다.

## 면접에서 어떻게 설명할지

> 섹션7에서는 AI가 만든 정리 문서를 그대로 완료로 보지 않고, Human Architect 관점에서 완료 기준을 테스트 근거와 연결했습니다. MockMvc 스모크 테스트로 핵심 플로우를 고정하고, 호환 유지 항목과 표준화 백로그를 분리해 실제로 무엇이 검증됐는지 설명 가능한 상태로 마감했습니다.

