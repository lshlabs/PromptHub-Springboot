import * as React from 'react'

/**
 * 반응형 디자인을 위한 최소한의 커스텀 훅
 *
 * 실제 사용되는 기능만 구현하여 복잡도를 줄이고 유지보수를 쉽게 합니다.
 * CSS 미디어 쿼리로 해결 가능한 부분은 CSS를 우선 사용하세요.
 */

// 브레이크포인트 정의 (실제 사용되는 것만)
const MOBILE_BREAKPOINT = 768 // 태블릿 시작점

/**
 * 모바일 기기 여부를 확인하는 훅
 * 주로 모바일에서 완전히 다른 동작이 필요한 경우에만 사용합니다.
 *
 * @example
 * const isMobile = useIsMobile()
 * if (isMobile) {
 *   // 모바일 전용 로직
 * }
 */
export function useIsMobile() {
  const [isMobile, setIsMobile] = React.useState<boolean>(false)

  React.useEffect(() => {
    // 초기값 설정
    const checkMobile = () => {
      setIsMobile(window.innerWidth <= MOBILE_BREAKPOINT)
    }

    // 마운트 시 즉시 체크
    checkMobile()

    // 리사이즈 이벤트 처리
    window.addEventListener('resize', checkMobile)

    return () => {
      window.removeEventListener('resize', checkMobile)
    }
  }, [])

  return isMobile
}

/**
 * 화면 크기를 구분하는 훅
 * CustomButton, CustomBadge 등에서 responsive 사이즈를 구현할 때 사용합니다.
 *
 * @returns 'mobile' | 'tablet' | 'desktop'
 */
export function useScreenSize() {
  const [screenSize, setScreenSize] = React.useState<'mobile' | 'tablet' | 'desktop'>('desktop')

  React.useEffect(() => {
    const getScreenSize = () => {
      const width = window.innerWidth

      if (width <= 480) return 'mobile'
      if (width <= 768) return 'tablet'
      return 'desktop'
    }

    const updateScreenSize = () => {
      setScreenSize(getScreenSize())
    }

    // 초기값 설정
    updateScreenSize()

    // 리사이즈 이벤트 처리
    window.addEventListener('resize', updateScreenSize)

    return () => {
      window.removeEventListener('resize', updateScreenSize)
    }
  }, [])

  return screenSize
}

/**
 * 사용 가이드:
 *
 * 1. 단순한 스타일 변경은 CSS 미디어 쿼리를 사용하세요
 *    @media (max-width: 768px) { ... }
 *
 * 2. Tailwind CSS 사용 시 반응형 클래스를 활용하세요
 *    className="text-sm md:text-base lg:text-lg"
 *
 * 3. JavaScript 로직이 필요한 경우에만 이 훅들을 사용하세요
 *    - 모바일에서 다른 컴포넌트를 렌더링
 *    - 터치/마우스 이벤트 분기
 *    - 모바일 전용 기능 활성화
 */
