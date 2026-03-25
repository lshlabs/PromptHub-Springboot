/**
 * Pagination 컴포넌트
 *
 * 공용 페이지네이션 기능을 제공합니다.
 * 백엔드 core 앱의 페이지네이션 API 응답 형식을 지원합니다.
 * community, mybookmarks, trending 등 모든 페이지에서 사용할 수 있습니다.
 */

'use client'

import { Button } from '@/components/ui/button'
import { ChevronLeft, ChevronRight } from 'lucide-react'

interface PaginationProps {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
  className?: string
  // 백엔드 API 응답과 호환성을 위한 추가 props
  hasNext?: boolean
  hasPrevious?: boolean
  totalCount?: number
}

export function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  className = '',
  hasNext,
  hasPrevious,
  totalCount,
}: PaginationProps) {
  const getVisiblePages = () => {
    const delta = 2
    const range = []
    const rangeWithDots = []

    for (
      let i = Math.max(2, currentPage - delta);
      i <= Math.min(totalPages - 1, currentPage + delta);
      i++
    ) {
      range.push(i)
    }

    if (currentPage - delta > 2) {
      rangeWithDots.push(1, '...')
    } else {
      rangeWithDots.push(1)
    }

    rangeWithDots.push(...range)

    if (currentPage + delta < totalPages - 1) {
      rangeWithDots.push('...', totalPages)
    } else {
      rangeWithDots.push(totalPages)
    }

    return rangeWithDots
  }

  // 백엔드 API 응답의 hasNext/hasPrevious를 우선 사용
  const canGoNext = hasNext !== undefined ? hasNext : currentPage < totalPages
  const canGoPrevious = hasPrevious !== undefined ? hasPrevious : currentPage > 1

  if (totalPages <= 1) return null

  return (
    <div className={`flex items-center justify-center gap-2 ${className}`}>
      {/* 이전 페이지 버튼 */}
      <Button
        variant="ghost"
        size="sm"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={!canGoPrevious}
        className="h-8 w-8 rounded-lg text-gray-600 hover:bg-gray-100 disabled:opacity-50">
        <ChevronLeft className="h-4 w-4" />
      </Button>

      {/* 페이지 번호들 */}
      {getVisiblePages().map((page, index) => (
        <div key={index}>
          {page === '...' ? (
            <span className="px-2 text-sm text-gray-400">...</span>
          ) : (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onPageChange(page as number)}
              className={`h-8 w-8 rounded-lg ${
                currentPage === page ? 'bg-gray-900 text-white' : 'text-gray-600 hover:bg-gray-100'
              }`}>
              {page}
            </Button>
          )}
        </div>
      ))}

      {/* 다음 페이지 버튼 */}
      <Button
        variant="ghost"
        size="sm"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={!canGoNext}
        className="h-8 w-8 rounded-lg text-gray-600 hover:bg-gray-100 disabled:opacity-50">
        <ChevronRight className="h-4 w-4" />
      </Button>
    </div>
  )
}
