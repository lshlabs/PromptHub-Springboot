// ===========================================
// API 타입 정의 - Spring Boot 백엔드 통신 기준
// ===========================================
// 표준화된 API 계약:
// 1) Authorization 헤더는 `Authorization: Bearer ...` 형식을 사용한다.
// 2) 백엔드는 후행 슬래시 없는 canonical URI를 기준으로 처리한다.
// 일부 프론트 상수에 남아 있는 trailing slash는 엣지 redirect를 통해 canonical URI로 정규화된다.

// ===========================================
// 공통 타입들
// ===========================================

export interface ApiResponse<T> {
  status: 'success' | 'error'
  data: T
  message?: string
}

export interface PaginatedResponse<T> {
  results: T[]
  pagination: {
    current_page: number
    total_pages: number
    total_count: number
    has_next: boolean
    has_previous: boolean
  }
}

export interface ErrorDetail {
  field?: string
  message: string
  code?: string
}

export interface ValidationError {
  detail?: string
  errors?: { [key: string]: string[] }
  non_field_errors?: string[]
}

// ===========================================
// Users 앱 관련 타입들
// ===========================================

export interface UserRegistrationRequest {
  email: string
  password: string
  password_confirm: string
}

export interface UserRegistrationResponse {
  id: number
  email: string
  username: string
  avatar_color1: string
  avatar_color2: string
  created_at: string
  token: string
  user: UserData
  message: string
}

export interface UserLoginRequest {
  email: string
  password: string
}

export interface UserLoginResponse {
  token: string
  user: UserData
  message: string
}

export interface UserData {
  id: number
  email: string
  username: string
  bio?: string
  location?: string
  github_handle?: string
  profile_image?: string
  avatar_color1: string
  avatar_color2: string
  created_at: string
}

export interface UserSummaryDTO {
  username: string
  bio?: string | null
  avatar_url?: string | null
  avatar_color1: string
  avatar_color2: string
  created_at: string
  post_count: number
  total_views: number
  total_likes_received: number
  total_bookmarks_received: number
}

export interface UserProfileCompleteness {
  percentage: number
  completed_fields: number
  total_fields: number
  missing_fields: string[]
}

export interface UserProfileEnvelope {
  user: UserData
  settings: UserSettingsDTO
  profile_completeness?: UserProfileCompleteness
}

export type UserProfileResponse = UserProfileEnvelope

export interface UserProfileUpdateResponse {
  message: string
  user: UserData
}

export interface AvatarRegenerateResponse {
  message: string
  user: UserData
}

export interface UserProfileUpdateRequest {
  username?: string
  bio?: string
  location?: string
  github_handle?: string
  profile_image?: File
  remove_profile_image?: boolean
  avatar_color1?: string
  avatar_color2?: string
}

export interface PasswordChangeRequest {
  current_password: string
  new_password: string
  new_password_confirm: string
}

export interface ChangePasswordResponse {
  message: string
  token?: string
}

export interface UserSettingsDTO {
  email_notifications_enabled: boolean
  in_app_notifications_enabled: boolean
  public_profile: boolean
  data_sharing: boolean
  two_factor_auth_enabled: boolean
  updated_at: string
}

export interface UserSessionDTO {
  key: string
  user_agent?: string | null
  ip_address?: string | null
  device?: string | null
  browser?: string | null
  os?: string | null
  location?: string | null
  created_at: string
  last_active: string
  revoked_at?: string | null
}

export interface TokenRefreshRequest {
  refresh: string
}

export interface TokenRefreshResponse {
  access: string
}

// ===========================================
// Posts 앱 관련 타입들
// ===========================================

export interface Platform {
  id: number
  name: string
}

export interface AiModel {
  id: number
  name: string
  platform: number
  platformName: string
}

export interface Category {
  id: number
  name: string
}

export interface Tag {
  name: string
  count: number
}

export interface PostCard {
  id: number
  title: string
  author: string
  authorInitial: string
  avatarSrc?: string
  authorAvatarColor1?: string
  authorAvatarColor2?: string
  createdAt: string
  relativeTime: string
  views: number
  platformId: number
  modelId?: number
  categoryId: number
  modelEtc?: string
  modelDetail?: string
  categoryEtc?: string
  modelDisplayName?: string // 백엔드에서 제공하는 계산된 모델명
  categoryDisplayName?: string // 백엔드에서 제공하는 계산된 카테고리명
  likes: number
  isLiked: boolean
  bookmarks: number
  isBookmarked: boolean
  satisfaction?: number
  tags: string[]
}

