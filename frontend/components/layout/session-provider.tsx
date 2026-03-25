'use client'

import { SessionProvider } from 'next-auth/react'
import type { Session } from 'next-auth'

interface NextAuthProviderProps {
  children: React.ReactNode
  session?: Session | null
}

/**
 * next-auth SessionProvider wrapper
 * 클라이언트 컴포넌트로 분리하여 서버 컴포넌트에서 사용 가능
 */
export default function NextAuthProvider({ children, session }: NextAuthProviderProps) {
  return <SessionProvider session={session}>{children}</SessionProvider>
}
