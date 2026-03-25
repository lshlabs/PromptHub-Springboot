import { Card, CardContent, CardHeader } from '@/components/ui/card'
import * as React from 'react'
import { useState, useEffect } from 'react'
import {
  Trophy,
  Medal,
  Award,
  Code,
  ImageIcon,
  Brain,
  Clock,
  DollarSign,
  Zap,
  Calculator,
  Wrench,
  Lightbulb,
} from 'lucide-react'
import { trendingApi } from '@/lib/api'
import { getDomainErrorMessage } from '@/lib/utils'
import type { CategoryRankings as CategoryRankingsData, TrendingRanking } from '@/types/api'

let categoryRankingsCache: CategoryRankingsData | null = null

interface CategoryRankingsProps {
  selectedModel: string | null
  setSelectedModel: (model: string | null) => void
  onSelectModel?: () => void
  onLoadingChange?: (loading: boolean) => void
}

// Lucide 아이콘 매핑
const iconMap = {
  Code,
  ImageIcon,
  Brain,
  Clock,
  DollarSign,
  Zap,
  Calculator,
  Wrench,
  Lightbulb,
  Trophy,
}

// API 아이콘 이름을 실제 아이콘 컴포넌트로 매핑
const getIconComponent = (iconName: string): React.ComponentType<{ className?: string }> => {
  return iconMap[iconName as keyof typeof iconMap] || Code
}

const getRankIcon = (rank: number) => {
  switch (rank) {
    case 1:
      return <Trophy className="h-4 w-4 text-white" />
    case 2:
      return <Medal className="h-4 w-4 text-white" />
    case 3:
      return <Award className="h-4 w-4 text-white" />
    default:
      return null
  }
}

const getRankStyle = (rank: number) => {
  switch (rank) {
    case 1:
      return {
        container:
          'bg-gradient-to-r from-yellow-50 to-yellow-100 border-2 border-yellow-200 shadow-lg',
        badge: 'bg-gradient-to-r from-yellow-500 to-yellow-600 text-white shadow-md',
        score: 'text-yellow-700 text-lg font-bold',
      }
    case 2:
      return {
        container: 'bg-white border border-gray-200 shadow-sm',
        badge: 'bg-gradient-to-r from-gray-400 to-gray-500 text-white shadow-sm',
        score: 'text-gray-700 text-base font-medium',
      }
    case 3:
      return {
        container: 'bg-white border border-gray-200 shadow-sm',
        badge: 'bg-gradient-to-r from-amber-600 to-amber-700 text-white',
        score: 'text-amber-700 text-base font-medium',
      }
    default:
      return {
        container: 'bg-white border border-gray-200 shadow-sm',
        badge: 'bg-gradient-to-r from-gray-400 to-gray-500 text-white',
        score: 'text-orange-600 text-base font-semibold',
      }
  }
}

