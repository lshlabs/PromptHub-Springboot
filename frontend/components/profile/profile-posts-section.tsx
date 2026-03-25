'use client'

import { PostList } from '@/components/posts'
import type { PostCard } from '@/types/api'

interface ProfilePostsSectionProps {
  posts: PostCard[]
  onPostClick: (postId: number) => void
  onRemoveBookmark?: (bookmarkId: number) => void
  isLoading?: boolean
  variant?: 'default' | 'bookmark' | 'trending' | 'user-posts' | 'liked-posts'
  title?: string
  contained?: boolean
  platformsData?: any[]
  modelsData?: any[]
  categoriesData?: any[]
}

export function ProfilePostsSection({
  posts,
  onPostClick,
  onRemoveBookmark,
  isLoading = false,
  variant = 'user-posts',
  title = '내 리뷰',
  contained = false,
  platformsData,
  modelsData,
  categoriesData,
}: ProfilePostsSectionProps) {
  const renderPostCardSkeletons = (count = 3) => (
    <div className="space-y-3" aria-hidden="true">
      {Array.from({ length: count }).map((_, idx) => (
        <div key={idx} className="rounded-2xl border border-gray-100 bg-white p-4 shadow-sm">
          <div className="space-y-3">
            <div className="h-5 w-2/3 animate-pulse rounded bg-gray-200" />
            <div className="flex gap-2">
              <div className="h-5 w-20 animate-pulse rounded bg-blue-100" />
              <div className="h-5 w-24 animate-pulse rounded bg-gray-100" />
            </div>
            <div className="flex items-center justify-between pt-1">
              <div className="flex items-center gap-2">
                <div className="h-7 w-7 animate-pulse rounded-full bg-gray-200" />
                <div className="h-4 w-24 animate-pulse rounded bg-gray-200" />
                <div className="h-4 w-16 animate-pulse rounded bg-gray-100" />
              </div>
              <div className="flex items-center gap-3">
                <div className="h-4 w-8 animate-pulse rounded bg-gray-100" />
                <div className="h-4 w-8 animate-pulse rounded bg-gray-100" />
              </div>
            </div>
          </div>
        </div>
      ))}
    </div>
  )

  const Wrapper: React.FC<{ children: React.ReactNode }> = ({ children }) =>
    contained ? (
      <>{children}</>
    ) : (
      <div className="rounded-lg border border-gray-100 bg-white p-6 shadow-sm">{children}</div>
    )

  return (
    <Wrapper>
      {title ? <h2 className="mb-4 text-xl font-bold text-gray-900">{title}</h2> : null}
      {isLoading ? (
        renderPostCardSkeletons(3)
      ) : (
        <PostList
          posts={posts}
          onPostClick={onPostClick}
          onRemoveBookmark={onRemoveBookmark}
          variant={variant}
          pagination={true}
          itemsPerPage={5}
          platformsData={platformsData}
          modelsData={modelsData}
          categoriesData={categoriesData}
        />
      )}
    </Wrapper>
  )
}
