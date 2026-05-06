'use client'

import { useState, useEffect } from 'react'
import { useSession } from 'next-auth/react'
import { authApi, getAccessToken, clearTokens, setTokens } from '@/lib/api'
import {
  extractProfileUser,
  isUnauthorizedAuthError,
  resolveAuthErrorMessage,
  resolveBootstrapToken,
} from '@/lib/auth-orchestrator'
import { logger } from '@/lib/logger'
import type { UserData as BackendUserData } from '@/types/api'

interface LogoutOptions {
  skipBackendRequest?: boolean
}

export interface UseAuthReturn {
  user: BackendUserData | null
  token: string | null
  isLoading: boolean
  isAuthenticated: boolean
  authError: string | null
  login: (email: string, password: string) => Promise<{ success: boolean; message: string }>
  loginWithGoogle: (idToken: string) => Promise<{ success: boolean; message: string }>
  setAuthData: (token: string, user: BackendUserData) => void
  register: (
    email: string,
    password: string,
    passwordConfirm: string,
  ) => Promise<{ success: boolean; message: string }>
  logout: (options?: LogoutOptions) => Promise<void>
  refreshUser: () => Promise<void>
  regenerateAvatar: (regenerateUsername?: boolean) => Promise<{ success: boolean; message: string }>
}

