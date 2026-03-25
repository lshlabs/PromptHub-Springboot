import type { Metadata } from 'next'
import { AuthProvider } from '@/components/layout/auth-provider'
import NextAuthProvider from '@/components/layout/session-provider'
import Header from '@/components/layout/header'
import Footer from '@/components/layout/footer'
import { Toaster } from '@/components/ui/toaster'

import './globals.css'

// 개발 환경에서 전역 함수 추가
if (typeof window !== 'undefined' && process.env.NODE_ENV === 'development') {
  import('@/utils/auth-cleanup')
}

export const metadata: Metadata = {
  title: 'PromptHub - AI 프롬프트 리뷰 플랫폼',
  description: 'AI 프롬프트를 공유하고 최적화할 수 있는 커뮤니티 플랫폼입니다.',
  keywords: 'AI, 프롬프트, ChatGPT, 최적화, 커뮤니티',
  authors: [{ name: 'hu2chaso' }],
  creator: 'hu2chaso',
  openGraph: {
    title: 'PromptHub',
    description: 'AI 프롬프트 리뷰 플랫폼',
    type: 'website',
  },
  icons: {
    icon: '/icon.svg',
    shortcut: '/icon.svg',
  },
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <body suppressHydrationWarning>
        <NextAuthProvider>
          <AuthProvider>
            <Header />
            <main className="min-h-screen">{children}</main>
            <Footer />
            <Toaster />
          </AuthProvider>
        </NextAuthProvider>
      </body>
    </html>
  )
}
