'use client'

import type React from 'react'
import { useState, useCallback } from 'react'
import { Star } from 'lucide-react'
import { cn } from '@/lib/utils'

interface StarRatingProps {
  rating?: number
  onRatingChange?: (rating: number) => void
  maxRating?: number
  size?: 'sm' | 'md' | 'lg'
  disabled?: boolean
  showValue?: boolean
  className?: string
}

export default function StarRating({
  rating = 0,
  onRatingChange,
  maxRating = 5,
  size = 'md',
  disabled = false,
  showValue = false,
  className,
}: StarRatingProps) {
  const [hoverRating, setHoverRating] = useState<number | null>(null)

  // 별 크기별 Tailwind 클래스
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8',
  }

  // 현재 표시할 별점(hover 중이면 hover 값, 아니면 실제 값)
  const currentRating = hoverRating !== null ? hoverRating : rating

  // 별 클릭 시 별점 변경
  const handleStarClick = useCallback(
    (starRating: number) => {
      if (disabled) return
      onRatingChange?.(starRating)
    },
    [disabled, onRatingChange],
  )

  // 별 hover 시 임시 별점 표시
  const handleStarHover = useCallback(
    (starRating: number) => {
      if (disabled) return
      setHoverRating(starRating)
    },
    [disabled],
  )

  // 마우스가 별 영역을 벗어나면 hover 별점 해제
  const handleMouseLeave = useCallback(() => {
    if (disabled) return
    setHoverRating(null)
  }, [disabled])

  // 별 하나 렌더링 (반쪽별 포함)
  const renderStar = (starIndex: number) => {
    const starValue = starIndex + 1
    const isActive = currentRating >= starValue
    const isHalfActive = currentRating >= starValue - 0.5 && currentRating < starValue
    const isHover = hoverRating !== null
    // hover 중일 때만 진한 노란색, 그 외에는 항상 동일한 노란색
    const starColor = isHover ? 'text-yellow-400' : 'text-yellow-300'

    return (
      <div
        key={starIndex}
        className="relative inline-block cursor-pointer"
        onMouseLeave={handleMouseLeave}>
        {/* 회색 배경 별 */}
        <Star
          className={cn(
            sizeClasses[size],
            'text-gray-300 transition-colors duration-150',
            disabled && 'cursor-not-allowed opacity-50',
          )}
          fill="currentColor"
        />

        {/* 노란색 오버레이(반쪽/전체) */}
        <div
          className="absolute inset-0 overflow-hidden"
          style={{
            width: isHalfActive ? '50%' : isActive ? '100%' : '0%',
          }}>
          <Star
            className={cn(sizeClasses[size], starColor, 'transition-all duration-150')}
            fill="currentColor"
          />
        </div>

        {/* 왼쪽 반쪽 클릭 영역 (0.5점) */}
        <button
          type="button"
          className={cn(
            'absolute inset-0 h-full w-1/2 rounded-l',
            disabled ? 'cursor-default' : 'cursor-pointer',
          )}
          onClick={() => handleStarClick(starValue - 0.5)}
          onMouseEnter={() => handleStarHover(starValue - 0.5)}
          disabled={disabled}
          tabIndex={-1}
        />

        {/* 오른쪽 반쪽 클릭 영역 (1점) */}
        <button
          type="button"
          className={cn(
            'absolute inset-0 left-1/2 h-full w-1/2 rounded-r',
            disabled ? 'cursor-default' : 'cursor-pointer',
          )}
          onClick={() => handleStarClick(starValue)}
          onMouseEnter={() => handleStarHover(starValue)}
          disabled={disabled}
          tabIndex={-1}
        />
      </div>
    )
  }

  return (
    <div className={cn('flex flex-col items-center gap-1', className)}>
      {/* 별 아이콘 그룹 */}
      <div className="flex items-center gap-0.5">
        {Array.from({ length: maxRating }, (_, index) => renderStar(index))}
      </div>

      {/* 별점 숫자 */}
      {showValue && (
        <span className="mt-2 text-gray-600">
          {currentRating.toFixed(1)} / {maxRating.toFixed(1)}
        </span>
      )}
    </div>
  )
}