// 기존 프론트엔드 샘플 데이터와 호환되는 타입
export interface PostCardFrontend {
  id: number
  title: string
  satisfaction: number
  author: string
  authorInitial: string
  avatarSrc?: string | { color1: string; color2: string; username: string }
  createdAt: string
  views: number
  likes: number
  isLiked: boolean
  isliked?: boolean
  platform: string
  model: string
  modelEtc?: string
  model_etc?: string
  category: string
  categoryEtc?: string
  category_etc?: string
}

export interface BookmarkedPostCard {
  id: number
  title: string
  author: string
  authorInitial: string
  avatarSrc: string | null
  authorAvatarColor1?: string
  authorAvatarColor2?: string
  createdAt: string
  relativeTime: string
  views: number
  satisfaction: number
  platformId: number
  modelId: number | null
  modelEtc: string
  categoryId: number
  categoryEtc: string
  tags: string[]
  likes: number
  isLiked: boolean
  bookmarks: number
  isBookmarked: boolean
}

// 통합 타입 (기존 컴포넌트와 호환)
export type PostCardData = PostCard | PostCardFrontend | BookmarkedPostCard

export interface PostDetail extends PostCard {
  prompt: string
  aiResponse: string
  additionalOpinion?: string
  isAuthor: boolean
}

export interface PostCreateRequest {
  title: string
  platform: number
  model?: number
  model_etc?: string
  model_detail?: string
  category: number
  category_etc?: string
  tags?: string[]
  satisfaction?: number
  prompt: string
  ai_response: string
  additional_opinion?: string
}

export interface PostUpdateRequest extends PostCreateRequest {
  id: number
}

export interface PostEditData {
  id: number
  title: string
  platformId: number
  modelId?: number
  categoryId: number
  modelEtc?: string
  categoryEtc?: string
  satisfaction?: number
  prompt: string
  aiResponse: string
  additionalOpinion?: string
  tags: string[]
}

export interface PostInteractionRequest {
  is_liked?: boolean
  is_bookmarked?: boolean
}

export interface PostInteractionResponse {
  is_liked?: boolean
  is_bookmarked?: boolean
  like_count?: number
  bookmark_count?: number
}

export interface PostListParams {
  page?: number
  page_size?: number
  search?: string
  platform?: number
  category?: number
  author?: string
  sort?: string
  tags?: string
  satisfaction_min?: number
  satisfaction_max?: number
}

export interface PlatformModelsResponse {
  models: AiModel[]
  default_model?: AiModel
}

// 모델 자동완성 관련 타입
export interface ModelSuggestion {
  id: number
  name: string
  platform: {
    id: number
    name: string
  }
  // similarity_score 제거됨
  // released_order 제거됨
}

export interface ModelSuggestResponse {
  status: 'success' | 'error'
  data: {
    query: string
    suggestions: ModelSuggestion[]
    total_count: number
  }
  message: string
}

// 사용자별 목록 파라미터 (이름 기반 필터)
export interface UserPostListParams {
  page?: number
  page_size?: number
  search?: string
  category?: string
  platform?: string
  sort?: string
  ordering?: string
  author?: string
}

// ===========================================
// Core 앱 관련 타입들
// ===========================================

// 트렌딩 관련 타입들
export interface TrendingRanking {
  rank: number
  name: string
  score: string | number
  provider: string
}

export interface TrendingCategory {
  title: string
  subtitle: string
  icon: string
  data: TrendingRanking[]
}

export interface CategoryRankings {
  [key: string]: TrendingCategory
}

export interface TrendingResponse {
  status: 'success' | 'error'
  data: CategoryRankings
  message?: string
  from_cache?: boolean
}

// 트렌딩 모델 상세 정보
export interface TrendingModelInfo {
  trending_name: string
  provider: string
  score: string | number
  rank: number
  category: {
    name: string
    title: string
  }
  related_model: {
    id: number
    name: string
    platform: string
  } | null
  related_posts_count: number
}

