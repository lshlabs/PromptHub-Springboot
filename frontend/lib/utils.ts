// ===========================================
// 통합 유틸리티 함수들 - 모든 헬퍼 함수와 변환 로직
// ===========================================

import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'
import {
  Platform,
  AiModel,
  Category,
  PostCard,
  PostDetail,
  PostCardData,
  PostCardFrontend,
  BookmarkedPostCard,
  ApiRequestError,
  ValidationError,
  UserData,
  UserProfileResponse,
  API_BASE_URL,
} from '@/types/api'

// ===========================================
// CSS 클래스 유틸리티
// ===========================================

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

// ===========================================
// 아바타 유틸리티
// ===========================================

export function getAvatarGradientStyle(color1?: string | null, color2?: string | null): string {
  const start = color1 || '#6B73FF'
  const end = color2 || '#9EE5FF'
  return `linear-gradient(135deg, ${start} 0%, ${end} 100%)`
}

export function getAvatarInitialFromUsername(
  username?: string | null,
  fallback: string = 'U',
): string {
  const first = username?.trim()?.charAt(0)
  if (!first) return fallback
  return first.toUpperCase()
}

export function generateRandomAvatarColors(): { color1: string; color2: string } {
  const hue1 = Math.floor(Math.random() * 360)
  const hueOffset = 40 + Math.floor(Math.random() * 120) // 40~159도 차이
  const hue2 = (hue1 + hueOffset) % 360
  const saturation1 = 70 + Math.floor(Math.random() * 20) // 70~89
  const saturation2 = 70 + Math.floor(Math.random() * 20)
  const lightness1 = 50 + Math.floor(Math.random() * 10) // 50~59
  const lightness2 = 62 + Math.floor(Math.random() * 10) // 62~71

  const hslToHex = (h: number, s: number, l: number) => {
    const sNorm = s / 100
    const lNorm = l / 100
    const c = (1 - Math.abs(2 * lNorm - 1)) * sNorm
    const x = c * (1 - Math.abs(((h / 60) % 2) - 1))
    const m = lNorm - c / 2
    let r = 0
    let g = 0
    let b = 0

    if (h < 60) [r, g, b] = [c, x, 0]
    else if (h < 120) [r, g, b] = [x, c, 0]
    else if (h < 180) [r, g, b] = [0, c, x]
    else if (h < 240) [r, g, b] = [0, x, c]
    else if (h < 300) [r, g, b] = [x, 0, c]
    else [r, g, b] = [c, 0, x]

    const toHex = (v: number) => Math.round((v + m) * 255).toString(16).padStart(2, '0')
    return `#${toHex(r)}${toHex(g)}${toHex(b)}`.toUpperCase()
  }

  return {
    color1: hslToHex(hue1, saturation1, lightness1),
    color2: hslToHex(hue2, saturation2, lightness2),
  }
}

// ===========================================
// 메타데이터 변환 유틸리티 (통합)
// ===========================================

// 메타데이터 매니저 클래스
export class MetadataManager {
  private platforms: Platform[] = []
  private models: AiModel[] = []
  private categories: Category[] = []

  // 데이터 설정
  setPlatforms(platforms: Platform[]) {
    this.platforms = platforms
  }

  setModels(models: AiModel[]) {
    this.models = models
  }

  setCategories(categories: Category[]) {
    this.categories = categories
  }

  // 모든 데이터 한번에 설정
  setAllMetadata(platforms: Platform[], models: AiModel[], categories: Category[]) {
    this.setPlatforms(platforms)
    this.setModels(models)
    this.setCategories(categories)
  }

  // ID → Name 변환
  getPlatformName(id: number): string {
    const platform = this.platforms.find(p => p.id === id)
    return platform?.name || '알 수 없는 플랫폼'
  }

  getModelName(id: number): string {
    const model = this.models.find(m => m.id === id)
    return model?.name || '알 수 없는 모델'
  }

  getCategoryName(id: number): string {
    const category = this.categories.find(c => c.id === id)
    return category?.name || '알 수 없는 카테고리'
  }

