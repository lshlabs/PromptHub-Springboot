/**
 * CommunityHeader 컴포넌트
 *
 * 커뮤니티 페이지의 헤더 섹션을 표시합니다.
 * 통계 카드들과 커뮤니티 소개 정보를 포함합니다.
 */

'use client'

import { Users, FileText, Star, MessageSquare, Clock } from 'lucide-react'

interface CommunityStats {
  activeUsers: number
  sharedPrompts: number
  averageSatisfaction: number
  weeklyAdded: number
}

interface CommunityHeaderProps {
  stats: CommunityStats
  loading?: boolean
}

export function CommunityHeader({ stats, loading = false }: CommunityHeaderProps) {
  const statCards = [
    {
      key: 'activeUsers',
      value: stats.activeUsers,
      label: '이용자 수',
      icon: Users,
      cardClass: 'border-blue-100 bg-blue-50',
      iconClass: 'text-blue-600',
      valueClass: 'text-blue-700',
      labelClass: 'text-blue-600',
    },
    {
      key: 'sharedPrompts',
      value: stats.sharedPrompts,
      label: '공유된 프롬프트',
      icon: FileText,
      cardClass: 'border-green-100 bg-green-50',
      iconClass: 'text-green-600',
      valueClass: 'text-green-700',
      labelClass: 'text-green-600',
    },
    {
      key: 'averageSatisfaction',
      value: stats.averageSatisfaction,
      label: '평균 만족도',
      icon: Star,
      cardClass: 'border-yellow-100 bg-yellow-50',
      iconClass: 'text-yellow-600',
      valueClass: 'text-yellow-700',
      labelClass: 'text-yellow-600',
    },
    {
      key: 'weeklyAdded',
      value: stats.weeklyAdded,
      label: '이번 주 추가',
      icon: Clock,
      cardClass: 'border-purple-100 bg-purple-50',
      iconClass: 'text-purple-600',
      valueClass: 'text-purple-700',
      labelClass: 'text-purple-600',
    },
  ] as const

  return (
    <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
      <div className="h-1 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500"></div>
      <div className="p-6 text-center">
        <div className="mb-3 flex items-center justify-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-blue-500 to-purple-600 sm:h-10 sm:w-10">
            <MessageSquare className="h-4 w-4 text-white sm:h-5 sm:w-5" />
          </div>
          <h1 className="text-gray-900">프롬프트 커뮤니티</h1>
        </div>
        <p className="mb-6 text-gray-600">
          전문가들의 검증된 프롬프트를 발견하고, <br className="block sm:hidden" />
          당신의 경험을 공유하세요
        </p>

        {/* 통계 카드들 */}
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
          {statCards.map(card => {
            const Icon = card.icon
            return (
              <div key={card.key} className={`rounded-xl border p-4 ${card.cardClass}`}>
                {loading ? (
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
                      <h1 className={card.valueClass}>{card.value}</h1>
                    </div>
                    <p className={`text-xs sm:text-sm ${card.labelClass}`}>{card.label}</p>
                  </>
                )}
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}
