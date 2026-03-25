'use client'

import { useRef, useState } from 'react'
import { TrendingHero, PostsList, CategoryRankings } from '@/components/trending'

export default function TrendingPage() {
  const [selectedModel, setSelectedModel] = useState<string | null>(null)
  const [isRankingsLoading, setIsRankingsLoading] = useState(true)
  const postsRef = useRef<HTMLDivElement | null>(null)

  const smoothScrollToPosts = () => {
    if (typeof window === 'undefined' || !postsRef.current) return
    const OFFSET = 80
    const elementTop = postsRef.current.getBoundingClientRect().top + window.pageYOffset
    const targetY = Math.max(elementTop - OFFSET, 0)
    window.scrollTo({ top: targetY, behavior: 'smooth' })
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        <div className="mx-auto max-w-7xl space-y-8">
          <TrendingHero />

          {/* CategoryRankings 컴포넌트는 이제 자체적으로 API 호출을 처리 */}
          <CategoryRankings
            selectedModel={selectedModel}
            setSelectedModel={setSelectedModel}
            onSelectModel={smoothScrollToPosts}
            onLoadingChange={setIsRankingsLoading}
          />

          <div ref={postsRef} />
          <PostsList
            selectedModel={selectedModel}
            setSelectedModel={setSelectedModel}
            hideEmptyPrompt={isRankingsLoading}
          />
        </div>
      </div>
    </div>
  )
}
