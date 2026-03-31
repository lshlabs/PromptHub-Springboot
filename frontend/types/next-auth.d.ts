import 'next-auth'
import 'next-auth/jwt'

/**
 * next-auth 타입 확장
 *
 * 주의: 여기서 'next-auth/jwt'는 Spring JWT가 아닌
 * next-auth 라이브러리 내부의 JWT 토큰 타입입니다.
 * 앱 백엔드는 Spring 토큰 인증을 사용합니다.
 */

declare module 'next-auth' {
  interface Session {
    googleProfile?: any // Google 프로필 정보
    idToken?: string // Google ID 토큰 (백엔드 전송용)
    backendToken?: string // 백엔드에서 발급된 토큰
    backendUser?: any // 백엔드 사용자 정보
  }
}

declare module 'next-auth/jwt' {
  interface JWT {
    googleProfile?: any // Google 프로필 정보
    idToken?: string // Google ID 토큰 (백엔드 전송용)
    backendToken?: string // 백엔드에서 발급된 토큰
    backendUser?: any // 백엔드 사용자 정보
    authError?: string
  }
}
