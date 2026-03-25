'use client'

import { use, useState, useEffect } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { GoBackButton } from '@/components/common/go-back-button'
import { PostHeader, PostContentSections, PostActions, PostList } from '@/components/posts'
import type { PostDetail } from '@/types/api'
import { useAuth } from '@/hooks/use-auth'
import { postsApi } from '@/lib/api'
import { useToast } from '@/hooks/use-toast'
import { getDomainErrorMessage } from '@/lib/utils'
import { useDelayedLoading } from '@/hooks/use-delayed-loading'

export default function PostDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { isAuthenticated, user, isLoading } = useAuth()
  const { toast } = useToast()

  const { id } = use(params)

  const [initialPage, setInitialPage] = useState(1)
  const [fromPage, setFromPage] = useState<'community' | 'trending' | 'bookmarks' | 'profile'>(
    'community',
  )
  const backButtonLabel =
    fromPage === 'trending' || fromPage === 'profile' ? '뒤로가기' : '목록으로'

  useEffect(() => {
    let fromValue = searchParams.get('from')
    let fromPage = searchParams.get('from_page')
    if (!fromPage || !fromValue) {
      const urlParams = new URLSearchParams(window.location.search)
      fromPage = fromPage || urlParams.get('from_page')
      fromValue = fromValue || urlParams.get('from')
    }

    if (
      fromValue === 'community' ||
      fromValue === 'trending' ||
      fromValue === 'bookmarks' ||
      fromValue === 'profile'
    ) {
      setFromPage(fromValue)
    }

    if (fromPage) {
      const page = parseInt(fromPage)
      if (!isNaN(page) && page > 0) {
        setInitialPage(page)
      }
    }
  }, [searchParams])

  const [postData, setPostData] = useState<PostDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [currentPage, setCurrentPage] = useState(1)
  const [hasViewIncremented, setHasViewIncremented] = useState(false)

  const [platformsData, setPlatformsData] = useState<any[]>([])
  const [modelsData, setModelsData] = useState<any[]>([])
  const [categoriesData, setCategoriesData] = useState<any[]>([])

  const [localLikeState, setLocalLikeState] = useState<{ isLiked: boolean; count: number } | null>(
    null,
  )
  const [localBookmarkState, setLocalBookmarkState] = useState<{
    isBookmarked: boolean
    count: number
  } | null>(null)
  const showPageLoading = useDelayedLoading(loading, { delayMs: 180, minVisibleMs: 320 })

  useEffect(() => {
    setCurrentPage(initialPage)
  }, [initialPage])

  useEffect(() => {
    const loadMetadata = async () => {
      try {
        const [platformsRes, modelsRes, categoriesRes] = await Promise.all([
          postsApi.getPlatforms(),
          postsApi.getModels(),
          postsApi.getCategories(),
        ])

        setPlatformsData(platformsRes.data || [])
        setModelsData(modelsRes.data || [])
        setCategoriesData(categoriesRes.data || [])
      } catch (error) {
        toast({
          title: '메타데이터 로드 실패',
          description: getDomainErrorMessage(
            error,
            '플랫폼/모델 표시 정보를 불러오지 못해 기본값으로 표시합니다. 새로고침하면 복구될 수 있습니다.',
            {
              unauthorized:
                '로그인 상태를 확인할 수 없어 기본 메타데이터로 표시합니다. 다시 로그인해 주세요.',
            },
          ),
          variant: 'destructive',
        })
        setPlatformsData([
          { id: 1, name: 'OpenAI' },
          { id: 2, name: 'Google' },
          { id: 3, name: 'Anthropic' },
          { id: 99, name: '기타' },
        ])
        setModelsData([
          { id: 1, name: 'o3', platform: 1, platform_name: 'OpenAI' },
          { id: 2, name: 'Gemini 2.5 Pro', platform: 2, platform_name: 'Google' },
          { id: 3, name: 'Claude 3.5 Sonnet', platform: 3, platform_name: 'Anthropic' },
          { id: 99, name: '기타', platform: 99, platform_name: '기타' },
        ])
        setCategoriesData([
          { id: 1, name: '창작' },
          { id: 2, name: '분석' },
          { id: 3, name: '코딩' },
          { id: 99, name: '기타' },
        ])
      }
    }

    loadMetadata()
  }, [])

  useEffect(() => {
    setLocalLikeState(null)
    setLocalBookmarkState(null)
    setHasViewIncremented(false)
  }, [id])

  useEffect(() => {
    if (isLoading) return // 인증정보 로딩 중이면 대기
    if (hasViewIncremented) return // 이미 조회수를 증가시켰으면 재실행 방지

    const fetchPostDetail = async () => {
      try {
        setLoading(true)
        setError(null)
        const response = await postsApi.getPost(Number(id))
        let postData = response.data
        if (postData.isAuthor === undefined && user) {
          postData = {
            ...postData,
            isAuthor: postData.author === user.username,
          }
        }

        if (localLikeState) {
          postData = {
            ...postData,
            likes: localLikeState.count,
            isLiked: localLikeState.isLiked,
          }
        }
        if (localBookmarkState) {
          postData = {
            ...postData,
            bookmarks: localBookmarkState.count,
            isBookmarked: localBookmarkState.isBookmarked,
          }
        }

        setPostData(postData)
        setHasViewIncremented(true) // 조회수 증가 완료 플래그 설정
      } catch (err) {
        setError(err instanceof Error ? err.message : '게시글을 불러오는데 실패했습니다.')
      } finally {
        setLoading(false)
      }
    }
    fetchPostDetail()
  }, [id, isLoading, hasViewIncremented])

  const handlePostClick = (postId: number) => {
    router.push(`/post/${postId}?from=${fromPage}&from_page=${currentPage}`)
  }

  const handleEdit = () => {
    if (!postData) return
    const urlParams = new URLSearchParams(window.location.search)
    const fromParam = urlParams.get('from') || 'community'
    router.push(`/edit-post/${postData.id}?from=${fromParam}`)
  }

  const handleLike = async () => {
    if (!postData || !isAuthenticated) return

    try {
      const response = await postsApi.toggleLike(postData.id)

      if ((response as any).message) {
        toast({
          title: '알림',
          description: (response as any).message,
        })
      } else {
        const newLikeState = {
          isLiked: response.data.is_liked ?? false,
          count: response.data.like_count ?? 0,
        }
        setLocalLikeState(newLikeState)

        setPostData((prev: PostDetail | null) =>
          prev
            ? {
                ...prev,
                likes: response.data.like_count ?? prev.likes,
                isLiked: response.data.is_liked ?? prev.isLiked,
              }
            : null,
        )

        toast({
          title: response.data.is_liked ? '좋아요 완료' : '좋아요 취소',
          description: response.data.is_liked
            ? '게시글에 좋아요를 눌렀습니다.'
            : '좋아요를 취소했습니다.',
        })
      }
    } catch (err: any) {
      toast({
        title: '좋아요 실패',
        description: getDomainErrorMessage(
          err,
          '좋아요 요청이 반영되지 않았습니다. 잠시 후 다시 눌러주세요.',
          {
            unauthorized: '로그인 세션이 만료되었습니다. 다시 로그인 후 좋아요를 시도해 주세요.',
            forbidden: '작성자 본인 게시글에는 좋아요를 누를 수 없습니다.',
          },
        ),
        variant: 'destructive',
      })
    }
  }

  const handleBookmark = async () => {
    if (!postData || !isAuthenticated) return

    try {
      const response = await postsApi.toggleBookmark(postData.id)

      if ((response as any).message) {
        toast({
          title: '알림',
          description: (response as any).message,
        })
      } else {
        const newBookmarkState = {
          isBookmarked: response.data.is_bookmarked ?? false,
          count: response.data.bookmark_count ?? 0,
        }
        setLocalBookmarkState(newBookmarkState)

        setPostData((prev: PostDetail | null) =>
          prev
            ? {
                ...prev,
                bookmarks: response.data.bookmark_count ?? prev.bookmarks,
                isBookmarked: response.data.is_bookmarked ?? prev.isBookmarked,
              }
            : null,
        )

        toast({
          title: response.data.is_bookmarked ? '북마크 완료' : '북마크 취소',
          description: response.data.is_bookmarked
            ? '게시글을 북마크했습니다.'
            : '북마크를 취소했습니다.',
        })
      }
    } catch (err: any) {
      toast({
        title: '북마크 실패',
        description: getDomainErrorMessage(
          err,
          '북마크 요청이 반영되지 않았습니다. 네트워크 상태를 확인하고 다시 시도해주세요.',
          {
            unauthorized: '로그인 세션이 만료되었습니다. 다시 로그인 후 북마크를 시도해 주세요.',
            forbidden: '작성자 본인 게시글은 북마크할 수 없습니다.',
            notFound: '이미 삭제된 게시글일 수 있습니다. 목록으로 돌아가 확인해주세요.',
          },
        ),
        variant: 'destructive',
      })
    }
  }

  const handleDelete = async () => {
    if (!postData) return
    if (!isAuthenticated) {
      toast({
        title: '로그인이 필요합니다',
        description: '삭제하려면 먼저 로그인해주세요.',
        variant: 'destructive',
      })
      return
    }
    const isAuthor = postData.isAuthor || (user ? postData.author === user.username : false)
    if (!isAuthor) {
      toast({
        title: '권한 없음',
        description: '이 게시글을 삭제할 권한이 없습니다.',
        variant: 'destructive',
      })
      return
    }
    const confirmed = window.confirm('정말로 이 게시글을 삭제하시겠습니까? 되돌릴 수 없습니다.')
    if (!confirmed) return

    try {
      const res = await postsApi.deletePost(postData.id)
      toast({ title: '삭제 완료', description: res.message || '게시글이 삭제되었습니다.' })
      router.push('/community')
    } catch (err: any) {
      toast({
        title: '삭제 실패',
        description: err?.message || '게시글 삭제 중 오류가 발생했습니다.',
        variant: 'destructive',
      })
    }
  }

  const handleViewMore = () => {
    router.push('/community')
  }

  const handlePageChange = (page: number) => {
    setCurrentPage(page)
    const url = new URL(window.location.href)
    url.searchParams.set('from_page', page.toString())
    window.history.replaceState({}, '', url.toString())
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50">
        <div className="container mx-auto px-2 py-2 pb-4 sm:px-4 sm:py-4">
          <div className="mx-auto max-w-7xl">
            <div className="px-2 py-2">
              <div className="pb-2">
                <div className="h-8 w-24 animate-pulse rounded bg-gray-200" />
              </div>
              <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
                <div className="h-1 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500" />
                <div className="space-y-5 p-6">
                  <div className="flex items-start justify-between gap-3">
                    <div className="h-8 w-2/3 animate-pulse rounded bg-gray-200" />
                    <div className="h-7 w-16 animate-pulse rounded-full bg-orange-100" />
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 animate-pulse rounded-full bg-gray-200" />
                    <div className="space-y-2">
                      <div className="h-4 w-24 animate-pulse rounded bg-gray-200" />
                      <div className="h-4 w-16 animate-pulse rounded bg-gray-100" />
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <div className="h-6 w-20 animate-pulse rounded bg-blue-100" />
                    <div className="h-6 w-24 animate-pulse rounded bg-gray-100" />
                  </div>
                  <div className="h-32 animate-pulse rounded-xl bg-gray-100" />
                  <div className="h-40 animate-pulse rounded-xl bg-gray-100" />
                  <div className="h-20 animate-pulse rounded-xl bg-gray-100" />
                  <div className="h-12 animate-pulse rounded-xl bg-gray-100" />
                </div>
              </div>
              {showPageLoading ? (
                <div className="mt-4 space-y-3" aria-hidden="true">
                  <div className="h-5 w-28 animate-pulse rounded bg-gray-200" />
                  <div className="rounded-2xl border border-gray-100 bg-white p-4 shadow-sm">
                    <div className="h-24 animate-pulse rounded-xl bg-gray-100" />
                  </div>
                </div>
              ) : null}
            </div>
          </div>
        </div>
      </div>
    )
  }

  if (error || !postData) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-50 to-blue-50">
        <div className="text-center">
          <h2 className="mb-2 text-xl font-semibold text-gray-900">오류가 발생했습니다</h2>
          <p className="mb-4 text-gray-600">{error || '게시글을 찾을 수 없습니다.'}</p>
          <button
            onClick={() => router.push('/community')}
            className="rounded-md bg-blue-600 px-4 py-2 text-white hover:bg-blue-700">
            커뮤니티로 돌아가기
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50">
      <div className="container mx-auto px-2 py-2 pb-4 sm:px-4 sm:py-4">
        <div className="mx-auto max-w-7xl">
          <div className="px-2 py-2">
            {/* 목록으로 가기 버튼 */}
            <div className="flex justify-start pb-2">
              <GoBackButton fromPage={fromPage} fallbackPath="/community" label={backButtonLabel} />
            </div>

            {/* 메인 포스트 카드 */}
            <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
              {/* 상단 그라데이션 바 */}
              <div className="h-1 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500"></div>

              <div className="p-6">
                {/* 헤더 섹션 */}
                <PostHeader
                  title={postData.title}
                  author={postData.author}
                  authorInitial={postData.authorInitial}
                  avatarSrc={postData.avatarSrc || undefined}
                  authorAvatarColor1={(postData as any).authorAvatarColor1}
                  authorAvatarColor2={(postData as any).authorAvatarColor2}
                  satisfaction={Number(postData.satisfaction)}
                  createdAt={postData.createdAt}
                  views={postData.views}
                  platform_id={postData.platformId}
                  model_id={postData.modelId || 0}
                  model_etc={postData.modelEtc}
                  model_detail={(postData as any).modelDetail || (postData as any).model_detail}
                  category_id={postData.categoryId}
                  category_etc={postData.categoryEtc}
                  platformsData={platformsData}
                  modelsData={modelsData}
                  categoriesData={categoriesData}
                />

                {/* 내용 섹션들 */}
                <PostContentSections
                  prompt={postData.prompt}
                  aiResponse={postData.aiResponse || ''}
                  additionalOpinion={postData.additionalOpinion || ''}
                  tags={postData.tags}
                />

                {/* 액션 버튼들 */}
                <PostActions
                  likes={postData.likes}
                  bookmarks={postData.bookmarks}
                  isLiked={postData.isLiked}
                  isBookmarked={postData.isBookmarked}
                  onLike={handleLike}
                  onBookmark={handleBookmark}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  isAuthor={postData.isAuthor || (user ? postData.author === user.username : false)}
                  isAuthenticated={isAuthenticated}
                  post={postData}
                />
              </div>
            </div>

            {/* 다른 리뷰들 섹션 */}
            <div className="pt-4">
              <div className="p-2">
                <h3 className="mb-1 text-lg font-bold text-gray-900">다른 리뷰들</h3>
                <p className="text-sm text-gray-500">더 많은 프롬프트 리뷰를 확인해보세요</p>
              </div>

              <PostList
                useApi={true}
                currentPostId={parseInt(id)}
                onPostClick={handlePostClick}
                onViewMore={handleViewMore}
                pagination={true}
                itemsPerPage={10}
                currentPage={currentPage}
                onPageChange={handlePageChange}
                searchParams={{}}
                platformsData={platformsData}
                modelsData={modelsData}
                categoriesData={categoriesData}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
