/**
 * PostActions 컴포넌트
 *
 * 게시글 상세 페이지의 액션 버튼들
 * 좋아요, 북마크, 수정, 삭제 버튼을 포함
 */

'use client'

import { Heart, Bookmark, Edit, Trash2 } from 'lucide-react'
import type { PostDetail } from '@/types/api'
import CustomButton from '@/components/common/custom-button'

interface PostActionsProps {
  likes?: number
  bookmarks?: number
  isLiked?: boolean
  isBookmarked?: boolean
  onLike?: () => void
  onBookmark?: () => void
  onEdit?: () => void
  onDelete?: () => void
  className?: string
  isAuthor?: boolean
  isAuthenticated?: boolean
  post?: PostDetail
}

export function PostActions({
  likes,
  bookmarks,
  isLiked,
  isBookmarked,
  onLike,
  onBookmark,
  onEdit,
  onDelete,
  className = '',
  isAuthor,
  isAuthenticated = false,
  post,
}: PostActionsProps) {
  const finalLikes = likes ?? post?.likes ?? 0
  const finalBookmarks = bookmarks ?? post?.bookmarks ?? 0
  const finalIsLiked = isLiked ?? post?.isLiked ?? false
  const finalIsBookmarked = isBookmarked ?? post?.isBookmarked ?? false
  const finalIsAuthor = isAuthor ?? post?.isAuthor ?? false

  return (
    <div className={`flex items-center justify-between border-t border-gray-50 pt-6 ${className}`}>
      <div className="flex items-center gap-2">
        <CustomButton
          color="flat"
          border="transparent"
          shape="rounded"
          size="xs"
          className={`${
            finalIsLiked
              ? 'bg-red-50 text-red-600'
              : 'text-gray-500 hover:bg-red-50 hover:text-red-600'
          } ${!isAuthenticated ? 'cursor-not-allowed opacity-50' : ''}`}
          onClick={isAuthenticated ? onLike : undefined}
          disabled={!isAuthenticated}>
          <Heart className={`h-4 w-4 ${finalIsLiked ? 'fill-current' : ''}`} />
          <span className={`text-xs sm:text-sm ${finalIsLiked ? 'text-red-600' : ''}`}>
            {finalLikes}
          </span>
          <span
            className={`hidden text-xs sm:inline sm:text-sm ${finalIsLiked ? 'text-red-600' : ''}`}>
            좋아요
          </span>
        </CustomButton>
        <CustomButton
          color="flat"
          border="transparent"
          shape="rounded"
          size="xs"
          className={`${
            finalIsBookmarked
              ? 'bg-green-50 text-green-600'
              : 'text-gray-500 hover:bg-green-50 hover:text-green-600'
          } ${!isAuthenticated ? 'cursor-not-allowed opacity-50' : ''}`}
          onClick={isAuthenticated ? onBookmark : undefined}
          disabled={!isAuthenticated}>
          <Bookmark className={`h-4 w-4 ${finalIsBookmarked ? 'fill-current' : ''}`} />
          <span className={`text-xs sm:text-sm ${finalIsBookmarked ? 'text-green-600' : ''}`}>
            {finalBookmarks}
          </span>
          <span
            className={`hidden text-xs sm:inline sm:text-sm ${finalIsBookmarked ? 'text-green-600' : ''}`}>
            북마크
          </span>
        </CustomButton>
      </div>
      {finalIsAuthor && (
        <div className="flex gap-3">
          <CustomButton
            color="flat"
            border="blue"
            shape="rounded"
            size="responsive"
            icon={<Edit className="h-4 w-4" />}
            className="text-blue-500 hover:text-blue-600"
            onClick={onEdit}>
            <span className="hidden sm:inline">수정</span>
          </CustomButton>
          <CustomButton
            color="flat"
            border="red"
            shape="rounded"
            size="responsive"
            icon={<Trash2 className="h-4 w-4" />}
            className="text-red-500 hover:text-red-600"
            onClick={onDelete}>
            <span className="hidden sm:inline">삭제</span>
          </CustomButton>
        </div>
      )}
    </div>
  )
}
