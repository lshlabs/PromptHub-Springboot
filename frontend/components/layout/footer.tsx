import Link from 'next/link'
import { Star, Github, MessageCircle, Twitter, Youtube } from 'lucide-react'

// ============================================================================
// 타입 정의
// ============================================================================

/**
 * 소셜 링크 인터페이스
 */
interface SocialLink {
  /** 링크 URL */
  href?: string
  /** 아이콘 컴포넌트 */
  icon: React.ComponentType<{ className?: string }>
  /** 접근성을 위한 라벨 */
  label: string
  /** 활성 여부 */
  available?: boolean
  /** 상태 텍스트 */
  statusLabel?: string
}

/**
 * 푸터 메뉴 아이템 인터페이스
 */
interface FooterMenuItem {
  /** 링크 URL */
  href?: string
  /** 표시 텍스트 */
  label: string
  /** 뱃지 텍스트 (옵션) */
  badge?: string
  /** 뱃지 스타일 (옵션) */
  badgeStyle?: string
  available?: boolean
}

// 푸터 섹션 타입은 간소화에 따라 제거되었습니다.

// ============================================================================
// 상수 정의
// ============================================================================

/**
 * 소셜 미디어 링크 목록
 */
const SOCIAL_LINKS: SocialLink[] = [
  {
    href: 'https://github.com/lshlabs/PromptHub',
    icon: Github,
    label: 'GitHub 프로필',
  },
  {
    available: false,
    statusLabel: '준비 중',
    icon: MessageCircle,
    label: 'Discord 커뮤니티',
  },
  {
    available: false,
    statusLabel: '준비 중',
    icon: Twitter,
    label: 'Twitter 팔로우',
  },
  {
    available: false,
    statusLabel: '준비 중',
    icon: Youtube,
    label: 'YouTube 채널',
  },
] as const

// FOOTER_SECTIONS 데이터는 간소화에 따라 제거되었습니다.

/**
 * 하단 링크 목록
 */
const BOTTOM_LINKS: FooterMenuItem[] = [
  { href: '/privacy', label: '개인정보처리방침', available: true },
  { href: '/terms', label: '이용약관', available: true },
  { href: '/cookies', label: '쿠키 정책', available: true },
] as const

// ============================================================================
// 메인 컴포넌트
// ============================================================================

/**
 * 푸터 컴포넌트
 *
 * @description
 * - 사이트 하단에 위치하는 푸터 영역
 * - 브랜드 정보, 네비게이션 링크, 소셜 미디어 링크 제공
 * - Chrome 확장프로그램 홍보 섹션 제거됨
 * - 반응형 디자인으로 모든 화면 크기에 대응
 *
 * @features
 * - 브랜드 로고 및 소개
 * - 카테고리별 네비게이션 링크
 * - 소셜 미디어 링크
 * - Chrome 확장프로그램 CTA
 * - 저작권 및 법적 링크
 * - 키보드 접근성
 * - SEO 최적화
 */
