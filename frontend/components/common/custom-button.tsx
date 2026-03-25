import { ButtonHTMLAttributes, ReactNode } from 'react'
import { cn } from '@/lib/utils'
import { useScreenSize } from '@/hooks/use-mobile'

/**
 * CommonButton 컴포넌트의 props 타입 정의
 *
 * @property {string} color - 버튼 색상 ('gradient' | 'flat')
 * @property {string} shape - 버튼 모양 ('rounded' | 'square')
 * @property {string} border - 버튼 테두리 ('gray' | 'transparent' | 'none')
 * @property {string} size - 버튼 크기 ('xs' | 'sm' | 'md' | 'lg' | 'responsive')
 * @property {ReactNode} icon - 버튼 아이콘
 * @property {string} iconPosition - 아이콘 위치 ('left' | 'right')
 * @property {ReactNode} children - 버튼 텍스트
 * @property {string} className - 추가 CSS 클래스
 * @property {boolean} disabled - 버튼 비활성화 여부
 */
export type CustomButtonProps = {
  color?: 'gradient' | 'flat'
  shape?: 'rounded' | 'square'
  border?: 'gray' | 'transparent' | 'none' | 'red' | 'blue' | 'green'
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'responsive'
  icon?: ReactNode
  iconPosition?: 'left' | 'right'
  children: ReactNode
  className?: string
} & ButtonHTMLAttributes<HTMLButtonElement>

const colorClasses = {
  gradient:
    'bg-gradient-to-r from-blue-500 to-purple-600 text-white hover:from-blue-600 hover:to-purple-700',
  flat: 'bg-transparent text-gray-600 hover:bg-gray-50',
}

const shapeClasses = {
  rounded: 'rounded-full',
  square: 'rounded-xl',
}

const borderClasses = {
  transparent: 'border border-transparent',
  gray: 'border border-gray-200 hover:border-gray-300',
  red: 'border border-red-300 hover:border-red-400',
  blue: 'border border-blue-300 hover:border-blue-400',
  green: 'border border-green-300 hover:border-green-400',
  none: 'border-none',
}

const sizeClasses = {
  xs: 'px-3 py-2 text-xs',
  sm: 'px-4 py-2 text-sm',
  md: 'px-5 py-2 text-sm',
  lg: 'px-6 py-2 text-base',
}

/**
 * 공통 버튼 컴포넌트
 *
 * 다양한 스타일과 크기를 지원하는 재사용 가능한 버튼 컴포넌트입니다.
 * 반응형 디자인을 지원하며, 아이콘과 텍스트를 함께 사용할 수 있습니다.
 *
 * @param props - CommonButtonProps
 * @param props.color - 버튼 색상 (기본값: 'gradient')
 * @param props.shape - 버튼 모양 (기본값: 'rounded')
 * @param props.border - 버튼 테두리 (기본값: 'gray')
 * @param props.size - 버튼 크기 (기본값: 'responsive')
 * @param props.icon - 버튼 아이콘
 * @param props.iconPosition - 아이콘 위치 (기본값: 'left')
 * @param props.children - 버튼 텍스트
 * @param props.className - 추가 CSS 클래스
 * @param props.disabled - 버튼 비활성화 여부
 * @returns JSX.Element
 *
 * @example
 * ```tsx
 * <CommonButton
 *   color="flat_blue"
 *   size="responsive"
 *   icon={<EditIcon />}
 *   iconPosition="right"
 *   onClick={handleEdit}
 * >
 *   수정하기
 * </CommonButton>
 * ```
 */
export function CustomButton({
  color = 'gradient',
  shape = 'rounded',
  border = 'gray',
  size = 'responsive',
  icon,
  iconPosition = 'left',
  children,
  className,
  disabled,
  ...rest
}: CustomButtonProps) {
  const screenSize = useScreenSize()

  // responsive size일 때 스크린 사이즈에 따라 동적으로 클래스 결정
  const getResponsiveSizeClass = () => {
    if (size !== 'responsive') return sizeClasses[size]

    switch (screenSize) {
      case 'mobile':
        return 'px-3 py-2 text-xs'
      case 'tablet':
        return 'px-4 py-2 text-sm'
      case 'desktop':
        return 'px-6 py-2 text-base'
      default:
        return 'px-4 py-2 text-sm' // 기본값 sm
    }
  }

  return (
    <button
      className={cn(
        'flex cursor-pointer items-center justify-center gap-2 font-medium transition-all duration-200 hover:scale-105',
        colorClasses[color],
        shapeClasses[shape],
        borderClasses[border],
        getResponsiveSizeClass(),
        disabled && 'cursor-not-allowed opacity-50 hover:opacity-50',
        className,
      )}
      disabled={disabled}
      {...rest}>
      {icon && iconPosition === 'left' && <span className="flex items-center">{icon}</span>}
      {children}
      {icon && iconPosition === 'right' && <span className="flex items-center">{icon}</span>}
    </button>
  )
}

export default CustomButton
