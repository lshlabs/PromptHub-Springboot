/**
 * Community 페이지
 *
 * 커뮤니티 페이지 - 사용자들이 프롬프트 리뷰를 공유하고 소통하는 공간
 */

'use client'

import { Suspense, useState, useEffect, useRef } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { ChevronUp } from 'lucide-react'
import { CommunityHeader, CommunityAction } from '@/components/community'
import { PostList } from '@/components/posts'
import { SearchBar } from '@/components/common/search-bar'
import AuthForm from '@/components/auth/auth-form'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog'
import type { SortOption } from '@/components/common/sort-selector'
import { CreatePostDialog } from '@/components/common/create-post-dialog'
import { postsApi, statsApi } from '@/lib/api'
import type { Platform, Category } from '@/types/api'
import { useMetadataUtils } from '@/lib/utils'
import { useAuthContext } from '@/components/layout/auth-provider'
import { logger } from '@/lib/logger'

// 백엔드 API에서 받아올 통계 데이터 타입
interface CommunityStats {
  activeUsers: number
  sharedPrompts: number
  averageSatisfaction: number
  totalBookmarks: number
  totalViews: number
  weeklyAdded: number
}

function CommunityPageContent() {
  const router = useRouter()
  const urlSearchParams = useSearchParams()
  const { isAuthenticated } = useAuthContext()
  const actionSectionRef = useRef<HTMLDivElement>(null)
  const { setMetadata } = useMetadataUtils()

  const normalizeSearchType = (value: string | null) => {
    const allowed = new Set(['all', 'title', 'content', 'title_content', 'author'])
    if (!value) return 'all'
    return allowed.has(value) ? value : 'all'
  }

  const getSearchTypeLabel = (value: string) => {
    switch (value) {
      case 'author':
        return '작성자'
      case 'title':
        return '제목'
      case 'content':
        return '내용'
      case 'title_content':
        return '제목+내용'
      default:
        return '전체'
    }
  }

  const initialSearchQuery = urlSearchParams.get('search') ?? ''
  const initialSearchType = normalizeSearchType(urlSearchParams.get('search_type'))

  // 상태 관리
  const [searchQuery, setSearchQuery] = useState(initialSearchQuery)
  const [searchType, setSearchType] = useState(initialSearchType)
  const [currentPage, setCurrentPage] = useState(1)
  const [sortBy, setSortBy] = useState<SortOption>('latest')
  const [selectedCategories, setSelectedCategories] = useState<string[]>([])
  const [selectedPlatforms] = useState<string[]>([])
  const [selectedModels, setSelectedModels] = useState<string[]>([])
  const [showCreateDialog, setShowCreateDialog] = useState(false)
  const [isAuthDialogOpen, setIsAuthDialogOpen] = useState(false)
  const [refreshTrigger, setRefreshTrigger] = useState(0)
  const [showTopRemote, setShowTopRemote] = useState(false)

  // 필터 데이터 상태
  const [platforms, setPlatforms] = useState<Platform[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [models, setModels] = useState<any[]>([])
  const [loadingFilters, setLoadingFilters] = useState(true)

  // 통계 데이터 상태
  const [stats, setStats] = useState<CommunityStats>({
    activeUsers: 0,
    sharedPrompts: 0,
    averageSatisfaction: 0,
    totalBookmarks: 0,
    totalViews: 0,
    weeklyAdded: 0,
  })

  // 통계 데이터 로드 (useRef로 마운트 상태 추적)
  const statsLoadedRef = useRef(false)
  const [statsLoading, setStatsLoading] = useState(false)
  const [statsResolved, setStatsResolved] = useState(false)

  useEffect(() => {
    if (statsLoadedRef.current || statsLoading) return // 이미 로드되었거나 로딩 중인 경우 스킵

    const loadStats = async () => {
      try {
        setStatsLoading(true)
        const response = await statsApi.getDashboardStats()
        const data = response.data
        setStats({
          activeUsers: data.total_users,
          sharedPrompts: data.total_posts,
          averageSatisfaction: (data.avg_satisfaction as number | undefined) ?? 0,
          totalBookmarks: data.total_bookmarks,
          totalViews: data.total_views,
          weeklyAdded: (data.weekly_added_posts as number | undefined) ?? 0,
        })
        statsLoadedRef.current = true // ref로 로드 완료 표시 (리렌더링 방지)
        setStatsResolved(true)
      } catch (err) {
        logger.error('통계 데이터 로드 실패:', err)
        statsLoadedRef.current = true // 에러 발생해도 로드 완료로 표시
        setStatsResolved(true)
      } finally {
        setStatsLoading(false)
      }
    }

    loadStats()
  }, [statsLoading])

  // 검색 매개변수 객체 (상태 기반)
  const postSearchParams = {
    search: searchQuery || undefined,
    search_type: searchQuery ? searchType || 'all' : undefined,
    categories: selectedCategories.length > 0 ? selectedCategories.join(',') : undefined,
    models: selectedModels.length > 0 ? selectedModels.join(',') : undefined,
  }

  useEffect(() => {
    const nextQuery = urlSearchParams.get('search') ?? ''
    const nextType = normalizeSearchType(urlSearchParams.get('search_type'))
    setSearchQuery(nextQuery)
    setSearchType(nextType)
    setCurrentPage(1)
  }, [urlSearchParams])

  useEffect(() => {
    if (typeof window === 'undefined') return

    const handleScroll = () => {
      setShowTopRemote(window.scrollY > 360)
    }

    handleScroll()
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  const handlePostClick = (postId: number) => {
    router.push(`/post/${postId}?from=community&from_page=${currentPage}`)
  }

  const handleSearch = (query: string, searchTypeValue: string = 'all') => {
    setSearchQuery(query)
    setSearchType(searchTypeValue || 'all')
    setCurrentPage(1) // 검색 시 1페이지로 리셋
    const next = new URLSearchParams(urlSearchParams.toString())
    if (query.trim()) {
      next.set('search', query.trim())
    } else {
      next.delete('search')
    }

    const normalizedType = searchTypeValue || 'all'
    if (normalizedType && normalizedType !== 'all') {
      next.set('search_type', normalizedType)
    } else {
      next.delete('search_type')
    }

    const nextQueryString = next.toString()
    router.replace(nextQueryString ? `/community?${nextQueryString}` : '/community')
    logger.debug('검색:', query, '타입:', normalizedType)
  }

  const handleClearSearch = () => {
    handleSearch('', 'all')
  }

  const handleScrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleCreatePost = () => {
    if (!isAuthenticated) {
      setIsAuthDialogOpen(true)
      return
    }
    setShowCreateDialog(true)
  }

  const handleCreateSuccess = () => {
    setRefreshTrigger(prev => prev + 1) // PostList 새로고침 트리거
  }

  // 액션 섹션으로 부드럽게 스크롤하는 함수
  const scrollToActionSection = () => {
    setTimeout(() => {
      if (actionSectionRef.current) {
        const element = actionSectionRef.current
        const elementPosition = element.getBoundingClientRect().top
        const offsetPosition = elementPosition + window.pageYOffset - 90 // 헤더 높이 고려하여 90px 위로

        window.scrollTo({
          top: offsetPosition,
          behavior: 'smooth',
        })
      }
    }, 100)
  }

  const handlePageChange = (page: number) => {
    setCurrentPage(page)
    logger.debug('페이지 변경:', page)
    scrollToActionSection()
  }

  const handleSortChange = (value: SortOption) => {
    setSortBy(value)
    setCurrentPage(1) // 정렬 변경 시 1페이지로 리셋
    logger.debug('정렬 변경:', value)
    scrollToActionSection()
  }

  // 필터 데이터 로드 (useRef로 마운트 상태 추적)
  const metadataLoadedRef = useRef(false)
  const [metadataLoading, setMetadataLoading] = useState(false)

  useEffect(() => {
    if (metadataLoadedRef.current || metadataLoading) return // 이미 로드되었거나 로딩 중인 경우 스킵

    const loadFilterData = async () => {
      try {
        setMetadataLoading(true)
        setLoadingFilters(true)

        const [platformsResponse, categoriesResponse, modelsResponse] = await Promise.all([
          postsApi.getPlatforms(),
          postsApi.getCategories(),
          postsApi.getModels(),
        ])

        setPlatforms(platformsResponse.data)
        setCategories(categoriesResponse.data)
        setModels(modelsResponse.data)

        // 메타데이터 유틸리티에 설정
        setMetadata(platformsResponse.data, modelsResponse.data, categoriesResponse.data)
        metadataLoadedRef.current = true // ref로 로드 완료 표시 (리렌더링 방지)
      } catch (error) {
        logger.error('필터 데이터 로드 실패:', error)
        metadataLoadedRef.current = true // 에러 발생해도 로드 완료로 표시
      } finally {
        setLoadingFilters(false)
        setMetadataLoading(false)
      }
    }

    loadFilterData()
  }, [metadataLoading])

  const handleFilterChange = (filters: {
    categories: string[]
    platforms: string[]
    models: string[]
  }) => {
    // 즉시 필터 적용
    setSelectedCategories(filters.categories)
    // selectedPlatforms는 더 이상 업데이트하지 않음 (모델 선택의 관문 역할만)
    setSelectedModels(filters.models)
    setCurrentPage(1) // 필터 변경 시 1페이지로 리셋
    logger.debug('필터 변경:', filters)
    scrollToActionSection()
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        <div className="mx-auto max-w-7xl space-y-8">
          {/* 헤더 섹션 */}
          <CommunityHeader stats={stats} loading={!statsResolved} />

          {/* 액션 섹션 (필터, 정렬, 게시글 작성) */}
          <div ref={actionSectionRef}>
            <CommunityAction
              onCreatePost={handleCreatePost}
              sortBy={sortBy}
              onSortChange={handleSortChange}
              selectedCategories={selectedCategories}
              selectedPlatforms={selectedPlatforms}
              selectedModels={selectedModels}
              onFilterChange={handleFilterChange}
              platforms={platforms}
              categories={categories}
              models={models}
              loadingFilters={loadingFilters}
              activeSearchBadge={
                searchQuery
                  ? {
                      label: getSearchTypeLabel(searchType),
                      query: searchQuery,
                    }
                  : null
              }
              onClearSearch={handleClearSearch}
            />
          </div>

          {/* 게시글 목록 */}
          <PostList
            useApi={true}
            searchParams={postSearchParams}
            sortBy={sortBy}
            onPostClick={handlePostClick}
            pagination={true}
            currentPage={currentPage}
            onPageChange={handlePageChange}
            itemsPerPage={10}
            key={refreshTrigger} // 새 게시글 작성 시 목록 새로고침
            platformsData={platforms}
            modelsData={models}
            categoriesData={categories}
          />

          {/* 검색창 */}
          <SearchBar
            onSearch={handleSearch}
            placeholder="프롬프트 리뷰 검색..."
            query={searchQuery}
            searchTypeValue={searchType}
          />
        </div>
      </div>

      {/* 게시글 작성 다이얼로그 */}
      <Dialog open={isAuthDialogOpen} onOpenChange={setIsAuthDialogOpen}>
        <DialogContent className="max-w-md p-0 [&>button]:hidden">
          <DialogTitle className="sr-only">로그인 또는 회원가입</DialogTitle>
          <DialogDescription className="sr-only">
            리뷰를 공유하려면 로그인 또는 회원가입이 필요합니다.
          </DialogDescription>
          <AuthForm
            defaultTab="login"
            onSuccess={() => {
              setIsAuthDialogOpen(false)
              setShowCreateDialog(true)
            }}
          />
        </DialogContent>
      </Dialog>
      <CreatePostDialog
        open={showCreateDialog}
        onOpenChange={setShowCreateDialog}
        onSuccess={handleCreateSuccess}
      />

      <button
        type="button"
        aria-label="최상단으로 이동"
        onClick={handleScrollToTop}
        className={`fixed bottom-6 right-4 z-40 flex h-11 w-11 items-center justify-center rounded-xl border border-gray-200 bg-white/95 text-gray-700 shadow-lg backdrop-blur transition-all duration-200 hover:-translate-y-0.5 hover:border-blue-200 hover:text-blue-600 hover:shadow-xl sm:bottom-8 sm:right-6 ${
          showTopRemote
            ? 'pointer-events-auto translate-y-0 opacity-100'
            : 'pointer-events-none translate-y-3 opacity-0'
        }`}>
        <ChevronUp className="h-5 w-5" />
      </button>
    </div>
  )
}

export default function CommunityPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-gray-50" />}>
      <CommunityPageContent />
    </Suspense>
  )
}
