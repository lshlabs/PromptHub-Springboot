import { getAccessToken, setTokens } from '@/lib/api'

type SessionLike = {
  djangoToken?: string | null
  djangoUser?: unknown
} | null

export const resolveBootstrapToken = (session: SessionLike): string | null => {
  const localToken = getAccessToken()
  if (localToken) return localToken

  const sessionToken = session?.djangoToken
  if (sessionToken) {
    setTokens(sessionToken)
    return sessionToken
  }

  return null
}

export const extractProfileUser = <T>(profileEnvelope: unknown, fallback?: T): T | null => {
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