export default function CategoryRankings({
  selectedModel,
  setSelectedModel,
  onSelectModel,
  onLoadingChange,
}: CategoryRankingsProps) {
  const [categoryRankings, setCategoryRankings] = useState<CategoryRankingsData>(
    categoryRankingsCache ?? {},
  )
  const [loading, setLoading] = useState(!categoryRankingsCache)
  const [error, setError] = useState<string | null>(null)
  const [dataLoaded, setDataLoaded] = useState(!!categoryRankingsCache) // 중복 API 호출 방지
  useEffect(() => {
    onLoadingChange?.(loading)
  }, [loading, onLoadingChange])

  useEffect(() => {
    // 이미 데이터가 로드된 경우 스킵
    if (dataLoaded) return

    const fetchCategoryRankings = async () => {
      try {
        setLoading(true)
        setError(null)
        const response = await trendingApi.getCategoryRankings()
        setCategoryRankings(response.data)
        categoryRankingsCache = response.data
        setDataLoaded(true) // 데이터 로드 완료 표시
      } catch (err) {
        setError(
          getDomainErrorMessage(
            err,
            '트렌딩 카테고리 데이터를 불러오지 못했습니다. 새로고침 후 다시 확인해주세요.',
            {
              serverError: '트렌딩 집계 서버가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.',
            },
          ),
        )
        setDataLoaded(true) // 에러 발생해도 로드 완료로 표시
      } finally {
        setLoading(false)
      }
    }

    fetchCategoryRankings()
  }, [dataLoaded])

  if (loading) {
    return (
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3" aria-hidden="true">
        {Array.from({ length: 6 }).map((_, idx) => (
          <div key={idx} className="rounded-xl border border-gray-100 bg-white/70 p-6">
            <div className="mb-4 flex items-start gap-3">
              <div className="h-10 w-10 animate-pulse rounded-lg bg-gray-200" />
              <div className="flex-1 space-y-2">
                <div className="h-5 w-2/3 animate-pulse rounded bg-gray-200" />
                <div className="h-4 w-5/6 animate-pulse rounded bg-gray-100" />
              </div>
            </div>
            <div className="space-y-3">
              <div className="h-16 animate-pulse rounded-lg bg-gray-100" />
              <div className="h-16 animate-pulse rounded-lg bg-gray-100" />
              <div className="h-16 animate-pulse rounded-lg bg-gray-100" />
            </div>
          </div>
        ))}
      </div>
    )
  }

  if (error) {
    return (
      <div className="rounded-lg bg-red-50 p-6 text-center">
        <p className="text-red-700">{error}</p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
      {Object.entries(categoryRankings).map(([key, category]) => {
        const IconComponent = getIconComponent(category.icon)
        return (
          <Card
            key={key}
            className="border-0 bg-white/70 backdrop-blur-sm transition-shadow hover:shadow-lg">
            <CardHeader className="pb-3">
              <div className="mb-2 flex items-start space-x-3">
                <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-gradient-to-r from-orange-500 to-pink-500">
                  <IconComponent className="h-5 w-5 text-white" />
                </div>
                <div className="min-w-0 flex-1">
                  <h3 className="text-lg font-semibold leading-tight">{category.title}</h3>
                  <p className="text-sm leading-tight text-gray-500">{category.subtitle}</p>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-3">
              {category.data.map((model: TrendingRanking) => {
                const rankStyle = getRankStyle(model.rank)
                const isSelected = selectedModel === model.name

                return (
                  <div
                    key={model.rank}
                    className={`flex cursor-pointer items-start justify-between rounded-lg p-4 transition-all duration-200 hover:scale-105 ${rankStyle.container} ${isSelected ? 'ring-2 ring-red-400' : ''}`}
                    onClick={() => {
                      setSelectedModel(isSelected ? null : model.name)
                      if (onSelectModel) {
                        onSelectModel()
                      }
                    }}>
                    <div className="flex min-w-0 flex-1 items-start space-x-3">
                      <div
                        className={`flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full ${rankStyle.badge}`}>
                        {getRankIcon(model.rank) || (
                          <span className="text-xs font-bold">{model.rank}</span>
                        )}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="break-words pr-2 text-sm font-medium leading-tight">
                          {model.name}
                        </div>
                        <div className="mt-1 text-xs text-gray-500">{model.provider}</div>
                      </div>
                    </div>
                    <div className="ml-3 flex-shrink-0 text-right">
                      <div className={rankStyle.score}>
                        {(() => {
                          const score =
                            typeof model.score === 'number' ? model.score.toString() : model.score
                          if (
                            score.includes('Input') &&
                            score.includes('Output') &&
                            score.includes('/')
                          ) {
                            const parts = score.split('/').map(part => part.trim())
                            return (
                              <div className="space-y-1">
                                <div className="text-xs">{parts[0]}</div>
                                <div className="text-xs">{parts[1]}</div>
                              </div>
                            )
                          }
                          return score
                        })()}
                      </div>
                    </div>
                  </div>
                )
              })}
            </CardContent>
          </Card>
        )
      })}
    </div>
  )
}
