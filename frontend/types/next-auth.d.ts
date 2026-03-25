import 'next-auth'
import 'next-auth/jwt'

/**
 * next-auth 타입 확장
 *
 * 주의: 여기서 'next-auth/jwt'는 Django JWT가 아닌
 * next-auth 라이브러리 내부의 JWT 토큰 타입입니다.
 * Django는 DRF Token Authentication을 사용합니다.
 */

declare module 'next-auth' {
  interface Session {
    googleProfile?: any // Google 프로필 정보
    idToken?: string // Google ID 토큰 (Django 백엔드 전송용)
    djangoToken?: string // Django에서 발급된 토큰
    djangoUser?: any // Django 사용자 정보
  }
}

declare module 'next-auth/jwt' {
  interface JWT {
    googleProfile?: any // Google 프로필 정보
    idToken?: string // Google ID 토큰 (Django 백엔드 전송용)
    djangoToken?: string // Django에서 발급된 토큰
    djangoUser?: any // Django 사용자 정보
  }
}