export interface TrendingModelInfoResponse {
  status: 'success' | 'error'
  data: TrendingModelInfo
  message?: string
}

// 트렌딩 모델 관련 게시글
export interface TrendingModelPostsResponse extends PaginatedResponse<PostCard> {
  trending_model: TrendingModelInfo
}

export interface SearchParams {
  q: string
  search_type?: string
  page?: number
  page_size?: number
  sort?: string
  platform?: number
  category?: number
  satisfaction_min?: number
  satisfaction_max?: number
}

export interface SortOption {
  value: string
  label: string
  description?: string
}

export interface FilterOption {
  type: 'platform' | 'category' | 'satisfaction' | 'date'
  label: string
  options: Array<{
    value: string | number
    label: string
    count?: number
  }>
}

export interface FilterOptions {
  platforms: FilterOption
  categories: FilterOption
  satisfaction: FilterOption
  date: FilterOption
}

// ===========================================
// API 엔드포인트 타입들
// ===========================================

export interface ApiEndpoints {
  // Users 앱
  auth: {
    register: '/api/auth/register/'
    login: '/api/auth/login/'
    logout: '/api/auth/logout/'
    refresh: '/api/auth/token/refresh/'
    profile: '/api/auth/profile/'
    avatarRegenerate: '/api/auth/profile/avatar/regenerate/'
    passwordChange: '/api/auth/profile/password/'
    userInfo: '/api/auth/info/'
    profileDelete: '/api/auth/profile/delete/'
    settings: '/api/auth/profile/settings/'
    sessions: '/api/auth/profile/sessions/'
    google: '/api/auth/google/'
    userSummary: (username: string) => string
  }

  // Posts 앱
  posts: {
    list: '/api/posts/'
    create: '/api/posts/'
    detail: (id: number) => string
    update: (id: number) => string
    delete: (id: number) => string
    like: (id: number) => string
    bookmark: (id: number) => string
    liked: '/api/posts/liked-posts/'
    bookmarked: '/api/posts/bookmarked-posts/'
    my: '/api/posts/my-posts/'
    platforms: '/api/posts/platforms/'
    models: '/api/posts/models/'
    modelsSuggest: '/api/posts/models/suggest/'
    platformModels: (platformId: number) => string
    categories: '/api/posts/categories/'
    tags: '/api/posts/tags/'
  }

  // Core 앱
  core: {
    search: '/api/core/search/'
    sortOptions: '/api/core/sort-options/'
    filterOptions: '/api/core/filter-options/'
    trending: {
      categoryRankings: '/api/core/trending/category-rankings/'
      refreshCache: '/api/core/trending/refresh-cache/'
      modelPosts: '/api/core/trending/model/'
      modelInfo: '/api/core/trending/model/'
    }
  }

  // Stats 앱
  stats: {
    dashboard: '/api/stats/dashboard/'
    user: '/api/stats/user/'
  }
}

// ===========================================
// HTTP 메서드별 타입들
// ===========================================

export interface GetRequest {
  url: string
  params?: Record<string, any>
  headers?: Record<string, string>
}

export interface PostRequest<T = any> {
  url: string
  data?: T
  headers?: Record<string, string>
}

export interface PutRequest<T = any> extends PostRequest<T> {}

export interface PatchRequest<T = any> extends PostRequest<T> {}

export interface DeleteRequest {
  url: string
  headers?: Record<string, string>
}

// ===========================================
// 인증 관련 타입들
// ===========================================

export interface AuthTokens {
  access: string
  refresh: string
}

export interface AuthState {
  isAuthenticated: boolean
  user: UserData | null
  tokens: AuthTokens | null
}

// ===========================================
// 에러 처리 타입들
// ===========================================

export interface ApiError {
  status: number
  message: string
  detail?: string
  errors?: ValidationError
  timestamp?: string
}

export interface NetworkError {
  message: string
  code?: string
  isNetworkError: true
}

export type ApiRequestError = ApiError | NetworkError

// ===========================================
// 유틸리티 타입들
// ===========================================

export interface MetadataResponse {
  platforms: Platform[]
  categories: Category[]
  models: AiModel[]
  tags: Tag[]
}

