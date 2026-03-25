/**
 * ProfileStatsSection 컴포넌트
 *
 * 프로필 페이지의 통계 섹션을 표시합니다.
 * 리뷰수, 좋아요, 북마크, 조회수를 카드 형태로 보여줍니다.
 */

'use client'

import { FileText, Heart, Bookmark, Eye } from 'lucide-react'

interface ProfileStats {
  postCount: number
  likeCount: number
  bookmarkCount: number
  viewCount: number
}

interface ProfileStatsSectionProps {
  stats: ProfileStats
  isLoading?: boolean
  title?: string
  contained?: boolean
}

export function ProfileStatsSection({
  stats,
  isLoading = false,
  title = '통계',
  contained = false,
}: ProfileStatsSectionProps) {
  const formatNumber = (num: number): string => {
    return num.toLocaleString('ko-KR')
  }

  const statCards = [
    {
      key: 'postCount',
      value: stats.postCount,
      label: '작성 리뷰수',
      icon: FileText,
      cardClass: 'border-blue-100 bg-blue-50',
      iconClass: 'text-blue-600',
      valueClass: 'text-blue-700',
      labelClass: 'text-blue-600',
    },
    {
      key: 'likeCount',
      value: stats.likeCount,
      label: '좋아요 수',
      icon: Heart,
      cardClass: 'border-red-100 bg-red-50',
      iconClass: 'text-red-600',
      valueClass: 'text-red-700',
      labelClass: 'text-red-600',
    },
    {
      key: 'bookmarkCount',
      value: stats.bookmarkCount,
      label: '북마크 수',
      icon: Bookmark,
      cardClass: 'border-green-100 bg-green-50',
      iconClass: 'text-green-600',
      valueClass: 'text-green-700',
      labelClass: 'text-green-600',
    },
    {
      key: 'viewCount',
      value: stats.viewCount,
      label: '조회수',
      icon: Eye,
      cardClass: 'border-purple-100 bg-purple-50',
      iconClass: 'text-purple-600',
      valueClass: 'text-purple-700',
      labelClass: 'text-purple-600',
    },
  ] as const

  const Wrapper: React.FC<{ children: React.ReactNode }> = ({ children }) =>
    contained ? (
      <>{children}</>
    ) : (
      <div className="rounded-lg border border-gray-100 bg-white p-6 shadow-sm">{children}</div>
    )

  return (
    <Wrapper>
      <div className="grid grid-cols-2 gap-4 text-center md:grid-cols-4">
        {statCards.map(card => {
          const Icon = card.icon
          return (
            <div key={card.key} className={`rounded-xl border p-4 ${card.cardClass}`}>
              {isLoading ? (
                <div className="space-y-2" aria-hidden="true">
                  <div className="mx-auto flex items-center justify-center gap-2">
                    <div className="h-4 w-4 animate-pulse rounded bg-white/70" />
                    <div className="h-6 w-12 animate-pulse rounded bg-white/80" />
                  </div>
                  <div className="mx-auto h-4 w-20 animate-pulse rounded bg-white/70" />
                </div>
              ) : (
                <>
                  <div className="mb-2 flex items-center justify-center gap-2">
                    <Icon className={`h-4 w-4 ${card.iconClass}`} />
                    <h1 className={card.valueClass}>{formatNumber(card.value)}</h1>
                  </div>
                  <p className={`text-xs sm:text-sm ${card.labelClass}`}>{card.label}</p>
                </>
              )}
            </div>
          )
        })}
      </div>
    </Wrapper>
  )
}