  // Name → ID 변환
  getPlatformId(name: string): number | null {
    const platform = this.platforms.find(p => p.name === name)
    return platform?.id || null
  }

  getModelId(name: string): number | null {
    const model = this.models.find(m => m.name === name)
    return model?.id || null
  }

  getCategoryId(name: string): number | null {
    const category = this.categories.find(c => c.name === name)
    return category?.id || null
  }

  // 표시명 결정 (레거시 - 하위 호환성을 위해 유지)
  getModelDisplayName(modelId: number | null | undefined, modelEtc = ''): string {
    if (modelEtc && modelEtc.trim()) {
      return modelEtc
    }
    if (modelId) {
      return this.getModelName(modelId)
    }
    return '알 수 없는 모델'
  }

  getCategoryDisplayName(categoryId: number, categoryEtc = ''): string {
    if (categoryEtc && categoryEtc.trim()) {
      return categoryEtc
    }
    return this.getCategoryName(categoryId)
  }

  // 표시명 결정 (백엔드 displayName 우선 - 새로운 방식)
  getModelDisplayNameFromBackend(post: {
    modelDisplayName?: string
    modelId?: number | null
    modelEtc?: string
  }): string {
    if (post.modelDisplayName && post.modelDisplayName.trim()) {
      return post.modelDisplayName
    }
    return this.getModelDisplayName(post.modelId, post.modelEtc)
  }

  getCategoryDisplayNameFromBackend(post: {
    categoryDisplayName?: string
    categoryId: number
    categoryEtc?: string
  }): string {
    if (post.categoryDisplayName && post.categoryDisplayName.trim()) {
      return post.categoryDisplayName
    }
    return this.getCategoryDisplayName(post.categoryId, post.categoryEtc)
  }

  // 관련 데이터 가져오기
  getModelsByPlatformId(platformId: number): AiModel[] {
    return this.models.filter(m => m.platform === platformId)
  }

  getModelsByPlatformName(platformName: string): AiModel[] {
    const platform = this.platforms.find(p => p.name === platformName)
    if (!platform) return []
    return this.getModelsByPlatformId(platform.id)
  }
}

// 싱글톤 인스턴스
export const metadataManager = new MetadataManager()

// Hook 스타일 함수들 (React 컴포넌트에서 사용)
export const useMetadataUtils = () => {
  return {
    // ID → Name 변환
    getPlatformName: (id: number) => metadataManager.getPlatformName(id),
    getModelName: (id: number) => metadataManager.getModelName(id),
    getCategoryName: (id: number) => metadataManager.getCategoryName(id),

    // Name → ID 변환
    getPlatformId: (name: string) => metadataManager.getPlatformId(name),
    getModelId: (name: string) => metadataManager.getModelId(name),
    getCategoryId: (name: string) => metadataManager.getCategoryId(name),

    // 표시명 결정 (레거시 - 하위 호환성을 위해 유지, 새 개발에서는 FromBackend 함수 사용 권장)
    getModelDisplayName: (modelId: number | null, modelEtc = '') =>
      metadataManager.getModelDisplayName(modelId, modelEtc),
    getCategoryDisplayName: (categoryId: number, categoryEtc = '') =>
      metadataManager.getCategoryDisplayName(categoryId, categoryEtc),

    // 표시명 결정 (백엔드 displayName 우선 - 새로운 방식)
    getModelDisplayNameFromBackend: (post: {
      modelDisplayName?: string
      modelId?: number | null
      modelEtc?: string
    }) => metadataManager.getModelDisplayNameFromBackend(post),
    getCategoryDisplayNameFromBackend: (post: {
      categoryDisplayName?: string
      categoryId: number
      categoryEtc?: string
    }) => metadataManager.getCategoryDisplayNameFromBackend(post),

    // 데이터 설정
    setMetadata: (platforms: Platform[], models: AiModel[], categories: Category[]) =>
      metadataManager.setAllMetadata(platforms, models, categories),

    // 관련 데이터 가져오기
    getModelsByPlatformId: (platformId: number) =>
      metadataManager.getModelsByPlatformId(platformId),
    getModelsByPlatformName: (platformName: string) =>
      metadataManager.getModelsByPlatformName(platformName),
  }
}

