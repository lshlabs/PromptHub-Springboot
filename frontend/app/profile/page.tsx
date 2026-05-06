'use client'

import { useState, useEffect, useRef } from 'react'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Card, CardHeader, CardContent } from '@/components/ui/card'
import { useRouter } from 'next/navigation'
import { useToast } from '@/hooks/use-toast'
import { ProfileInfoCard, ProfileStatsSection, ProfilePostsSection } from '@/components/profile'
import { ProfileCompleteness } from '@/components/profile/profile-completeness'
import { authApi, userDataApi, statsApi, postsApi } from '@/lib/api'
import { getDomainErrorMessage } from '@/lib/utils'
import { useAuthContext } from '@/components/layout/auth-provider'
import { useDelayedLoading } from '@/hooks/use-delayed-loading'

// 프론트엔드 사용자 데이터 타입
interface ProfileUserData {
  username: string
  bio: string
  location: string
  githubHandle: string
  profileImage: string | null
  avatarColor1: string
  avatarColor2: string
}

// PostCard 타입 import
import { API_BASE_URL, type PostCard } from '@/types/api'

export default function ProfilePage() {
  const router = useRouter()
  const { user, isAuthenticated, isLoading: authLoading, refreshUser } = useAuthContext()
  const { toast } = useToast()

  // 중복 실행 방지 플래그 (useRef 사용)
  const isLoadingDataRef = useRef(false)

  // 사용자 데이터 상태
  const [userData, setUserData] = useState<ProfileUserData>({
    username: '사용자',
    bio: '',
    location: '',
    githubHandle: '',
    profileImage: null,
    avatarColor1: '#6B73FF',
    avatarColor2: '#9EE5FF',
  })

  // 프로필 완성도 상태
  const [profileCompleteness, setProfileCompleteness] = useState<{
    percentage: number
    completed_fields: number
    total_fields: number
    missing_fields: string[]
  } | null>(null)

  // 게시글 데이터 상태
  const [userPosts, setUserPosts] = useState<PostCard[]>([])
  const [likedPosts, setLikedPosts] = useState<PostCard[]>([])
  const [bookmarkedPosts, setBookmarkedPosts] = useState<PostCard[]>([])
  const [isEditing, setIsEditing] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [hasProfileLoadedOnce, setHasProfileLoadedOnce] = useState(false)
  const [likedLoading, setLikedLoading] = useState(false)
  const [bookmarkedLoading, setBookmarkedLoading] = useState(false)
  const [likedLoaded, setLikedLoaded] = useState(false)
  const [bookmarkedLoaded, setBookmarkedLoaded] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [activeTab, setActiveTab] = useState<'my' | 'liked' | 'bookmarked'>('my')
  const [backendStats, setBackendStats] = useState({
    postsCount: 0,
    totalLikes: 0,
    totalViews: 0,
    totalBookmarks: 0,
  })

  // 메타데이터 상태 (PostList에서 중복 API 요청 방지)
  const [platformsData, setPlatformsData] = useState<any[]>([])
  const [modelsData, setModelsData] = useState<any[]>([])
  const [categoriesData, setCategoriesData] = useState<any[]>([])
  const delayedProfileLoading = useDelayedLoading(isLoading, { delayMs: 180, minVisibleMs: 320 })
  const delayedLikedLoading = useDelayedLoading(likedLoading, { delayMs: 180, minVisibleMs: 320 })
  const delayedBookmarkedLoading = useDelayedLoading(bookmarkedLoading, {
    delayMs: 180,
    minVisibleMs: 320,
  })
  const showProfileLoading = hasProfileLoadedOnce ? delayedProfileLoading : isLoading
  const showLikedLoading = likedLoaded ? delayedLikedLoading : likedLoading
  const showBookmarkedLoading = bookmarkedLoaded ? delayedBookmarkedLoading : bookmarkedLoading

  // (간소화) 백엔드→프론트 데이터 변환은 각 API에서 이미 일관화되어 있어 별도 변환 함수 생략

  // 통계 데이터 계산 (백엔드 데이터 우선 사용)
  const getStats = () => ({
    postCount: backendStats.postsCount,
    likeCount: backendStats.totalLikes,
    bookmarkCount: backendStats.totalBookmarks,
    viewCount: backendStats.totalViews,
  })

  // 백엔드 데이터를 프론트엔드 형식으로 변환
  const transformBackendData = (backendData: any): ProfileUserData => {
    const userData = backendData.user || backendData
    const normalizedProfileImage =
      typeof userData.profile_image === 'string' && userData.profile_image
        ? userData.profile_image.startsWith('http')
          ? userData.profile_image
          : `${API_BASE_URL}${userData.profile_image}`
        : null
    return {
      username: userData.username || '사용자',
      bio: userData.bio || '',
      location: userData.location || '',
      githubHandle: userData.github_handle || '',
      profileImage: normalizedProfileImage,
      avatarColor1: userData.avatar_color1 || '#6B73FF',
      avatarColor2: userData.avatar_color2 || '#9EE5FF',
    }
  }

  // 프로필 완성도 계산 함수
  const calculateProfileCompleteness = (userData: ProfileUserData) => {
    const requiredFields = ['username', 'bio', 'location', 'githubHandle']
    let completedFields = 0

    for (const field of requiredFields) {
      const value = userData[field as keyof ProfileUserData]
      if (value && String(value).trim()) {
        completedFields++
      }
    }

    const percentage = (completedFields / requiredFields.length) * 100
    const missingFields = requiredFields.filter(field => {
      const value = userData[field as keyof ProfileUserData]
      return !value || !String(value).trim()
    })

    return {
      percentage: Math.round(percentage),
      completed_fields: completedFields,
      total_fields: requiredFields.length,
      missing_fields: missingFields,
    }
  }

  // 프로필 데이터 로드 (중복 API 호출 개선)
  const loadProfileData = async () => {
    // 중복 실행 방지
    if (isLoadingDataRef.current) {
      return
    }

    try {
      isLoadingDataRef.current = true
      setIsLoading(true)

      // 인증 상태 확인
      if (!isAuthenticated) {
        router.push('/')
        return
      }

      // Promise.all을 사용하여 4개 API를 병렬로 호출 (성능 개선)
      // 사용자 게시글은 프로필 로드 후 별도 호출
      const [profileRes, platformsRes, modelsRes, categoriesRes] = await Promise.all([
        authApi.getProfile(),
        postsApi.getPlatforms(),
        postsApi.getModels(),
        postsApi.getCategories(),
      ])

      // 사용자 게시글은 프로필 로드 후 별도 호출
      let myPostsRes = null
      try {
        myPostsRes = await userDataApi.getUserPosts({
          page: 1,
          page_size: 50,
        })
      } catch (postsError) {
        toast({
          title: '일부 데이터 로드 실패',
          description: getDomainErrorMessage(
            postsError,
            '내 리뷰 목록 연결이 지연되어 프로필만 먼저 표시합니다. 내 리뷰 탭이 비어 보이면 잠시 후 탭을 다시 눌러주세요.',
            {
              unauthorized:
                '로그인 세션이 끊겨 내 리뷰를 가져오지 못했습니다. 다시 로그인한 뒤 프로필 페이지를 새로 열어주세요.',
            },
          ),
          variant: 'destructive',
        })
      }

      // 통계 API는 별도로 호출 (인증 오류 시 무시)
      let statsRes = null
      try {
        statsRes = await statsApi.getUserStats()
      } catch (statsError) {
        toast({
          title: '통계 데이터 로드 실패',
          description: getDomainErrorMessage(
            statsError,
            '조회수·좋아요 집계 서버 응답이 없어 통계를 0으로 임시 표시했습니다. 내용 확인 후 페이지를 한 번 새로고침해 주세요.',
          ),
          variant: 'destructive',
        })
      }

      if (!profileRes) {
        throw new Error('사용자 데이터를 찾을 수 없습니다.')
      }

      // 사용자 프로필 데이터 변환
      const transformedData = transformBackendData(profileRes.user)
      setUserData(transformedData)
      setHasProfileLoadedOnce(true)

      // 프로필 완성도 정보 추출
      if (profileRes.profile_completeness) {
        setProfileCompleteness(profileRes.profile_completeness)
      }

      // 사용자 게시글 (API 결과 사용)
      if (myPostsRes?.data?.results && Array.isArray(myPostsRes.data.results)) {
        setUserPosts(myPostsRes.data.results)
      }

      // 사용자 통계 (API 결과 사용, 실패 시 기본값)
      if (statsRes?.data) {
        const s = statsRes.data
        setBackendStats({
          postsCount: s.posts_count || 0,
          totalLikes: s.total_likes || 0,
          totalViews: s.total_views || 0,
          totalBookmarks: s.total_bookmarks || 0,
        })
      } else {
        // 통계 API 실패 시 기본값 설정
        setBackendStats({
          postsCount: 0,
          totalLikes: 0,
          totalViews: 0,
          totalBookmarks: 0,
        })
      }

      // 메타데이터 저장 (PostList에서 중복 API 요청 방지)
      if (platformsRes?.data) setPlatformsData(platformsRes.data)
      if (modelsRes?.data) setModelsData(modelsRes.data)
      if (categoriesRes?.data) setCategoriesData(categoriesRes.data)
    } catch (error) {
      toast({
        title: '프로필 로드 실패',
        description: getDomainErrorMessage(
          error,
          '프로필 기본 정보를 가져오지 못해 페이지를 열 수 없습니다. 네트워크를 확인한 뒤 홈에서 프로필로 다시 진입해 주세요.',
          {
            unauthorized:
              '로그인 세션이 만료되어 프로필 접근이 차단되었습니다. 다시 로그인한 뒤 프로필 메뉴를 다시 눌러주세요.',
          },
        ),
        variant: 'destructive',
      })
      router.push('/')
    } finally {
      setIsLoading(false)
      isLoadingDataRef.current = false
    }
  }

  // 사용자 정보가 있을 때 프로필 데이터 로드
  useEffect(() => {
    if (user && !authLoading && isAuthenticated) {
      loadProfileData()
    } else if (!authLoading && !isAuthenticated) {
      // 인증되지 않은 사용자는 홈으로 리다이렉트
      router.push('/')
    }
  }, [user, authLoading, isAuthenticated])

  // 탭 전환 시 좋아요/북마크 Lazy-load (무한 루프 방지)
  useEffect(() => {
    const loadLiked = async () => {
      if (likedLoaded) return // 이미 로드된 경우 스킵

      try {
        setLikedLoading(true)
        const res = await userDataApi.getLikedPosts({ page: 1, page_size: 50 })
        if (res?.data?.results) {
          setLikedPosts(res.data.results)
        } else {
          setLikedPosts([])
        }
        setLikedLoaded(true) // 로드 완료 표시
      } catch (e) {
        toast({
          title: '좋아요 목록 로드 실패',
          description: getDomainErrorMessage(
            e,
            '좋아요 탭 데이터 동기화가 지연되고 있습니다. 현재 화면은 유지되며, 좋아요 탭을 다시 눌러 재시도해 주세요.',
          ),
          variant: 'destructive',
        })
        setLikedPosts([])
        setLikedLoaded(true) // 에러 발생해도 로드 완료로 표시
      } finally {
        setLikedLoading(false)
      }
    }
    const loadBookmarked = async () => {
      if (bookmarkedLoaded) return // 이미 로드된 경우 스킵

      try {
        setBookmarkedLoading(true)
        const res = await userDataApi.getBookmarkedPosts({ page: 1, page_size: 50 })
        if (res?.data?.results) {
          setBookmarkedPosts(res.data.results)
        } else {
          setBookmarkedPosts([])
        }
        setBookmarkedLoaded(true) // 로드 완료 표시
      } catch (e) {
        toast({
          title: '북마크 목록 로드 실패',
          description: getDomainErrorMessage(
            e,
            '북마크 탭 목록을 최신 상태로 가져오지 못했습니다. 탭 재진입 후에도 같으면 페이지를 새로고침해 주세요.',
          ),
          variant: 'destructive',
        })
        setBookmarkedPosts([])
        setBookmarkedLoaded(true) // 에러 발생해도 로드 완료로 표시
      } finally {
        setBookmarkedLoading(false)
      }
    }

    // 탭 전환 시에만 로드 (이미 로드된 경우 스킵)
    if (activeTab === 'liked' && !likedLoaded && !likedLoading) {
      loadLiked()
    }
    if (activeTab === 'bookmarked' && !bookmarkedLoaded && !bookmarkedLoading) {
      loadBookmarked()
    }
  }, [activeTab, likedLoaded, bookmarkedLoaded, likedLoading, bookmarkedLoading])

  // 이벤트 핸들러들
  const handleSave = async (newUserData: ProfileUserData, profileImageFile?: File | null) => {
    try {
      setIsSaving(true)

      // 백엔드로 전송할 데이터 준비
      const updateData: any = {}
      if (newUserData.username !== userData.username) updateData.username = newUserData.username
      if (newUserData.bio !== userData.bio) updateData.bio = newUserData.bio
      if (newUserData.location !== userData.location) updateData.location = newUserData.location
      if (newUserData.githubHandle !== userData.githubHandle)
        updateData.github_handle = newUserData.githubHandle
      if (newUserData.avatarColor1 !== userData.avatarColor1)
        updateData.avatar_color1 = newUserData.avatarColor1
      if (newUserData.avatarColor2 !== userData.avatarColor2)
        updateData.avatar_color2 = newUserData.avatarColor2

      // 프로필 이미지 업로드/삭제 반영
      if (profileImageFile instanceof File) {
        updateData.profile_image = profileImageFile
      }

      if (userData.profileImage && !newUserData.profileImage) {
        updateData.remove_profile_image = true
      }

      const response = await authApi.updateProfile(updateData)
      const updatedBackendData = response.user
      if (!updatedBackendData) {
        throw new Error('업데이트된 사용자 데이터를 찾을 수 없습니다.')
      }
      const transformedData = transformBackendData(updatedBackendData)

      setUserData(transformedData)
      setIsEditing(false)

      const applyAuthorAvatarUpdate = (posts: PostCard[]) =>
        posts.map(post =>
          post.author === userData.username || post.author === transformedData.username
            ? ({
                ...post,
                author: transformedData.username,
                authorInitial: transformedData.username.charAt(0).toUpperCase() || 'U',
                authorAvatarColor1: transformedData.avatarColor1,
                authorAvatarColor2: transformedData.avatarColor2,
              } as PostCard)
            : post,
        )

      setUserPosts(prev => applyAuthorAvatarUpdate(prev))
      setLikedPosts(prev => applyAuthorAvatarUpdate(prev))
      setBookmarkedPosts(prev => applyAuthorAvatarUpdate(prev))

      // 프로필 완성도 재계산 및 업데이트
      const updatedCompleteness = calculateProfileCompleteness(transformedData)
      setProfileCompleteness(updatedCompleteness)

      toast({
        title: '성공',
        description: '프로필이 성공적으로 업데이트되었습니다.',
      })

      // 전역 상태 업데이트하여 Header 등 모든 컴포넌트 새로고침
      await refreshUser()
    } catch (error) {
      const errorMessage = getDomainErrorMessage(
        error,
        '프로필 변경사항 저장이 완료되지 않았습니다. 입력값을 확인한 뒤 저장 버튼을 다시 눌러주세요.',
        {
          unauthorized:
            '로그인 상태가 만료되어 저장 요청이 거절되었습니다. 다시 로그인한 뒤 같은 수정을 다시 저장해 주세요.',
          conflict:
            '다른 기기나 탭에서 프로필이 먼저 수정되었습니다. 최신 정보로 새로고침한 뒤 다시 저장해 주세요.',
        },
      )
      toast({
        title: '프로필 저장 실패',
        description: errorMessage,
        variant: 'destructive',
      })
    } finally {
      setIsSaving(false)
    }
  }

  const handleCancel = () => {
    setIsEditing(false)
  }

  const handlePostClick = (postId: number) => {
    router.push(`/post/${postId}?from=profile`)
  }

  const handleRemoveBookmark = async (bookmarkId: number) => {
    try {
      await postsApi.toggleBookmark(bookmarkId)
      // 북마크 목록에서 제거
      setBookmarkedPosts(prev => prev.filter(bookmark => bookmark.id !== bookmarkId))
      toast({
        title: '북마크 해제 완료',
        description: '북마크에서 제거되었습니다.',
      })
    } catch (err) {
      toast({
        title: '북마크 해제 실패',
        description: getDomainErrorMessage(
          err,
          '북마크 해제 요청이 처리 대기 상태입니다. 목록 새로고침 후 남아 있으면 다시 해제해 주세요.',
          {
            unauthorized:
              '로그인 세션이 끊겨 북마크를 해제하지 못했습니다. 재로그인 후 북마크 탭에서 다시 시도해 주세요.',
            notFound: '이미 삭제된 게시글입니다. 목록을 새로고침해 주세요.',
          },
        ),
        variant: 'destructive',
      })
    }
  }

  const handleAccountSettingsClick = () => {
    router.push('/profile/settings')
  }

  const handleRequestLocationUpdate = async (): Promise<string | null> => {
    if (typeof window === 'undefined' || !navigator.geolocation) {
      throw new Error('이 브라우저에서는 위치 기능을 지원하지 않습니다.')
    }

    const position = await new Promise<GeolocationPosition>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: false,
        timeout: 10000,
        maximumAge: 300000,
      })
    }).catch((error: GeolocationPositionError) => {
      if (error.code === error.PERMISSION_DENIED) {
        throw new Error('위치 권한이 거부되었습니다. 브라우저 권한을 확인해주세요.')
      }
      if (error.code === error.TIMEOUT) {
        throw new Error('위치 확인 시간이 초과되었습니다. 다시 시도해주세요.')
      }
      throw new Error('현재 위치를 확인하지 못했습니다.')
    })

    const { latitude, longitude } = position.coords
    const controller = new AbortController()
    const timer = window.setTimeout(() => controller.abort(), 10000)

    try {
      const url = new URL('https://nominatim.openstreetmap.org/reverse')
      url.searchParams.set('format', 'jsonv2')
      url.searchParams.set('lat', String(latitude))
      url.searchParams.set('lon', String(longitude))
      url.searchParams.set('accept-language', 'ko,en')

      const res = await fetch(url.toString(), {
        method: 'GET',
        signal: controller.signal,
      })

      if (!res.ok) {
        throw new Error('위치 정보를 변환하지 못했습니다.')
      }

      const data = await res.json()
      const address = data?.address || {}
      const city =
        address.city || address.town || address.village || address.county || address.municipality
      const state = address.state || address.province
      const country = address.country

      const best =
        city && country
          ? `${city}, ${country}`
          : state && country
            ? `${state}, ${country}`
            : typeof data?.display_name === 'string'
              ? data.display_name
                  .split(',')
                  .slice(0, 3)
                  .map((part: string) => part.trim())
                  .join(', ')
              : null

      if (!best) {
        throw new Error('위치 정보를 읽지 못했습니다. 직접 입력해주세요.')
      }

      return best
    } catch (error: any) {
      if (error?.name === 'AbortError') {
        throw new Error('위치 변환 요청 시간이 초과되었습니다. 다시 시도해주세요.')
      }
      throw error
    } finally {
      window.clearTimeout(timer)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6 text-gray-900 md:p-10">
      <div className="mx-auto grid max-w-7xl grid-cols-1 gap-8 lg:grid-cols-3">
        {/* 왼쪽 영역 */}
        <div className="space-y-8 lg:col-span-1">
          {/* 프로필 완성도 표시 */}
          {profileCompleteness && <ProfileCompleteness completeness={profileCompleteness} />}

          <ProfileInfoCard
            userData={userData}
            isEditing={isEditing}
            onEdit={() => setIsEditing(true)}
            onSave={handleSave}
            onCancel={handleCancel}
            onAccountSettings={handleAccountSettingsClick}
            onRequestLocationUpdate={handleRequestLocationUpdate}
            isLoading={showProfileLoading || isSaving}
          />
        </div>

        {/* 오른쪽 영역 */}
        <div className="space-y-8 lg:col-span-2">
          <ProfileStatsSection
            stats={getStats()}
            isLoading={showProfileLoading}
            title={''}
            contained
          />
          <Card className="overflow-hidden border border-gray-100 bg-white">
            <CardHeader className="pb-2">
              <Tabs
                value={activeTab}
                onValueChange={v => setActiveTab(v as 'my' | 'liked' | 'bookmarked')}>
                <TabsList className="grid w-full grid-cols-3">
                  <TabsTrigger value="my">내 리뷰</TabsTrigger>
                  <TabsTrigger value="liked">좋아요</TabsTrigger>
                  <TabsTrigger value="bookmarked">북마크</TabsTrigger>
                </TabsList>
              </Tabs>
            </CardHeader>
            <CardContent className="pt-2">
              {(() => {
                const postsForTab =
                  activeTab === 'my'
                    ? userPosts
                    : activeTab === 'liked'
                      ? likedPosts
                      : bookmarkedPosts
                const loadingForTab =
                  activeTab === 'my'
                    ? showProfileLoading
                    : activeTab === 'liked'
                      ? showLikedLoading
                      : showBookmarkedLoading
                const variantForTab: 'default' | 'bookmark' | 'trending' | 'user-posts' | 'liked-posts' =
                  activeTab === 'bookmarked'
                    ? 'bookmark'
                    : activeTab === 'my'
                      ? 'user-posts'
                      : 'liked-posts'
                return (
                  <ProfilePostsSection
                    posts={postsForTab}
                    onPostClick={handlePostClick}
                    onRemoveBookmark={activeTab === 'bookmarked' ? handleRemoveBookmark : undefined}
                    isLoading={loadingForTab}
                    variant={variantForTab}
                    title={''}
                    contained
                    platformsData={platformsData}
                    modelsData={modelsData}
                    categoriesData={categoriesData}
                  />
                )
              })()}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
