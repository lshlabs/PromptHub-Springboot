import NextAuth from 'next-auth'
import GoogleProvider from 'next-auth/providers/google'
import { HttpRequestError, postJson } from '@/lib/http'

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

          try {
            const djangoData = await postJson<any>('/api/auth/google/', {
              id_token: account.id_token,
            })
            token.djangoToken = djangoData.token
            token.djangoUser = djangoData.user
            token.authError = undefined
          } catch (error) {
            if (error instanceof HttpRequestError) {
              token.authError =
                error.status === 401 || error.status === 403
                  ? 'DJANGO_AUTH_UNAUTHORIZED'
                  : error.status >= 500
                    ? 'DJANGO_AUTH_SERVER_ERROR'
                    : 'DJANGO_AUTH_FAILED'
              return token
            }
            token.authError = 'DJANGO_AUTH_UNKNOWN_ERROR'
          }
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
      // Django 토큰과 사용자 정보 포함
      if (token.djangoToken && token.djangoUser) {
        session.djangoToken = token.djangoToken
        session.djangoUser = token.djangoUser
      }
      return session
    },
    async signIn({ user, account, profile }) {
      return true
    },
  },
})

export { handler as GET, handler as POST }
