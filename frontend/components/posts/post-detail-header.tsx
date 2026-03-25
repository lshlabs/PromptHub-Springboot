/**
 * PostHeader 컴포넌트
 *
 * 게시글 상세 페이지의 헤더 부분
 * 제목, 작성자 정보, 평점, 메타 정보를 포함
 */

'use client'

import { UserSummaryPopover } from '@/components/common/user-summary-popover'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Star, Clock, Eye } from 'lucide-react'
import CustomBadge from '@/components/common/custom-badge'
import { getModelName, getCategoryName, generateAvatarGradient } from '@/lib/utils'

interface PostHeaderProps {
  title: string
  author: string
  authorInitial: string
  avatarSrc?: string
  authorAvatarColor1?: string
  authorAvatarColor2?: string
  satisfaction: number
  createdAt: string
  views: number
  platform_id: number
  model_id: number
  model_etc?: string
  model_detail?: string
  category_id: number
  category_etc?: string
  className?: string
  // 메타데이터 (표시명 변환용)
  platformsData?: any[]
  modelsData?: any[]
  categoriesData?: any[]
}

const formatDateTime = (dateString: string): string => {
  try {
    const date = new Date(dateString)
    if (isNaN(date.getTime())) {
      return '날짜 형식 오류'
    }
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch (error) {
    return '날짜 변환 오류'
  }
}

export function PostHeader({
  title,
  author,
  authorInitial,
  avatarSrc,
  authorAvatarColor1,
  authorAvatarColor2,
  satisfaction,
  createdAt,
  views,
  platform_id,
  model_id,
  model_etc,
  model_detail,
  category_id,
  category_etc,
  className = '',
  platformsData,
  modelsData,
  categoriesData,
}: PostHeaderProps) {
  const formattedDateTime = formatDateTime(createdAt)

  return (
    <div className={className}>
      <div className="mb-4 flex items-center justify-between gap-2">
        <h1 className="text-gray-900">{title}</h1>
        <div className="flex shrink-0 items-center gap-1 rounded-full bg-orange-500 px-2 py-1 sm:px-3 sm:py-1.5">
          <Star className="h-3 w-3 fill-white text-white sm:h-4 sm:w-4" />
          <span className="text-xs font-medium text-white sm:text-sm">
            {Number(satisfaction).toFixed(1)}
          </span>
        </div>
      </div>
      <div className="mb-4 flex items-center justify-between">
        <UserSummaryPopover username={author} align="start">
          <div className="flex items-center gap-3">
            <Avatar className="h-8 w-8 ring-1 ring-gray-100 lg:h-10 lg:w-10">
              {typeof avatarSrc === 'string' && avatarSrc ? (
                <AvatarImage src={avatarSrc} alt={author} />
              ) : null}
              <AvatarFallback
                className="font-semibold text-white"
                style={
                  authorAvatarColor1 && authorAvatarColor2
                    ? { background: generateAvatarGradient(authorAvatarColor1, authorAvatarColor2) }
                    : undefined
                }>
                {authorInitial}
              </AvatarFallback>
            </Avatar>
            <div>
              <p className="font-semibold text-gray-900 hover:underline">{author}</p>
              <p className="text-gray-500">작성자</p>
            </div>
          </div>
        </UserSummaryPopover>
      </div>

      <div className="mb-4 flex items-center gap-4">
        <div className="flex items-center gap-1.5 text-gray-500">
          <div className="h-2 w-2 rounded-full bg-green-400"></div>
          <Clock className="h-3.5 w-3.5" />
          <span>{formattedDateTime}</span>
        </div>
        <div className="flex items-center gap-1.5 text-gray-500">
          <Eye className="h-3.5 w-3.5" />
          <span>{views}회 조회</span>
        </div>
      </div>

      <div className="mb-5 flex gap-2">
        <CustomBadge variant="blue" size="responsive">
          {(() => {
            if (model_detail && model_detail.trim()) {
              return model_detail
            }
            return getModelName(model_id, model_etc || null, modelsData || [])
          })()}
        </CustomBadge>
        <CustomBadge variant="gray" size="responsive">
          {getCategoryName(category_id, category_etc || null, categoriesData || [])}
        </CustomBadge>
      </div>

      <div className="mb-5 h-px w-full bg-gradient-to-r from-transparent via-gray-200 to-transparent"></div>
    </div>
  )
}