// ===========================================
// ID 기반 표시명 처리 유틸리티 함수들 (레거시 호환)
// ===========================================

export function getPlatformName(platformId: number, platforms: Platform[]): string {
  const platform = platforms.find(p => p.id === platformId)
  return platform?.name || '알 수 없음'
}

export function getModelName(
  modelId: number | null | undefined,
  modelEtc: string | null | undefined,
  models: AiModel[],
): string {
  // model_etc가 있으면 우선 사용
  if (modelEtc && modelEtc.trim()) {
    return modelEtc
  }

  // modelId가 있으면 모델 이름 조회
  if (modelId) {
    const model = models.find(m => m.id === modelId)
    return model?.name || '알 수 없는 모델'
  }

  return '알 수 없는 모델'
}

export function getCategoryName(
  categoryId: number,
  categoryEtc: string | null | undefined,
  categories: Category[],
): string {
  // category_etc가 있으면 우선 사용
  if (categoryEtc && categoryEtc.trim()) {
    return categoryEtc
  }

  // categoryId로 카테고리 이름 조회
  const category = categories.find(c => c.id === categoryId)
  return category?.name || '알 수 없는 카테고리'
}

export function getModelsByPlatform(platformId: number, models: AiModel[]): AiModel[] {
  return models.filter(m => m.platform === platformId)
}

// ===========================================
// 데이터 변환 유틸리티 함수들
// ===========================================

// PostCard 데이터에 표시명 추가 (통합 타입 지원)
export function enrichPostCard(
  post: PostCardData,
  platforms: Platform[],
  models: AiModel[],
  categories: Category[],
): PostCardData & {
  platformName: string
  modelName: string
  categoryName: string
} {
  if (isBackendPostCard(post)) {
    // 백엔드 PostCard 타입
    return {
      ...post,
      platformName: getPlatformName(post.platformId, platforms),
      modelName: getModelName(post.modelId, post.modelEtc, models),
      categoryName: getCategoryName(post.categoryId, post.categoryEtc, categories),
    }
  } else if (isFrontendPostCard(post)) {
    // 프론트엔드 PostCardFrontend 타입
    return {
      ...post,
      platformName: post.platform,
      modelName: post.modelEtc || post.model_etc || post.model,
      categoryName: post.categoryEtc || post.category_etc || post.category,
    }
  } else if (isBookmarkPostCard(post)) {
    return {
      ...post,
      platformName: getPlatformName(post.platformId, platforms),
      modelName: getModelName(post.modelId, post.modelEtc, models),
      categoryName: getCategoryName(post.categoryId, post.categoryEtc, categories),
    }
  }

  // 기본적으로 PostCard로 처리
  const postCard = post as PostCard
  return {
    ...postCard,
    platformName: getPlatformName(postCard.platformId, platforms),
    modelName: getModelName(postCard.modelId, postCard.modelEtc, models),
    categoryName: getCategoryName(postCard.categoryId, postCard.categoryEtc, categories),
  }
}

// PostDetail 데이터에 표시명 추가
export function enrichPostDetail(
  post: PostDetail,
  platforms: Platform[],
  models: AiModel[],
  categories: Category[],
): PostDetail & {
  platformName: string
  modelName: string
  categoryName: string
} {
  return {
    ...post,
    platformName: getPlatformName(post.platformId, platforms),
    modelName: getModelName(post.modelId, post.modelEtc, models),
    categoryName: getCategoryName(post.categoryId, post.categoryEtc, categories),
  }
}

// ===========================================
// 시간 관련 유틸리티 함수들
// ===========================================

