import { getAccessToken, setTokens } from '@/lib/api'

type SessionLike = {
  backendToken?: string | null
  backendUser?: unknown
} | null

export const resolveBootstrapToken = (session: SessionLike): string | null => {
  const localToken = getAccessToken()
  if (localToken) return localToken

  const sessionToken = session?.backendToken
  if (sessionToken) {
    // 새로고침 직후에는 NextAuth 세션만 먼저 살아 있어서 로컬 토큰을 다시 심어 둔다.
    setTokens(sessionToken)
    return sessionToken
  }

  return null
}

export const extractProfileUser = <T>(profileEnvelope: unknown, fallback?: T): T | null => {
  // 이전 화면은 user 래퍼가 없는 응답도 받아서, 마이그레이션 중 두 형태를 같이 허용한다.
  if (
    profileEnvelope &&
    typeof profileEnvelope === 'object' &&
    'user' in profileEnvelope &&
    (profileEnvelope as { user?: T }).user
  ) {
    return (profileEnvelope as { user: T }).user
  }
  if (profileEnvelope) return profileEnvelope as T
  return fallback ?? null
}

export const isUnauthorizedAuthError = (error: any): boolean => {
  return error?.response?.status === 401 || error?.status === 401
}

export const resolveAuthErrorMessage = (error: any, fallback: string): string => {
  if (error?.response?.data?.message) return error.response.data.message
  if (error?.response?.data?.detail) return error.response.data.detail
  if (error?.message) return error.message
  return fallback
}
