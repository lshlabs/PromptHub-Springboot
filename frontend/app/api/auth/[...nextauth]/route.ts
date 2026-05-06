import NextAuth from 'next-auth'
import GoogleProvider from 'next-auth/providers/google'
import { HttpRequestError, postJson } from '@/lib/http'

type BackendGoogleAuthResponse = {
  token?: string
  user?: unknown
}

const getGoogleCredentials = () => {
  const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID
  const clientSecret = process.env.GOOGLE_CLIENT_SECRET

  if (!clientId || clientId.length === 0) {
    throw new Error('Missing NEXT_PUBLIC_GOOGLE_CLIENT_ID')
  }

  if (!clientSecret || clientSecret.length === 0) {
    throw new Error('Missing GOOGLE_CLIENT_SECRET')
  }

  return { clientId, clientSecret }
}

const resolveBackendAuthErrorCode = (error: unknown): string => {
  if (error instanceof HttpRequestError) {
    // NextAuth는 문자열 에러 코드만 안정적으로 넘겨서, 백엔드 상태값을 화면용 코드로 줄여 둔다.
    if (error.status === 400) return 'BACKEND_AUTH_BAD_REQUEST'
    if (error.status === 401 || error.status === 403) return 'BACKEND_AUTH_UNAUTHORIZED'
    if (error.status >= 500) return 'BACKEND_AUTH_SERVER_ERROR'
    return 'BACKEND_AUTH_FAILED'
  }
  return 'BACKEND_AUTH_UNKNOWN_ERROR'
}

const buildBackendAuthError = (error: unknown): Error => {
  const code = resolveBackendAuthErrorCode(error)
  const wrapped = new Error(code)
  ;(wrapped as Error & { cause?: unknown }).cause = error
  return wrapped
}

const handler = NextAuth({
  providers: [
    GoogleProvider({
      clientId: getGoogleCredentials().clientId,
      clientSecret: getGoogleCredentials().clientSecret,
    }),
  ],
  callbacks: {
    async jwt({ token, account, profile, trigger }) {
      if (trigger === 'signIn' || (account && profile)) {
        if (account && profile) {
          token.googleProfile = profile
          token.accessToken = account.access_token
          token.idToken = account.id_token

          const backendToken = (account as typeof account & { backendToken?: string }).backendToken
          const backendUser = (account as typeof account & { backendUser?: unknown }).backendUser
          if (!backendToken || !backendUser) {
            // signIn에서 백엔드 교환이 끝난 뒤에만 세션을 만들 수 있게 한 번 더 막는다.
            throw new Error('BACKEND_AUTH_TOKEN_MISSING')
          }

          token.backendToken = backendToken
          token.backendUser = backendUser
          token.authError = undefined
        }
      }
      return token
    },
    async session({ session, token }) {
      if (token.googleProfile) {
        session.googleProfile = token.googleProfile
      }
      if (token.idToken) {
        session.idToken = token.idToken
      }
      // 백엔드 토큰과 사용자 정보 포함
      if (token.backendToken && token.backendUser) {
        session.backendToken = token.backendToken
        session.backendUser = token.backendUser
      }
      return session
    },
    async signIn({ user, account, profile }) {
      if (account?.provider !== 'google') {
        return true
      }

      if (!account.id_token || typeof account.id_token !== 'string') {
        throw new Error('BACKEND_AUTH_MISSING_ID_TOKEN')
      }

      try {
        const backendAuth = await postJson<BackendGoogleAuthResponse>('/api/auth/google', {
          id_token: account.id_token,
        })

        if (!backendAuth?.token || !backendAuth?.user) {
          throw new Error('BACKEND_AUTH_INVALID_PAYLOAD')
        }

        ;(account as typeof account & { backendToken?: string }).backendToken = backendAuth.token
        ;(account as typeof account & { backendUser?: unknown }).backendUser = backendAuth.user
      } catch (error) {
        // 에러를 흡수하지 않고 sign-in 실패로 강제하여
        // "Google 통과 후 홈에서 비로그인" 엇갈림을 방지한다.
        throw buildBackendAuthError(error)
      }

      return true
    },
  },
})

export { handler as GET, handler as POST }