export interface UploadResponse {
  url: string
  filename: string
  size: number
}

// ===========================================
// React Query 관련 타입들 (선택사항)
// ===========================================

export interface QueryParams {
  enabled?: boolean
  staleTime?: number
  cacheTime?: number
  refetchOnWindowFocus?: boolean
}

export interface MutationOptions<TData = any, TError = ApiRequestError, TVariables = any> {
  onSuccess?: (data: TData) => void
  onError?: (error: TError) => void
  onSettled?: () => void
}

// ===========================================
// 폼 관련 타입들
// ===========================================

export interface FormFieldError {
  message: string
  type?: string
}

export interface FormErrors {
  [key: string]: FormFieldError
}

export interface FormState<T = any> {
  data: T
  errors: FormErrors
  isLoading: boolean
  isSubmitting: boolean
  isDirty: boolean
  isValid: boolean
}

// ===========================================
// 타입 가드 함수들
// ===========================================

export const isApiError = (error: any): error is ApiError => {
  return error && typeof error.status === 'number' && typeof error.message === 'string'
}

export const isNetworkError = (error: any): error is NetworkError => {
  return error && error.isNetworkError === true
}

export const isValidationError = (error: any): error is ValidationError => {
  return error && (error.errors || error.non_field_errors || error.detail)
}

// ===========================================
// 상수들
// ===========================================

// 클라이언트(브라우저)에서는 공개 변수(NEXT_PUBLIC_API_BASE_URL) 사용
// 서버(SSR)에서는 컨테이너 내부 네트워크 접근을 위해 NEXT_INTERNAL_API_BASE_URL 우선 사용
export const API_BASE_URL = resolveApiBaseUrl()

export const API_ENDPOINTS: ApiEndpoints = {
  auth: {
    register: '/api/auth/register/',
    login: '/api/auth/login/',
    logout: '/api/auth/logout/',
    refresh: '/api/auth/token/refresh/',
    profile: '/api/auth/profile/',
    avatarRegenerate: '/api/auth/profile/avatar/regenerate/',
    passwordChange: '/api/auth/profile/password/',
    userInfo: '/api/auth/info/',
    profileDelete: '/api/auth/profile/delete/',
    settings: '/api/auth/profile/settings/',
    sessions: '/api/auth/profile/sessions/',
    google: '/api/auth/google/',
    userSummary: (username: string) => `/api/auth/users/${encodeURIComponent(username)}/summary/`,
  },
  posts: {
    list: '/api/posts/',
    create: '/api/posts/',
    detail: (id: number) => `/api/posts/${id}/`,
    update: (id: number) => `/api/posts/${id}/`,
    delete: (id: number) => `/api/posts/${id}/`,
    like: (id: number) => `/api/posts/${id}/like/`,
    bookmark: (id: number) => `/api/posts/${id}/bookmark/`,
    liked: '/api/posts/liked-posts/',
    bookmarked: '/api/posts/bookmarked-posts/',
    my: '/api/posts/my-posts/',
    platforms: '/api/posts/platforms/',
    models: '/api/posts/models/',
    modelsSuggest: '/api/posts/models/suggest/',
    platformModels: (platformId: number) => `/api/posts/platforms/${platformId}/models/`,
    categories: '/api/posts/categories/',
    tags: '/api/posts/tags/',
  },
  core: {
    search: '/api/core/search/',
    sortOptions: '/api/core/sort-options/',
    filterOptions: '/api/core/filter-options/',
    trending: {
      categoryRankings: '/api/core/trending/category-rankings/',
      refreshCache: '/api/core/trending/refresh-cache/',
      modelPosts: '/api/core/trending/model/',
      modelInfo: '/api/core/trending/model/',
    },
  },
  stats: {
    dashboard: '/api/stats/dashboard/',
    user: '/api/stats/user/',
  },
}

export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  NO_CONTENT: 204,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  UNPROCESSABLE_ENTITY: 422,
  INTERNAL_SERVER_ERROR: 500,
} as const

export const DEFAULT_PAGE_SIZE = 20
export const MAX_PAGE_SIZE = 100
import { resolveApiBaseUrl } from '@/lib/http'
