'use client'

import { useState, useEffect } from 'react'
import { PostCard } from '@/components/common/post-card'
import { Pagination } from '@/components/common/pagination'
import { EmptyState } from '@/components/common/empty-state'
import { postsApi } from '@/lib/api'
import type { PostCard as ApiPostCard } from '@/types/api'
import type { SortOption } from '@/components/common/sort-selector'
import { useDelayedLoading } from '@/hooks/use-delayed-loading'
import { getDomainErrorMessage } from '@/lib/utils'

type PostListVariant = 'default' | 'bookmark' | 'trending' | 'user-posts' | 'liked-posts'

interface PostListProps {
  posts?: ApiPostCard[]
  currentPostId?: number
  onPostClick?: (postId: number) => void
  onRemoveBookmark?: (id: number) => void
  onViewMore?: () => void
  variant?: PostListVariant
  onBrowsePrompts?: () => void
  pagination?: boolean
  itemsPerPage?: number
  currentPage?: number
  totalPages?: number
  onPageChange?: (page: number) => void
  className?: string
  useApi?: boolean
  searchParams?: {
    search?: string
    search_type?: string
    categories?: string
    platforms?: string
    models?: string
  }
  sortBy?: SortOption
  platformsData?: any[]
  modelsData?: any[]
  categoriesData?: any[]
}

