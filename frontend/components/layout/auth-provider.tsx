/**
 * 인증 컨텍스트 프로바이더
 *
 * 전역 인증 상태를 관리하고 하위 컴포넌트들에게 제공합니다.
 */
'use client'

import { createContext, useContext, type ReactNode } from 'react'
import { useAuth, type UseAuthReturn } from '@/hooks/use-auth'

type AuthContextType = UseAuthReturn

const AuthContext = createContext<AuthContextType | undefined>(undefined)

interface AuthProviderProps {
  children: ReactNode
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
  const auth = useAuth()

  return <AuthContext.Provider value={auth}>{children}</AuthContext.Provider>
}

export const useAuthContext = () => {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuthContext는 AuthProvider 내에서 사용되어야 합니다.')
  }
  return context
}