export function formatRelativeTime(dateString: string): string {
  const date = new Date(dateString)
  const now = new Date()
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000)

  if (diffInSeconds < 60) {
    return '방금 전'
  }

  const diffInMinutes = Math.floor(diffInSeconds / 60)
  if (diffInMinutes < 60) {
    return `${diffInMinutes}분 전`
  }

  const diffInHours = Math.floor(diffInMinutes / 60)
  if (diffInHours < 24) {
    return `${diffInHours}시간 전`
  }

  const diffInDays = Math.floor(diffInHours / 24)
  if (diffInDays < 7) {
    return `${diffInDays}일 전`
  }

  const diffInWeeks = Math.floor(diffInDays / 7)
  if (diffInWeeks < 4) {
    return `${diffInWeeks}주 전`
  }

  const diffInMonths = Math.floor(diffInDays / 30)
  if (diffInMonths < 12) {
    return `${diffInMonths}개월 전`
  }

  const diffInYears = Math.floor(diffInDays / 365)
  return `${diffInYears}년 전`
}

export function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

export function formatDateTime(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// ===========================================
// 만족도 관련 유틸리티 함수들
// ===========================================

export function formatSatisfaction(data: PostCardData): string {
  // 직접 satisfaction 속성에 접근
  const satisfaction = (data as any).satisfaction

  // 문자열인 경우 숫자로 변환
  let satisfactionNumber: number | undefined

  if (typeof satisfaction === 'string') {
    satisfactionNumber = parseFloat(satisfaction)
  } else if (typeof satisfaction === 'number') {
    satisfactionNumber = satisfaction
  }

  // 숫자인지 확인하고 안전하게 처리
  if (typeof satisfactionNumber === 'number' && !isNaN(satisfactionNumber)) {
    return satisfactionNumber.toFixed(1)
  }

  // satisfaction이 없거나 숫자가 아닌 경우 기본값 반환
  return '0.0'
}

export function formatSatisfactionPercent(satisfaction: number): string {
  return `${Math.round(satisfaction * 20)}%`
}

// ===========================================
// 에러 처리 유틸리티 함수들
// ===========================================

// API 에러에서 사용자 친화적 메시지 추출
export function getErrorMessage(error: ApiRequestError | Error | unknown): string {
  if (!error) {
    return '알 수 없는 오류가 발생했습니다.'
  }

  // ApiRequestError 타입인 경우
  if (typeof error === 'object' && error !== null && 'message' in error) {
    return String(error.message)
  }

  // Error 객체인 경우
  if (error instanceof Error) {
    return error.message
  }

  // 문자열인 경우
  if (typeof error === 'string') {
    return error
  }

  return '알 수 없는 오류가 발생했습니다.'
}

const getHttpStatus = (error: unknown): number | null => {
  if (!error || typeof error !== 'object') return null
  if ('status' in error && typeof (error as { status?: unknown }).status === 'number') {
    return (error as { status: number }).status
  }
  if (
    'response' in error &&
    error.response &&
    typeof (error as { response?: { status?: unknown } }).response?.status === 'number'
  ) {
    return (error as { response: { status: number } }).response.status
  }
  return null
}

export function getDomainErrorMessage(
  error: unknown,
  fallback: string,
  overrides?: Partial<{
    unauthorized: string
    forbidden: string
    notFound: string
    conflict: string
    rateLimited: string
    serverError: string
    networkError: string
  }>,
): string {
  const status = getHttpStatus(error)
  const message = getErrorMessage(error)
  const normalized = message.toLowerCase()

  const merged = {
    unauthorized: '로그인 세션이 만료되었습니다. 다시 로그인한 뒤 같은 작업을 다시 시도해주세요.',
    forbidden: '이 작업을 수행할 권한이 없습니다. 권한이 있는 계정으로 다시 시도해주세요.',
    notFound: '요청한 항목을 찾지 못했습니다. 목록을 새로고침하고 항목 존재 여부를 확인해주세요.',
    conflict: '다른 변경과 충돌했습니다. 화면을 새로고침한 뒤 다시 시도해주세요.',
    rateLimited: '요청이 잠시 제한되었습니다. 잠시 기다린 뒤 다시 시도해주세요.',
    serverError: '서버 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.',
    networkError: '네트워크 연결이 불안정합니다. 연결 상태를 확인한 뒤 다시 시도해주세요.',
    ...overrides,
  }

  if (status === 401) return merged.unauthorized
  if (status === 403) return merged.forbidden
  if (status === 404) return merged.notFound
  if (status === 409) return merged.conflict
  if (status === 429) return merged.rateLimited
  if (status !== null && status >= 500) return merged.serverError

  if (
    normalized.includes('network') ||
    normalized.includes('fetch') ||
    normalized.includes('timeout') ||
    normalized.includes('aborted')
  ) {
    return merged.networkError
  }

  return message || fallback
}

// ValidationError에서 필드별 에러 메시지 추출
export function getFieldErrors(error: ValidationError): Record<string, string> {
  const fieldErrors: Record<string, string> = {}

  if (error.errors && typeof error.errors === 'object') {
    Object.entries(error.errors).forEach(([field, messages]) => {
      if (Array.isArray(messages) && messages.length > 0) {
        fieldErrors[field] = messages.join(', ')
      }
    })
  }

  return fieldErrors
}

// ===========================================
// 아바타 관련 유틸리티 함수들
// ===========================================

export function generateAvatarGradient(color1: string, color2: string): string {
  return `linear-gradient(135deg, ${color1} 0%, ${color2} 100%)`
}

export function getInitials(name: string): string {
  return name
    .split(' ')
    .map(word => word.charAt(0))
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

// ===========================================
// 필터링 관련 유틸리티 함수들
// ===========================================

export function formatFilterConditions(
  filters: Record<string, any>,
  platforms: Platform[],
  categories: Category[],
): string[] {
  const conditions: string[] = []

  if (filters.platforms && filters.platforms.length > 0) {
    const platformNames = filters.platforms
      .map((id: number) => platforms.find(p => p.id === id)?.name)
      .filter(Boolean)
    if (platformNames.length > 0) {
      conditions.push(`플랫폼: ${platformNames.join(', ')}`)
    }
  }

  if (filters.categories && filters.categories.length > 0) {
    const categoryNames = filters.categories
      .map((id: number) => categories.find(c => c.id === id)?.name)
      .filter(Boolean)
    if (categoryNames.length > 0) {
      conditions.push(`카테고리: ${categoryNames.join(', ')}`)
    }
  }

  if (filters.search && filters.search.trim()) {
    conditions.push(`검색: "${filters.search}"`)
  }

  if (filters.sort && filters.sort !== 'latest') {
    const sortNames: Record<string, string> = {
      popular: '인기순',
      satisfaction: '만족도순',
      views: '조회수순',
      likes: '좋아요순',
    }
    conditions.push(`정렬: ${sortNames[filters.sort] || filters.sort}`)
  }

  return conditions
}

// ===========================================
// 로컬 스토리지 유틸리티 함수들
// ===========================================

// 안전한 로컬 스토리지 읽기
export function getFromLocalStorage<T>(key: string, defaultValue: T): T {
  if (typeof window === 'undefined') {
    return defaultValue
  }

  try {
    const item = localStorage.getItem(key)
    return item ? JSON.parse(item) : defaultValue
  } catch {
    return defaultValue
  }
}

// 안전한 로컬 스토리지 쓰기
export function setToLocalStorage<T>(key: string, value: T): boolean {
  if (typeof window === 'undefined') {
    return false
  }

  try {
    localStorage.setItem(key, JSON.stringify(value))
    return true
  } catch {
    return false
  }
}

// ===========================================
// 타입 가드 함수들
// ===========================================

export const isBackendPostCard = (data: PostCardData): data is PostCard => {
  return 'platformId' in data && 'modelId' in data && 'categoryId' in data
}

export const isFrontendPostCard = (data: PostCardData): data is PostCardFrontend => {
  return 'platform' in data && 'model' in data && 'category' in data
}

export const isBookmarkPostCard = (data: PostCardData): data is BookmarkedPostCard => {
  return 'isBookmarked' in data && 'relativeTime' in data && 'bookmarks' in data
}

// ===========================================
// PostCard 데이터 노말라이제이션 함수들
// ===========================================

// PostCardData를 PostCard 형태로 노말라이즈
export function normalizePostCard(data: PostCardData): PostCard {
  if (isBackendPostCard(data)) {
    return data
  }

  if (isFrontendPostCard(data)) {
    // PostCardFrontend를 PostCard로 변환
    return {
      id: data.id,
      title: data.title,
      author: data.author,
      authorInitial: data.authorInitial,
      avatarSrc: typeof data.avatarSrc === 'string' ? data.avatarSrc : undefined,
      createdAt: data.createdAt,
      relativeTime: formatRelativeTime(data.createdAt),
      views: data.views,
      platformId: 1, // 기본값, 또는 platform 문자열에서 ID로 변환 필요
      modelId: 1, // 기본값, 또는 model 문자열에서 ID로 변환 필요
      categoryId: 1, // 기본값, 또는 category 문자열에서 ID로 변환 필요
      modelEtc: data.modelEtc || data.model_etc,
      categoryEtc: data.categoryEtc || data.category_etc,
      likes: data.likes,
      isLiked: data.isLiked ?? data.isliked ?? false,
      bookmarks: 0, // PostCardFrontend에는 bookmarks 정보가 없음
      isBookmarked: false, // PostCardFrontend에는 bookmark 정보가 없음
      satisfaction: data.satisfaction,
      tags: [], // PostCardFrontend에는 tags 정보가 없음
    }
  }

  if (isBookmarkPostCard(data)) {
    return {
      id: data.id,
      title: data.title,
      author: data.author,
      authorInitial: data.authorInitial,
      avatarSrc: data.avatarSrc || undefined,
      createdAt: data.createdAt,
      relativeTime: data.relativeTime,
      views: data.views,
      platformId: data.platformId,
      modelId: data.modelId || undefined,
      categoryId: data.categoryId,
      modelEtc: data.modelEtc,
      categoryEtc: data.categoryEtc,
      likes: data.likes,
      isLiked: data.isLiked,
      bookmarks: data.bookmarks,
      isBookmarked: data.isBookmarked,
      satisfaction: data.satisfaction,
      tags: data.tags,
    }
  }

  // 기본적으로 PostCard로 간주
  return data as PostCard
}

// PostCardData 배열을 PostCard 배열로 노말라이즈
export function normalizePostCards(data: PostCardData[]): PostCard[] {
  return data.map(normalizePostCard)
}

// ===========================================
// 좋아요/북마크 상태 확인 함수들
// ===========================================

export function getIsLiked(post: PostCardData): boolean {
  if (isBackendPostCard(post) || isBookmarkPostCard(post)) {
    return post.isLiked
  }
  if (isFrontendPostCard(post)) {
    return post.isLiked ?? post.isliked ?? false
  }
  return false
}

export function getIsBookmarked(post: PostCardData): boolean {
  if (isBackendPostCard(post) || isBookmarkPostCard(post)) {
    return post.isBookmarked
  }
  return false
}

// ===========================================
// 사용자 데이터 변환 함수들
// ===========================================

export function transformBackendData(backendData: UserProfileResponse): UserData {
  const userData = (backendData as any).user || backendData
  const profileImage =
    typeof userData.profile_image === 'string' && userData.profile_image
      ? userData.profile_image.startsWith('http')
        ? userData.profile_image
        : `${API_BASE_URL}${userData.profile_image}`
      : null
  return {
    id: userData.id,
    username: userData.username,
    email: userData.email,
    bio: userData.bio || '',
    location: userData.location || '',
    github_handle: userData.github_handle || '',
    profile_image: profileImage,
    avatar_color1: userData.avatar_color1 || '#6B73FF',
    avatar_color2: userData.avatar_color2 || '#9EE5FF',
    created_at: userData.created_at,
  }
}