export default function Footer(): JSX.Element {
  // ========================================================================
  // 유틸리티 함수들
  // ========================================================================

  /**
   * 현재 연도 (저작권 표시용)
   */
  const getCurrentYear = () => new Date().getFullYear()

  // ========================================================================
  // 렌더링 헬퍼 함수들
  // ========================================================================

  /**
   * 소셜 링크 렌더링
   *
   * @param link - 소셜 링크 객체
   * @returns 소셜 링크 JSX
   */
  const renderSocialLink = (link: SocialLink): JSX.Element => {
    const Icon = link.icon
    const isActive = !!link.href && link.available !== false

    if (!isActive) {
      return (
        <div
          key={link.label}
          className="group relative flex h-8 w-8 items-center justify-center rounded-lg bg-gray-800/60 text-gray-500"
          aria-label={`${link.label} (${link.statusLabel || '준비 중'})`}>
          <Icon className="h-4 w-4" aria-hidden="true" />
          <span className="pointer-events-none absolute -top-8 left-1/2 hidden -translate-x-1/2 whitespace-nowrap rounded bg-gray-700 px-2 py-1 text-[10px] text-gray-100 group-hover:block">
            {link.statusLabel || '준비 중'}
          </span>
        </div>
      )
    }

    const href = link.href as string

    return (
      <Link
        key={link.label}
        href={href}
        className="flex h-8 w-8 items-center justify-center rounded-lg bg-gray-800 transition-colors duration-200 hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 focus:ring-offset-gray-900"
        aria-label={link.label}
        target={href.startsWith('http') ? '_blank' : undefined}
        rel={href.startsWith('http') ? 'noopener noreferrer' : undefined}>
        <Icon className="h-4 w-4" aria-hidden="true" />
      </Link>
    )
  }

  // 메뉴 섹션 관련 렌더링 로직은 간소화에 따라 제거되었습니다.

  // ========================================================================
  // 메인 렌더링
  // ========================================================================

  return (
    <footer className="relative bg-gradient-to-br from-gray-900 via-gray-900 to-gray-800 py-16 text-white">
      {/* 배경 그라데이션 효과 */}
      <div
        className="absolute inset-0 bg-gradient-to-r from-blue-600/5 to-purple-600/5"
        aria-hidden="true"></div>

      <div className="container relative mx-auto px-4">
        <div className="grid grid-cols-1 gap-8">
          {/* ================================================================
              브랜드 섹션
              ================================================================ */}
          <div className="lg:col-span-1">
            <div className="relative overflow-hidden rounded-2xl border border-white/10 bg-gradient-to-br from-gray-900/60 via-gray-900/40 to-gray-800/60 p-6 shadow-[0_0_0_1px_rgba(255,255,255,0.02)]">
              {/* 장식용 글로우 */}
              <div className="pointer-events-none absolute -left-10 -top-10 h-40 w-40 rounded-full bg-blue-600/20 blur-3xl"></div>
              <div className="pointer-events-none absolute -bottom-10 -right-10 h-40 w-40 rounded-full bg-purple-600/20 blur-3xl"></div>

              <div className="flex flex-col gap-6 sm:grid sm:grid-cols-[auto,1fr,auto] sm:items-center">
                {/* 로고 */}
                <div className="flex items-center gap-3">
                  <div className="relative">
                    <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-r from-blue-600 to-purple-600 shadow-lg shadow-blue-500/20 ring-1 ring-white/20">
                      <Star className="h-7 w-7 fill-white text-white" aria-hidden="true" />
                    </div>
                    <div className="absolute -right-1 -top-1 h-2.5 w-2.5 animate-pulse rounded-full bg-yellow-400 sm:h-3 sm:w-3"></div>
                  </div>
                  <div>
                    <h3 className="bg-gradient-to-r from-blue-400 to-purple-400 bg-clip-text text-2xl font-extrabold tracking-tight text-transparent">
                      PromptHub
                    </h3>
                    <p className="text-xs text-gray-400">AI 프롬프트 리뷰 플랫폼</p>
                  </div>
                </div>

                {/* 소개 및 하이라이트 */}
                <div>
                  <p className="text-sm leading-relaxed text-gray-300">
                    더 나은 프롬프트로 AI와 소통하세요. 커뮤니티와 함께 성장하는 플랫폼.
                  </p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    <span className="rounded-full border border-white/10 bg-white/5 px-2.5 py-1 text-xs text-gray-200">
                      리뷰
                    </span>
                    <span className="rounded-full border border-white/10 bg-white/5 px-2.5 py-1 text-xs text-gray-200">
                      커뮤니티
                    </span>
                    <span className="rounded-full border border-white/10 bg-white/5 px-2.5 py-1 text-xs text-gray-200">
                      트렌드
                    </span>
                  </div>
                </div>

                {/* 소셜 링크 */}
                <div
                  className="flex justify-start gap-3 sm:justify-end"
                  role="list"
                  aria-label="소셜 미디어 링크">
                  {SOCIAL_LINKS.map(renderSocialLink)}
                </div>
              </div>
            </div>
          </div>

          {/* 간소화: 푸터 메뉴 섹션 제거 */}
        </div>

        {/* 간소화: Chrome 확장프로그램 하이라이트 제거 */}

        {/* ================================================================
            하단 저작권 및 법적 링크
            ================================================================ */}
        <div className="mt-12 border-t border-gray-800 pt-8">
          <div className="flex flex-col items-center justify-between gap-4 sm:flex-row">
            <p className="text-sm text-gray-400">
              Made by @hu2chaso | PromptHub - Private Project {getCurrentYear()}
            </p>
            <nav className="flex gap-6 text-sm text-gray-400" aria-label="법적 링크">
              {BOTTOM_LINKS.map(link => (
                link.href && link.available !== false ? (
                  <Link
                    key={link.label}
                    href={link.href}
                    className="transition-colors duration-200 hover:text-white focus:text-white focus:outline-none">
                    {link.label}
                  </Link>
                ) : (
                  <span key={link.label} className="cursor-not-allowed text-gray-500">
                    {link.label}
                  </span>
                )
              ))}
            </nav>
          </div>
        </div>
      </div>
    </footer>
  )
}