export function PostList({
  posts: externalPosts,
  currentPostId,
  onPostClick,
  onRemoveBookmark,
  onViewMore,
  variant = 'default',
  onBrowsePrompts,
  pagination = false,
  itemsPerPage = 10,
  currentPage: externalCurrentPage,
  totalPages: externalTotalPages,
  onPageChange: externalOnPageChange,
  className = '',
  useApi = false,
  searchParams,
  sortBy = 'latest',
  platformsData: externalPlatformsData,
  modelsData: externalModelsData,
  categoriesData: externalCategoriesData,
}: PostListProps) {
  const [apiPosts, setApiPosts] = useState<ApiPostCard[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [hasFetchedOnce, setHasFetchedOnce] = useState(!useApi)
  const [apiPagination, setApiPagination] = useState({
    current_page: 1,
    total_pages: 1,
    total_count: 0,
    has_next: false,
    has_previous: false,
  })

  const [platformsData, setPlatformsData] = useState<any[]>(externalPlatformsData || [])
  const [modelsData, setModelsData] = useState<any[]>(externalModelsData || [])
  const [categoriesData, setCategoriesData] = useState<any[]>(externalCategoriesData || [])

  const [internalCurrentPage, setInternalCurrentPage] = useState(1)
  const showLoading = useDelayedLoading(loading, { delayMs: 180, minVisibleMs: 320 })

  const renderPostCardSkeletons = (count = 3) => (
    <div className={`space-y-3 ${className}`} aria-hidden="true">
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

  // 메타데이터는 상위 컴포넌트에서 전달받은 것만 사용 (중복 로드 방지)
  useEffect(() => {
    if (externalPlatformsData) setPlatformsData(externalPlatformsData)
    if (externalModelsData) setModelsData(externalModelsData)
    if (externalCategoriesData) setCategoriesData(externalCategoriesData)
  }, [externalPlatformsData, externalModelsData, externalCategoriesData])

  useEffect(() => {
    if (!useApi) return

    const loadPosts = async () => {
      try {
        setLoading(true)
        setError('')

        const page = externalCurrentPage || internalCurrentPage
        const response = await postsApi.getPosts({
          page,
          page_size: itemsPerPage,
          search: searchParams?.search,
          search_type: searchParams?.search_type,
          ...(searchParams?.categories ? { categories: searchParams.categories } : ({} as any)),
          ...(searchParams?.platforms ? { platforms: searchParams.platforms } : ({} as any)),
          models: searchParams?.models,
          sort_by: sortBy,
        })

        setApiPosts(response.data.results)
        setApiPagination(response.data.pagination)
      } catch (err) {
        setError(
          getDomainErrorMessage(
            err,
            '게시글 목록을 불러오지 못했습니다. 필터를 줄이거나 잠시 후 다시 시도해주세요.',
            {
              unauthorized: '로그인이 만료되었습니다. 다시 로그인 후 목록을 새로고침해주세요.',
            },
          ),
        )
        setApiPosts([])
      } finally {
        setLoading(false)
        setHasFetchedOnce(true)
      }
    }

    loadPosts()
  }, [
    useApi,
    externalCurrentPage,
    internalCurrentPage,
    itemsPerPage,
    searchParams?.search,
    searchParams?.search_type,
    searchParams?.categories,
    searchParams?.platforms,
    searchParams?.models,
    sortBy,
  ])

  const rawPosts = useApi ? apiPosts : externalPosts || []

  const getProcessedPosts = () => {
    if (useApi) return rawPosts

    let filtered = [...rawPosts]

    if (searchParams?.search) {
      const query = searchParams.search.toLowerCase()
      const normalizedSearchType = (searchParams.search_type || 'all').toLowerCase()
      filtered = filtered.filter(post => {
        const matchesTitle = post.title.toLowerCase().includes(query)
        const matchesAuthor = post.author.toLowerCase().includes(query)
        const matchesContent = false // 로컬 데이터에는 상세 본문이 없어 내용 검색 불가

        if (normalizedSearchType === 'author') return matchesAuthor
        if (normalizedSearchType === 'title') return matchesTitle
        if (normalizedSearchType === 'content') return matchesContent
        if (normalizedSearchType === 'title_content') return matchesTitle || matchesContent
        return matchesTitle || matchesAuthor || matchesContent
      })
    }

    if (searchParams?.categories) {
      const categoryIds = searchParams.categories.split(',')
      filtered = filtered.filter(post => categoryIds.includes(post.categoryId.toString()))
    }

    if (searchParams?.platforms) {
      const platformIds = searchParams.platforms.split(',')
      filtered = filtered.filter(post => platformIds.includes(post.platformId.toString()))
    }

    filtered.sort((a, b) => {
      switch (sortBy) {
        case 'latest':
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        case 'oldest':
          return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        case 'popular':
          return b.likes - a.likes
        case 'satisfaction':
          return Number(b.satisfaction) - Number(a.satisfaction)
        case 'views':
          return b.views - a.views
        default:
          return 0
      }
    })

    return filtered
  }

  const posts = getProcessedPosts()

  const isExternalPagination =
    (externalCurrentPage !== undefined &&
      externalTotalPages !== undefined &&
      externalOnPageChange !== undefined) ||
    useApi

  const currentPage = isExternalPagination
    ? externalCurrentPage || (useApi ? apiPagination.current_page : 1)
    : internalCurrentPage

  const totalPages = isExternalPagination
    ? useApi
      ? apiPagination.total_pages
      : externalTotalPages || 1
    : Math.ceil(posts.length / itemsPerPage)

  const startIndex = ((currentPage || 1) - 1) * itemsPerPage
  const endIndex = startIndex + itemsPerPage
  const currentPosts = useApi ? posts : pagination ? posts.slice(startIndex, endIndex) : posts

  const handlePageChange = (page: number) => {
    if (useApi && externalOnPageChange) {
      externalOnPageChange(page)
    } else if (isExternalPagination && externalOnPageChange) {
      externalOnPageChange(page)
    } else {
      setInternalCurrentPage(page)
    }
  }

  if (useApi && !hasFetchedOnce) {
    return renderPostCardSkeletons(3)
  }

  if (useApi && showLoading && posts.length === 0) {
    return renderPostCardSkeletons(3)
  }

  if (useApi && error) {
    return <div className={`rounded-lg bg-red-50 p-4 text-red-700 ${className}`}>{error}</div>
  }

  if (posts.length === 0) {
    // 검색 상황인지 판단 (검색어가 있거나 필터가 적용된 경우)
    const isSearching =
      searchParams?.search ||
      searchParams?.search_type ||
      searchParams?.categories ||
      searchParams?.platforms ||
      searchParams?.models

    const emptyStateType = isSearching ? 'search' : variant === 'default' ? 'posts' : variant

    return (
      <EmptyState
        type={emptyStateType}
        onAction={variant === 'bookmark' ? onBrowsePrompts : undefined}
        className={className}
      />
    )
  }

  return (
    <div className={`space-y-4 ${className}`}>
      {useApi && showLoading && posts.length > 0 ? (
        <div className="rounded-lg border border-gray-100 bg-white/80 px-4 py-2 text-sm text-gray-500">
          목록을 업데이트하는 중...
        </div>
      ) : null}
      {currentPosts.map(post => (
        <PostCard
          key={post.id}
          data={post}
          variant={variant === 'bookmark' ? 'bookmark' : 'normal'}
          currentPostId={currentPostId}
          onClick={() => onPostClick?.(post.id)}
          onRemoveBookmark={onRemoveBookmark}
          platformsData={platformsData}
          modelsData={modelsData}
          categoriesData={categoriesData}
        />
      ))}
      {pagination && totalPages > 1 && (
        <div className="mt-8">
          <Pagination
            currentPage={currentPage || 1}
            totalPages={totalPages || 1}
            onPageChange={handlePageChange}
          />
        </div>
      )}
    </div>
  )
}
