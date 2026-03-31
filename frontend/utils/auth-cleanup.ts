/**
 * 인증 관련 모든 데이터를 완전히 삭제하는 유틸리티
 */

import { signOut } from 'next-auth/react'

/**
 * NextAuth 세션과 모든 로컬 스토리지 데이터를 완전히 삭제
 */
export async function clearAllAuthData(): Promise<{ ok: true } | { ok: false; message: string }> {
  try {
    // 1. NextAuth 세션 삭제
    await signOut({
      redirect: false,
      callbackUrl: '/',
    })

    // 2. 로컬 스토리지 삭제
    if (typeof window !== 'undefined') {
      // 백엔드 토큰 관련
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      localStorage.removeItem('token')

      // NextAuth 관련
      localStorage.removeItem('nextauth.message')
      localStorage.removeItem('next-auth.session-token')
      localStorage.removeItem('next-auth.callback-url')
      localStorage.removeItem('next-auth.csrf-token')

      // 세션 스토리지도 삭제
      sessionStorage.clear()
    }

    // 3. 쿠키 삭제 (NextAuth 관련)
    if (typeof document !== 'undefined') {
      const cookies = [
        'next-auth.session-token',
        'next-auth.callback-url',
        'next-auth.csrf-token',
        '__Secure-next-auth.session-token',
        '__Host-next-auth.csrf-token',
      ]

      cookies.forEach(cookieName => {
        document.cookie = `${cookieName}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`
        document.cookie = `${cookieName}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=localhost`
        document.cookie = `${cookieName}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=.localhost`
      })
    }

    // 4. 페이지 새로고침으로 상태 완전 초기화
    if (typeof window !== 'undefined') {
      window.location.href = '/'
    }

    return { ok: true }
  } catch (error) {
    const message =
      error instanceof Error
        ? error.message
        : '인증 데이터 정리 중 알 수 없는 오류가 발생했습니다.'
    return { ok: false, message }
  }
}

/**
 * 개발자 도구 콘솔에서 사용할 수 있는 전역 함수
 */
if (typeof window !== 'undefined') {
  ;(window as any).clearAllAuthData = clearAllAuthData
}
