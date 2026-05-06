'use client'

import { useEffect, useState } from 'react'
import { useSession, signOut } from 'next-auth/react'
import { useAuthContext } from '@/components/layout/auth-provider'
import { logger } from '@/lib/logger'
import { HttpRequestError, fetchJson, tokenAuthHeaders } from '@/lib/http'
import type { UserProfileEnvelope } from '@/types/api'

/**
 * NextAuth 세션과 로컬 인증 상태를 동기화하는 훅
 *
 * 사용법:
 * - Google 로그인 후 리다이렉트된 페이지에서 호출
 * - 토큰 유효성을 검증한 후에만 동기화
 */
export function useSessionSync() {
  const { data: session, status } = useSession()
  const { setAuthData, isAuthenticated, user } = useAuthContext()
  const [syncAttempted, setSyncAttempted] = useState(false)
  const [syncError, setSyncError] = useState<string | null>(null)

  useEffect(() => {
    // NextAuth가 아직 판정 중일 때 건드리면 로컬 인증 상태가 먼저 비어 보일 수 있다.
    if (status === 'loading') return

    // 개발 중 Hot Refresh가 걸려도 같은 세션을 계속 재검증하지 않도록 탭 단위로만 표시한다.
    const userEmail = session?.user?.email || 'anonymous'
    const syncKey = `session_sync_${userEmail}`
    const alreadySynced = sessionStorage.getItem(syncKey)

    if (syncAttempted || alreadySynced) {
      logger.debug('🔄 세션 동기화 이미 완료됨, 스킵')
      return
    }

    // Google OAuth 재로그인 때는 이전 이메일 기준 완료 표시가 남아 있을 수 있어 한 번 비운다.
    if (status === 'unauthenticated' && alreadySynced && userEmail !== 'anonymous') {
      logger.debug('🔄 Google OAuth 재로그인 감지, 동기화 상태 초기화')
      sessionStorage.removeItem(syncKey)
      setSyncAttempted(false)
      setSyncError(null)
      return
    }

    // NextAuth 세션과 앱 자체 토큰이 어긋날 때만 백엔드 프로필로 다시 맞춘다.
    if (
      status === 'authenticated' &&
      session?.backendToken &&
      session?.backendUser &&
      (!isAuthenticated || user?.email !== session.backendUser?.email)
    ) {
      const syncSession = async () => {
        try {
          logger.debug('🔄 세션 동기화 시작:', session.backendUser?.email)
          setSyncError(null)

          const backendToken = session.backendToken
          if (!backendToken) {
            throw new Error('백엔드 토큰 또는 사용자 데이터 누락')
          }

          try {
            const profileData = await fetchJson<UserProfileEnvelope>('/api/auth/profile', {
              headers: tokenAuthHeaders(backendToken),
            })
            const latestUserData = profileData.user || session.backendUser

            logger.debug('✅ 백엔드 토큰 유효성 확인 완료, 최신 프로필로 동기화')

            if (latestUserData) {
              setAuthData(backendToken, latestUserData)

              // 이메일별로 표시해야 다른 계정으로 다시 로그인할 때 동기화를 건너뛰지 않는다.
              if (latestUserData.email) {
                const syncKey = `session_sync_${latestUserData.email}`
                sessionStorage.setItem(syncKey, 'true')
              }

              logger.debug('🎉 세션 동기화 완료 - 사용자:', latestUserData.email)
            } else {
              throw new Error('백엔드 토큰 또는 사용자 데이터 누락')
            }
          } catch (error) {
            if (error instanceof HttpRequestError) {
              logger.debug(`❌ 백엔드 토큰이 무효함 (상태: ${error.status})`, error.payload)
              throw new Error('백엔드 토큰이 무효합니다.')
            }
            throw error
          }
        } catch (error) {
          logger.error('❌ 세션 동기화 중 오류:', error)
          setSyncError(error instanceof Error ? error.message : '알 수 없는 오류')

          // 백엔드 토큰이 거절되면 NextAuth만 남아도 로그인처럼 보여서 같이 정리한다.
          try {
            await signOut({ redirect: false })
          } catch (signOutError) {
            logger.error('NextAuth 세션 정리 실패:', signOutError)
          }
        } finally {
          setSyncAttempted(true)
        }
      }

      syncSession()
    }
  }, [
    status,
    session?.backendToken,
    session?.backendUser?.email,
    syncAttempted,
    isAuthenticated,
    user?.email,
  ])

  return { syncError, syncAttempted }
}
