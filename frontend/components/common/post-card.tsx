/**
 * Post Card - 4가지 바리에이션 통합 컴포넌트
 * 반응형으로 작은 화면에서 아바타 생략
 */
'use client'

import type { MouseEvent } from 'react'
import { useState } from 'react'

import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Heart, Star, Eye, Clock, BookmarkCheck, X, TrendingUp } from 'lucide-react'
import { UserSummaryPopover } from '@/components/common/user-summary-popover'
import type { PostCardData, PostCardFrontend } from '@/types/api'
import {
  formatRelativeTime,
  isBackendPostCard,
  isFrontendPostCard,
  isBookmarkPostCard,
  getIsLiked,
  formatSatisfaction,
} from '@/lib/utils'
import { generateAvatarGradient } from '@/lib/utils'
import { useMetadataUtils } from '@/lib/utils'

type CardVariant = 'normal' | 'current' | 'bookmark' | 'popular'

interface PostCardProps {
  data: PostCardData
  variant?: CardVariant
  currentPostId?: number
  onClick?: () => void
  onRemoveBookmark?: (id: number) => void
  // 메타데이터 props 추가
  platformsData?: any[]
  modelsData?: any[]
  categoriesData?: any[]
}

export function PostCard({
  data,
  variant = 'normal',
  currentPostId,
  onClick,
  onRemoveBookmark,
  // 메타데이터 props 추가
  platformsData,
  modelsData,
  categoriesData,
}: PostCardProps) {
  const [showRemoveDialog, setShowRemoveDialog] = useState(false)
  const { getModelDisplayNameFromBackend, getCategoryDisplayNameFromBackend, setMetadata } =
    useMetadataUtils()

  // 메타데이터가 전달되면 설정 (컴포넌트 렌더 시점에 동기적으로 처리)
  if (platformsData && modelsData && categoriesData) {
    setMetadata(platformsData, modelsData, categoriesData)
  }

  // 상대적 시간 결정
  const relativeTime =
    'relativeTime' in data ? data.relativeTime : formatRelativeTime(data.createdAt)

  // 현재 포스트 강조 여부 결정
  const isCurrent = data.id === currentPostId

  // 인기 포스트 여부 결정 (좋아요 50개 이상)
  const isPopular = data.likes >= 100

  // variant 결정 로직: bookmark가 최우선, popular은 bookmark가 아닐 때만 적용
  const finalVariant = variant === 'bookmark' ? 'bookmark' : isPopular ? 'popular' : variant

  // 만족도 안전하게 포맷팅
  const satisfactionDisplay = formatSatisfaction(data)

  const handleRemoveBookmark = (e: MouseEvent) => {
    e.stopPropagation()
    setShowRemoveDialog(true)
  }

  const handleConfirmRemove = () => {
    if (onRemoveBookmark) {
      onRemoveBookmark(data.id)
    }
    setShowRemoveDialog(false)
  }

  // 모델명과 카테고리 표시 로직
  const getModelDisplay = () => {
    if (isBackendPostCard(data) || isBookmarkPostCard(data)) {
      // 백엔드 데이터: 새로운 백엔드 displayName 우선 사용
      return getModelDisplayNameFromBackend({
        modelDisplayName: (data as any).modelDisplayName,
        modelId: data.modelId || null,
        modelEtc: data.modelEtc,
      })
    } else if (isFrontendPostCard(data)) {
      // 프론트엔드 샘플 데이터: model_detail 우선 확인
      const frontendData = data as PostCardFrontend
      const modelDetail = (frontendData as any).model_detail
      if (modelDetail && modelDetail.trim()) {
        return modelDetail
      }
      // model_detail이 없으면 기존 로직
      return frontendData.model === '기타' && frontendData.model_etc
        ? frontendData.model_etc
        : frontendData.model
    }
    return '모델명'
  }

  const getCategoryDisplay = () => {
    if (isBackendPostCard(data) || isBookmarkPostCard(data)) {
      // 백엔드 데이터: 새로운 백엔드 displayName 우선 사용
      return getCategoryDisplayNameFromBackend({
        categoryDisplayName: (data as any).categoryDisplayName,
        categoryId: data.categoryId,
        categoryEtc: data.categoryEtc,
      })
    } else if (isFrontendPostCard(data)) {
      // 프론트엔드 샘플 데이터: 기존 로직 유지
      return data.category === '기타' && data.category_etc ? data.category_etc : data.category
    }
    return '카테고리'
  }

  // 좋아요 상태 결정
  const isLiked = getIsLiked(data)

  // 바리에이션별 스타일 맵
  const variantStylesMap = {
    current: {
      cardBorder: 'border-2 border-blue-200 hover:border-blue-300',
      cardShadow: 'hover:shadow-xl',
      topBar: 'bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500',
      headerBg: 'bg-blue-50/30 border-blue-100',
      avatarRing: 'ring-2 ring-blue-100',
      avatarBg: 'bg-blue-100 text-blue-700',
      authorColor: 'text-gray-800',
      eyeColor: 'text-blue-500',
      eyeTextColor: 'text-blue-600',
      starBg: 'bg-orange-500 shadow-md',
      modelBg: 'bg-blue-100 text-blue-800 ring-blue-700/20',
      categoryBg: 'bg-gray-100 text-gray-700 ring-gray-500/20',
      leftBar: '',
    },
    bookmark: {
      cardBorder: 'border border-gray-100 hover:border-gray-200',
      cardShadow: 'hover:shadow-lg',
      topBar: '',
      headerBg: 'bg-gray-50/50 border-gray-100',
      avatarRing: '',
      avatarBg: 'bg-gray-100 text-gray-600',
      authorColor: 'text-gray-700',
      eyeColor: 'text-gray-400',
      eyeTextColor: 'text-gray-500',
      starBg: 'bg-orange-500',
      modelBg: 'bg-blue-50 text-blue-700 ring-blue-700/10',
      categoryBg: 'bg-gray-50 text-gray-600 ring-gray-500/10',
      leftBar: 'bg-gradient-to-b from-green-400 to-green-600',
    },
    popular: {
      cardBorder: 'border border-red-100 hover:border-red-200',
      cardShadow: 'hover:shadow-xl',
      topBar: 'bg-gradient-to-r from-red-500 via-pink-500 to-orange-500',
      headerBg: 'bg-gradient-to-br from-red-50/50 to-pink-50/30 border-red-100',
      avatarRing: 'ring-2 ring-red-100',
      avatarBg: 'bg-red-100 text-red-700',
      authorColor: 'text-gray-800',
      eyeColor: 'text-gray-400',
      eyeTextColor: 'text-gray-500',
      starBg: 'bg-gradient-to-r from-orange-500 to-red-500 shadow-lg',
      modelBg: 'bg-blue-50 text-blue-700 ring-blue-700/10',
      categoryBg: 'bg-gray-50 text-gray-600 ring-gray-500/10',
      leftBar: '',
    },
    normal: {
      cardBorder: 'border border-gray-100 hover:border-gray-200',
      cardShadow: 'hover:shadow-lg',
      topBar: '',
      headerBg: 'bg-gray-50/50 border-gray-100',
      avatarRing: '',
      avatarBg: 'bg-gray-100 text-gray-600',
      authorColor: 'text-gray-700',
      eyeColor: 'text-gray-400',
      eyeTextColor: 'text-gray-500',
      starBg: 'bg-orange-500',
      modelBg: 'bg-blue-50 text-blue-700 ring-blue-700/10',
      categoryBg: 'bg-gray-50 text-gray-600 ring-gray-500/10',
      leftBar: '',
    },
  }

  const getVariantStyles = () => {
    if (isCurrent) return variantStylesMap.current
    return (
      variantStylesMap[finalVariant as keyof typeof variantStylesMap] || variantStylesMap.normal
    )
  }

  const styles = getVariantStyles()

  return (
    <>
      <div
        className={`group cursor-pointer rounded-2xl bg-white ${styles.cardBorder} transition-all duration-200 ${styles.cardShadow} relative overflow-hidden active:scale-[0.98]`}
        onClick={onClick}
        style={{
          fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
        }}>
        {/* 상단 그라데이션 바 (현재 포스트 또는 popular만) */}
        {styles.topBar && (
          <div className={`absolute left-0 right-0 top-0 h-1 ${styles.topBar}`}></div>
        )}

        {/* 북마크 좌측 세로 바 (bookmark만) */}
        {variant === 'bookmark' && styles.leftBar && (
          <div className={`absolute bottom-0 left-0 top-0 w-1 ${styles.leftBar}`}></div>
        )}

        {/* 상단 섹션: 제목과 별점 */}
        <div
          className={`${styles.headerBg} border-b p-3 sm:p-4 md:p-5 ${variant === 'bookmark' ? 'pl-4 sm:pl-6' : ''}`}>
          <div className="mb-2 flex items-start justify-between gap-2 sm:mb-3 sm:gap-3">
            <h3 className="line-clamp-2 flex-1 text-sm font-semibold leading-tight text-gray-900 sm:text-base md:text-lg">
              {data.title}
            </h3>
            <div
              className={`flex items-center gap-1 rounded-full ${styles.starBg} shrink-0 px-2 py-1 sm:px-3 sm:py-1.5`}>
              <Star className="h-3 w-3 fill-white text-white sm:h-4 sm:w-4" />
              <span className="text-xs font-medium text-white sm:text-sm">
                {satisfactionDisplay}
              </span>
            </div>
          </div>

          {/* 모델명과 카테고리 */}
          <div className="flex flex-wrap gap-1 sm:gap-1.5">
            <span
              className={`inline-flex items-center rounded-md ${styles.modelBg} px-1.5 py-0.5 text-xs font-medium ring-1 ring-inset sm:px-2 sm:py-1`}>
              {getModelDisplay()}
            </span>
            <span
              className={`inline-flex items-center rounded-md ${styles.categoryBg} px-1.5 py-0.5 text-xs font-medium ring-1 ring-inset sm:px-2 sm:py-1`}>
              {getCategoryDisplay()}
            </span>
          </div>
        </div>

        {/* 하단 섹션: 작성자와 통계 */}
        <div
          className={`bg-white p-3 sm:p-4 md:p-5 ${variant === 'bookmark' ? 'pl-4 sm:pl-6' : ''}`}>
          <div className="mb-2 flex items-center justify-between sm:mb-3">
            {/* 작성자 정보 - 480px 이하에서 아바타 숨김 */}
            <div className="flex min-w-0 items-center gap-2 text-xs text-gray-500 sm:gap-2.5 sm:text-sm">
              <UserSummaryPopover username={data.author} align="start">
                <div className="flex min-w-0 items-center gap-2 sm:gap-2.5">
                  {/* 아바타 - 480px 이하에서 숨김 */}
                  <Avatar className={`hidden h-6 w-6 xs:block sm:h-7 sm:w-7 ${styles.avatarRing}`}>
                    {'avatarSrc' in data && typeof data.avatarSrc === 'string' && data.avatarSrc ? (
                      <AvatarImage src={data.avatarSrc} alt={data.author} />
                    ) : null}
                    <AvatarFallback
                      className={`${styles.avatarBg} text-xs font-medium`}
                      style={{
                        color: '#fff',
                        ...('authorAvatarColor1' in data &&
                        data.authorAvatarColor1 &&
                        'authorAvatarColor2' in data &&
                        data.authorAvatarColor2
                          ? {
                              background: generateAvatarGradient(
                                data.authorAvatarColor1,
                                data.authorAvatarColor2,
                              ),
                            }
                          : {}),
                      }}>
                      {data.authorInitial}
                    </AvatarFallback>
                  </Avatar>
                  <span className={`font-medium ${styles.authorColor} truncate hover:underline`}>
                    {data.author}
                  </span>
                </div>
              </UserSummaryPopover>
              <span className="shrink-0 text-gray-300">•</span>
              <div className="flex shrink-0 items-center gap-1">
                <Clock className="h-3 w-3 sm:h-3.5 sm:w-3.5" />
                <span className="whitespace-nowrap">{relativeTime}</span>
              </div>
            </div>

            {/* 통계 */}
            <div className="flex shrink-0 items-center gap-2 text-xs sm:gap-3 sm:text-sm">
              {finalVariant === 'popular' ? (
                // 인기 포스트는 좋아요 수 특별 강조
                <div className="flex items-center gap-1.5 rounded-full bg-gradient-to-r from-red-500 to-pink-500 px-2 py-1">
                  <Heart className="h-3.5 w-3.5 fill-white text-white sm:h-4 sm:w-4" />
                  <span className="font-bold text-white">{data.likes}</span>
                  <TrendingUp className="h-3 w-3 text-white" />
                </div>
              ) : (
                <div className="flex items-center gap-1">
                  <Heart
                    className={`h-3.5 w-3.5 sm:h-4 sm:w-4 ${isLiked ? 'fill-red-500 text-red-500' : 'text-gray-400'}`}
                  />
                  <span className="font-medium text-gray-500">{data.likes}</span>
                </div>
              )}
              <div className="flex items-center gap-1">
                <Eye className={`h-3.5 w-3.5 sm:h-4 sm:w-4 ${styles.eyeColor}`} />
                <span className={`font-medium ${styles.eyeTextColor}`}>{data.views}</span>
              </div>
            </div>
          </div>

          {/* 북마크 해제 알림 바 (bookmark만) */}
          {variant === 'bookmark' && (
            <div className="flex items-center justify-between rounded-lg border border-green-200 bg-green-50 px-2.5 py-1.5 sm:px-3 sm:py-2">
              <div className="flex min-w-0 items-center gap-1.5 text-xs text-green-700 sm:gap-2 sm:text-sm">
                <BookmarkCheck className="h-3.5 w-3.5 shrink-0 sm:h-4 sm:w-4" />
                <span className="truncate font-medium">북마크에 저장됨</span>
              </div>
              <button
                className="flex shrink-0 items-center gap-1 rounded-md bg-white px-1.5 py-1 text-xs font-medium text-red-600 transition-colors hover:bg-red-50 active:scale-95 sm:px-2"
                onClick={handleRemoveBookmark}>
                <X className="h-3 w-3" />
                <span>해제</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* 북마크 해제 확인 다이얼로그 - PostCard 외부에 위치 */}
      <AlertDialog open={showRemoveDialog} onOpenChange={setShowRemoveDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>북마크 해제</AlertDialogTitle>
            <AlertDialogDescription>
              이 게시글을 북마크에서 제거하시겠습니까?
              <br />
              <span className="font-medium text-gray-900">"{data.title}"</span>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleConfirmRemove}
              className="bg-red-600 text-white hover:bg-red-700">
              해제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
