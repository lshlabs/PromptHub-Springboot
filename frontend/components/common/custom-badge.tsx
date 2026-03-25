/**
 * CustomBadge 컴포넌트
 *
 * 재사용 가능한 배지 컴포넌트입니다.
 * 다양한 색상 테마, 크기, 모양을 지원하며 반응형 디자인을 포함합니다.
 *
 * @param props - CustomBadgeProps
 * @param props.variant - 배지 색상 테마 ('blue' | 'gray' | 'green' | 'red' | 'yellow' | 'white')
 * @param props.size - 배지 크기 ('xs' | 'sm' | 'md' | 'responsive')
 * @param props.shape - 배지 모양 ('rounded' | 'square')
 * @param props.icon - 배지 아이콘
 * @param props.children - 배지 텍스트
 * @param props.className - 추가 CSS 클래스
 * @param props.onClick - 클릭 핸들러
 * @param props.removable - 삭제 가능한 배지 여부
 * @param props.onRemove - 삭제 핸들러
 * @returns JSX.Element
 *
 * @example
 * ```tsx
 * <CustomBadge
 *   variant="blue"
 *   size="responsive"
 *   icon={<Hash className="h-3 w-3" />}
 *   onClick={handleClick}
 * >
 *   태그명
 * </CustomBadge>
 * ```
 */

'use client'

import { ReactNode } from 'react'
import { cn } from '@/lib/utils'
import { X } from 'lucide-react'

export type CustomBadgeProps = {
  variant?: 'blue' | 'gray' | 'green' | 'red' | 'yellow' | 'white'
  size?: 'xs' | 'sm' | 'md' | 'responsive'
  shape?: 'rounded' | 'square'
  icon?: ReactNode
  children: ReactNode
  className?: string
  onClick?: () => void
  removable?: boolean
  onRemove?: () => void
}

const variantClasses = {
  blue: {
    base: 'bg-blue-50 text-blue-700 ring-blue-700/10',
    hover: 'hover:bg-blue-100',
  },
  gray: {
    base: 'bg-gray-50 text-gray-600 ring-gray-500/10',
    hover: 'hover:bg-gray-100',
  },
  green: {
    base: 'bg-green-50 text-green-700 ring-green-700/10',
    hover: 'hover:bg-green-100',
  },
  red: {
    base: 'bg-red-50 text-red-700 ring-red-700/10',
    hover: 'hover:bg-red-100',
  },
  yellow: {
    base: 'bg-yellow-50 text-yellow-700 ring-yellow-700/10',
    hover: 'hover:bg-yellow-100',
  },
  white: {
    base: 'bg-white text-gray-700 ring-gray-500/10',
    hover: 'hover:bg-gray-50',
  },
}

const sizeClasses = {
  xs: 'px-1.5 py-0.5 text-xs font-medium sm:px-2 sm:py-1',
  sm: 'px-1.5 py-0.5 text-xs font-medium sm:px-2 sm:py-1',
  md: 'px-1.5 py-0.5 text-xs font-medium sm:px-2 sm:py-1',
}

export function CustomBadge({
  variant = 'gray',
  size = 'responsive',
  shape = 'rounded',
  icon,
  children,
  className = '',
  onClick,
  removable = false,
  onRemove,
}: CustomBadgeProps) {
  // responsive size일 때 PostCard 패턴 적용
  const getResponsiveSizeClass = () => {
    if (size !== 'responsive') return sizeClasses[size]
    return 'px-1.5 py-0.5 text-xs font-medium sm:px-2 sm:py-1'
  }

  const handleRemove = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (onRemove) {
      onRemove()
    }
  }

  return (
    <div
      className={cn(
        'inline-flex items-center rounded-md ring-1 ring-inset transition-all duration-200',
        variantClasses[variant].base,
        variantClasses[variant].hover,
        getResponsiveSizeClass(),
        onClick && 'cursor-pointer',
        removable && 'pr-1', // 삭제 버튼 공간 확보
        className,
      )}
      onClick={onClick}>
      {icon && <span className="flex items-center">{icon}</span>}
      <span>{children}</span>
      {removable && (
        <button
          onClick={handleRemove}
          className="ml-1 rounded-full p-0.5 text-gray-400 hover:bg-gray-200 hover:text-gray-600"
          type="button">
          <X className="h-3 w-3" />
        </button>
      )}
    </div>
  )
}

export default CustomBadge