export function useAuth(): UseAuthReturn {
  const [user, setUser] = useState<BackendUserData | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [initAttempted, setInitAttempted] = useState(false)
  const [authError, setAuthError] = useState<string | null>(null)

  // Google 로그인은 NextAuth가 먼저 들고 있어서, 앱 토큰 복원 순서를 맞추려고 같이 본다.
  const { data: session, status: sessionStatus } = useSession()

  const isAuthenticated = !!token && !!user

  // 초기화 시 토큰과 사용자 정보 확인
  useEffect(() => {
    // 초기화가 여러 번 돌면 프로필 호출과 토큰 정리가 서로 덮어쓸 수 있어서 한 번만 시도한다.
    if (initAttempted) return

    // NextAuth가 아직 판정 중일 때 로컬 토큰을 지우면 OAuth 복귀 직후 로그아웃처럼 보인다.
    if (sessionStatus === 'loading') {
      logger.debug('⏳ NextAuth 세션 로딩 중, initAuth 대기...')
      return
    }

    // 이미 토큰과 사용자 정보가 모두 있으면 스킵 (중복 호출 방지)
    if (token && user) {
      logger.debug('🔄 initAuth - 이미 인증 완료됨, 스킵')
      setIsLoading(false)
      setInitAttempted(true)
      return
    }

    // 세션 훅이 토큰을 곧 심어도, 여기서 프로필을 다시 읽어 최신 사용자 정보는 맞춰 둔다.
    if (sessionStatus === 'authenticated' && session?.backendToken && session?.backendUser) {
      logger.debug('🔄 NextAuth 세션 존재, 세션 동기화 우선 - 사용자 데이터 로드 진행')
      // initAuth는 계속 진행하되, 토큰은 이미 설정된 것으로 간주
    }

    // 새로고침 후 첫 렌더에서는 localStorage 토큰으로 헤더/보호 화면 깜빡임을 줄인다.
    const storedToken = getAccessToken()
    if (storedToken && !token) {
      logger.debug('🔄 localStorage 토큰 발견, 상태 복원 시도')
      setToken(storedToken)
      // 사용자 정보는 initAuth에서 별도로 로드
    }

    const initAuth = async () => {
      try {
        setAuthError(null)

        let tokenToUse = resolveBootstrapToken(session as any)
        if (tokenToUse && !getAccessToken() && session?.backendToken) {
          logger.debug('🔍 initAuth - NextAuth 세션에서 백엔드 토큰 사용')
          setToken(tokenToUse)
        }

        logger.debug('🔍 initAuth - 토큰 확인:', tokenToUse ? '토큰 존재' : '토큰 없음')

        if (tokenToUse) {
          // 이미 화면 상태가 채워졌으면 불필요한 프로필 호출을 줄인다.
          if (user) {
            logger.debug('🔄 initAuth - 토큰 설정됨, 사용자 정보는 이미 존재함')
          } else {
            logger.debug('🔍 initAuth - 프로필 정보 가져오기 시도')
            const response = await authApi.getProfile()
            logger.debug('🔍 initAuth - 프로필 응답:', response)
            const userData = extractProfileUser<BackendUserData>(response, session?.backendUser as any)
            if (userData) {
              setUser(userData)
              logger.debug('✅ initAuth - 사용자 정보 설정 완료:', userData)
            }
          }
        } else {
          logger.debug('❌ initAuth - 토큰이 없어서 인증되지 않음')
        }
      } catch (error: any) {
        logger.error('❌ 인증 초기화 오류:', error)
        setAuthError(error?.message || '인증 초기화 중 오류가 발생했습니다.')

        // 백엔드가 401을 주면 NextAuth 세션만 남겨도 다시 로그인된 것처럼 보여서 같이 정리한다.
        if (isUnauthorizedAuthError(error)) {
          logger.debug('🧹 401 오류로 인한 전체 인증 상태 초기화')
          try {
            const { signOut } = await import('next-auth/react')
            await signOut({ redirect: false })
            logger.debug('🧹 NextAuth 세션도 정리 완료')
          } catch (signOutError) {
            logger.error('NextAuth signOut 오류:', signOutError)
          }
        }

        clearTokens()
        setToken(null)
        setUser(null)
      } finally {
        setIsLoading(false)
        setInitAttempted(true)
      }
    }

    initAuth()
  }, [initAttempted, sessionStatus, session?.backendToken, session?.backendUser])

  // API 인터셉터에서 만료를 알리면 모든 화면의 인증 상태를 한 번에 비운다.
  useEffect(() => {
    const handleAuthExpired = () => {
      logger.debug('🔓 인증 만료 이벤트 수신, 상태 초기화')
      clearTokens()
      setToken(null)
      setUser(null)
      setAuthError('인증이 만료되었습니다. 다시 로그인해주세요.')
    }

    window.addEventListener('auth:expired', handleAuthExpired)
    return () => window.removeEventListener('auth:expired', handleAuthExpired)
  }, [])

  const login = async (
    email: string,
    password: string,
  ): Promise<{ success: boolean; message: string }> => {
    logger.debug('🔑 useAuth login 시작:', { email, password: '***' })

    try {
      const response = await authApi.login({ email, password })
      logger.debug('🔑 useAuth authApi.login 응답:', response)

      if (response.token && response.user) {
        logger.debug('🔑 토큰과 사용자 정보 존재:', {
          token: response.token,
          user: response.user,
        })

        // 토큰 저장은 API 레이어가 하고, 훅은 화면 상태만 바로 맞춘다.
        setToken(response.token)
        setUser(response.user)

        // 로그인 직후 라우팅에서 이전 상태를 읽지 않도록 아주 짧게 기다린다.
        await new Promise(resolve => setTimeout(resolve, 100))

        logger.debug('✅ useAuth login 성공 - 토큰과 사용자 정보 설정 완료')
        logger.debug('🔍 저장된 토큰 확인:', getAccessToken())
        logger.debug('🔍 설정된 사용자 정보:', response.user)
        logger.debug('🔍 인증 상태 확인:', !!response.token && !!response.user)

        return { success: true, message: response.message }
      }

      logger.debug('❌ useAuth 응답에 토큰이나 사용자 정보 없음')
      return { success: false, message: '로그인에 실패했습니다.' }
    } catch (error: any) {
      logger.error('❌ useAuth login 오류:', error)
      logger.error('❌ 오류 타입:', typeof error)
      logger.error('❌ 오류 메시지:', error?.message)
      logger.error('❌ 오류 응답:', error?.response)
      logger.error('❌ 오류 상태:', error?.response?.status)
      logger.error('❌ 오류 데이터:', error?.response?.data)

      const message = resolveAuthErrorMessage(error, '로그인 중 오류가 발생했습니다.')

      return { success: false, message }
    }
  }

  const loginWithGoogle = async (
    idToken: string,
  ): Promise<{ success: boolean; message: string }> => {
    try {
      const response = await authApi.loginWithGoogle(idToken)
      if (response.token && response.user) {
        setToken(response.token)
        setUser(response.user)
        await new Promise(resolve => setTimeout(resolve, 50))
        return { success: true, message: response.message }
      }
      return { success: false, message: 'Google 로그인에 실패했습니다.' }
    } catch (error: any) {
      const message = resolveAuthErrorMessage(error, 'Google 로그인 중 오류가 발생했습니다.')
      return { success: false, message }
    }
  }

  const register = async (
    email: string,
    password: string,
    passwordConfirm: string,
  ): Promise<{ success: boolean; message: string }> => {
    logger.debug('🔑 useAuth register 시작:', { email, password: '***' })

    try {
      const response = await authApi.register({
        email,
        password,
        password_confirm: passwordConfirm,
      })

      logger.debug('🔑 useAuth authApi.register 응답:', response)

      if (response.token && response.user) {
        // 토큰은 authApi.register에서 자동으로 저장됨
        setToken(response.token)
        setUser(response.user)

        logger.debug('✅ useAuth register 성공 - 반환')
        return { success: true, message: response.message }
      }

      logger.debug('❌ useAuth register 응답에 토큰이나 사용자 정보 없음')
      return { success: false, message: '회원가입에 실패했습니다.' }
    } catch (error: any) {
      logger.error('❌ useAuth register 오류:', error)
      logger.error('❌ 오류 타입:', typeof error)
      logger.error('❌ 오류 메시지:', error?.message)
      logger.error('❌ 오류 응답:', error?.response)
      logger.error('❌ 오류 상태:', error?.response?.status)
      logger.error('❌ 오류 데이터:', error?.response?.data)

      const message = resolveAuthErrorMessage(error, '회원가입 중 오류가 발생했습니다.')

      return { success: false, message }
    }
  }

  const logout = async (options?: LogoutOptions): Promise<void> => {
    if (!options?.skipBackendRequest) {
      try {
        // 백엔드 로그아웃 시도 (실패해도 계속 진행)
        await authApi.logout()
      } catch (error) {
        logger.error('백엔드 로그아웃 요청 오류 (무시하고 계속 진행):', error)
      }
    }

    try {
      // NextAuth 세션 정리
      const { signOut } = await import('next-auth/react')
      await signOut({ redirect: false })
      logger.debug('🧹 NextAuth 세션 정리 완료')
    } catch (error) {
      logger.error('NextAuth 로그아웃 오류 (무시하고 계속 진행):', error)
    }

    // 항상 로컬 상태 클리어
    clearTokens()
    setToken(null)
    setUser(null)

    logger.debug('✅ 로그아웃 완료 - 모든 인증 상태 초기화됨')
  }

  const refreshUser = async (): Promise<void> => {
    try {
      if (!token) return

      const response = await authApi.getProfile()
      const userData = extractProfileUser<BackendUserData>(response)
      if (userData) {
        setUser(userData)
      }
    } catch (error) {
      logger.error('사용자 정보 새로고침 오류:', error)
    }
  }

  const regenerateAvatar = async (
    regenerateUsername = false,
  ): Promise<{ success: boolean; message: string }> => {
    try {
      if (!token) {
        return { success: false, message: '로그인이 필요합니다.' }
      }

      const response = await authApi.regenerateAvatar(regenerateUsername)
      if (response?.user) {
        setUser(response.user)
      }

      return {
        success: true,
        message: response?.message || '아바타 재생성에 성공했습니다.',
      }
    } catch (error: any) {
      const message =
        error?.response?.data?.message || error?.message || '아바타 재생성 중 오류가 발생했습니다.'
      return { success: false, message }
    }
  }

  /**
   * 백엔드에서 받은 토큰과 사용자 정보를 직접 설정
   * Google 로그인 등에서 사용
   */
  const setAuthData = (backendToken: string, userData: BackendUserData): void => {
    // 이미 같은 토큰과 사용자가 설정되어 있으면 중복 설정 방지
    if (token === backendToken && user?.email === userData?.email) {
      logger.debug('🔄 setAuthData - 이미 같은 데이터가 설정되어 있음, 스킵')
      return
    }

    logger.debug('🔑 setAuthData 호출:', {
      token: backendToken?.substring(0, 10) + '...',
      user: userData?.email,
    })

    // 토큰을 localStorage에 저장
    setTokens(backendToken)

    // 상태 업데이트
    setToken(backendToken)
    setUser(userData)

    // 저장 확인
    setTimeout(() => {
      const storedToken = getAccessToken()
      logger.debug('✅ 인증 데이터 설정 완료 - 저장 확인:', {
        stored: storedToken?.substring(0, 10) + '...',
        state: backendToken?.substring(0, 10) + '...',
        match: storedToken === backendToken,
      })
    }, 50)
  }

  return {
    user,
    token,
    isLoading,
    isAuthenticated,
    authError,
    login,
    loginWithGoogle,
    setAuthData,
    register,
    logout,
    refreshUser,
    regenerateAvatar,
  }
}
