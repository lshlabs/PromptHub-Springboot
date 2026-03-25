/**
 * GoBackButton 컴포넌트
 *
 * 뒤로가기 기능을 제공하는 공용 컴포넌트
 * 현재 페이지 경로에 따라 적절한 목록 페이지로 돌아감
 */

'use client'

import { ArrowLeft } from 'lucide-react'
import { useRouter } from 'next/navigation'
import CustomButton from '@/components/common/custom-button'

interface GoBackButtonProps {
  fallbackPath?: string // 기본 돌아갈 경로 (예: /community)
  className?: string
  fromPage?: string // 이전 페이지 정보 (예: 'community', 'trending', 'bookmarks', 'profile')
  label?: string
}

export function GoBackButton({
  fallbackPath = '/community',
  className = '',
  fromPage,
  label = '목록으로',
}: GoBackButtonProps) {
  const router = useRouter()

  const handleGoBack = () => {
    const currentPath = window.location.pathname

    // fromPage prop이 제공된 경우 우선 사용
    if (fromPage) {
      switch (fromPage) {
        case 'community':
          router.push('/community')
          return
        case 'trending':
          router.push('/trending')
          return
        case 'bookmarks':
          router.push('/bookmarks')
          return
        case 'profile':
          router.push('/profile')
          return
        default:
          break
      }
    }

    // URL 파라미터에서 이전 페이지 정보 확인
    const urlParams = new URLSearchParams(window.location.search)
    const fromParam = urlParams.get('from')

    if (fromParam) {
      switch (fromParam) {
        case 'community':
          router.push('/community')
          return
        case 'trending':
          router.push('/trending')
          return
        case 'bookmarks':
          router.push('/bookmarks')
          return
        case 'profile':
          router.push('/profile')
          return
        default:
          break
      }
    }

    // 세션 스토리지에서 이전 페이지 정보 확인
    const storedFromPage = sessionStorage.getItem('fromPage')
    if (storedFromPage) {
      switch (storedFromPage) {
        case 'community':
          router.push('/community')
          return
        case 'trending':
          router.push('/trending')
          return
        case 'bookmarks':
          router.push('/bookmarks')
          return
        case 'profile':
          router.push('/profile')
          return
        default:
          break
      }
    }

    // post-detail 페이지에서의 처리 (document.referrer 사용)
    if (currentPath.startsWith('/post/')) {
      const previousPage = document.referrer
      const currentDomain = window.location.origin

      if (previousPage.startsWith(currentDomain)) {
        const previousPath = new URL(previousPage).pathname

        if (previousPath === '/community') {
          router.push('/community')
          return
        } else if (previousPath === '/trending') {
          router.push('/trending')
          return
        } else if (previousPath === '/bookmarks') {
          router.push('/bookmarks')
          return
        } else if (previousPath === '/profile') {
          router.push('/profile')
          return
        }
      }
    }

    // edit-post 페이지에서의 처리
    if (currentPath.startsWith('/edit-post/')) {
      const previousPage = document.referrer
      const currentDomain = window.location.origin

      if (previousPage.startsWith(currentDomain)) {
        const previousPath = new URL(previousPage).pathname

        // 이전 페이지가 post-detail인 경우, post-detail의 이전 페이지를 확인
        if (previousPath.startsWith('/post/')) {
          const postDetailPreviousPage = document.referrer

          if (postDetailPreviousPage.startsWith(currentDomain)) {
            const postDetailPreviousPath = new URL(postDetailPreviousPage).pathname

            if (postDetailPreviousPath === '/community') {
              router.push('/community')
              return
            } else if (postDetailPreviousPath === '/trending') {
              router.push('/trending')
              return
            } else if (postDetailPreviousPath === '/bookmarks') {
              router.push('/bookmarks')
              return
            } else if (postDetailPreviousPath === '/profile') {
              router.push('/profile')
              return
            }
          }
        }

        // 이전 페이지가 직접 목록 페이지인 경우
        if (previousPath === '/community') {
          router.push('/community')
          return
        } else if (previousPath === '/trending') {
          router.push('/trending')
          return
        } else if (previousPath === '/bookmarks') {
          router.push('/bookmarks')
          return
        } else if (previousPath === '/profile') {
          router.push('/profile')
          return
        }
      }
    }

    // 모든 방법이 실패한 경우 기본값
    router.push(fallbackPath)
  }

  return (
    <CustomButton
      color="flat"
      border="none"
      shape="rounded"
      size="xs"
      icon={<ArrowLeft className="h-4 w-4" />}
      onClick={handleGoBack}
      className={`text-gray-600 hover:text-blue-600 ${className}`}>
      {label}
    </CustomButton>
  )
}
