import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { MessageCircle } from 'lucide-react'
import { PostCard } from '@/components/common/post-card'
import { useState, useEffect } from 'react'
import { trendingApi } from '@/lib/api'
import type { PostCard as ApiPostCard, TrendingModelInfo } from '@/types/api'
import { useDelayedLoading } from '@/hooks/use-delayed-loading'
import { getDomainErrorMessage } from '@/lib/utils'

interface PostsListProps {
  selectedModel: string | null
  setSelectedModel: (model: string | null) => void
  hideEmptyPrompt?: boolean
}

export default function PostsList({
  selectedModel,
  setSelectedModel,
  hideEmptyPrompt = false,
}: PostsListProps) {
  const [posts, setPosts] = useState<ApiPostCard[]>([])
  const [modelInfo, setModelInfo] = useState<TrendingModelInfo | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [hasFetchedForSelection, setHasFetchedForSelection] = useState(false)
  const showLoading = useDelayedLoading(loading, { delayMs: 180, minVisibleMs: 320 })

  const renderTrendingPostSkeletons = (count = 2) => (
    <div className="space-y-3 py-2" aria-hidden="true">
      {Array.from({ length: count }).map((_, idx) => (
        <div key={idx} className="rounded-xl border border-gray-100 bg-white p-4">
          <div className="space-y-3">
            <div className="h-5 w-1/2 animate-pulse rounded bg-gray-200" />
            <div className="h-4 w-2/3 animate-pulse rounded bg-gray-100" />
            <div className="flex gap-2">
              <div className="h-5 w-16 animate-pulse rounded bg-blue-100" />
              <div className="h-5 w-20 animate-pulse rounded bg-gray-100" />
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="h-7 w-7 animate-pulse rounded-full bg-gray-200" />
                <div className="h-4 w-20 animate-pulse rounded bg-gray-200" />
              </div>
              <div className="h-4 w-16 animate-pulse rounded bg-gray-100" />
            </div>
          </div>
        </div>
      ))}
    </div>
  )

  // 선택된 모델이 변경될 때마다 관련 게시글 조회
  useEffect(() => {
    if (!selectedModel) {
      setPosts([])
      setModelInfo(null)
      setError(null)
      setHasFetchedForSelection(false)
      return
    }

    const fetchModelPosts = async () => {
      try {
        setLoading(true)
        setError(null)

        const response = await trendingApi.getTrendingModelPosts(selectedModel, {
          page: 1,
          page_size: 20,
          sort: 'latest',
        })

        setPosts(response.results)
        setModelInfo(response.trending_model)
      } catch (err) {
        setError(
          getDomainErrorMessage(
            err,
            '선택한 트렌딩 모델의 리뷰를 불러오지 못했습니다. 모델을 다시 선택하거나 잠시 후 재시도해주세요.',
            {
              notFound: '해당 모델과 연결된 리뷰가 아직 없습니다. 다른 모델을 선택해보세요.',
            },
          ),
        )
        setPosts([])
        setModelInfo(null)
      } finally {
        setLoading(false)
        setHasFetchedForSelection(true)
      }
    }

    fetchModelPosts()
  }, [selectedModel])

  return (
    <>
      {selectedModel && (
        <Card className="border-0 bg-white/70 backdrop-blur-sm">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-xl font-semibold">{selectedModel} 관련 게시물</h3>
                {modelInfo && (
                  <div className="mt-1 flex items-center gap-2 text-sm text-gray-500">
                    <span>{modelInfo.provider}</span>
                    <span>•</span>
                    <span>순위 #{modelInfo.rank}</span>
                    <span>•</span>
                    <span>{modelInfo.related_posts_count}개 게시글</span>
                  </div>
                )}
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setSelectedModel(null)
                  if (typeof window !== 'undefined') {
                    window.scrollTo({ top: 0, behavior: 'smooth' })
                  }
                }}>
                선택 해제
              </Button>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {!hasFetchedForSelection && !showLoading ? (
              renderTrendingPostSkeletons(2)
            ) : null}

            {showLoading && posts.length > 0 ? (
              <div className="rounded-md border border-gray-100 bg-white/80 px-4 py-2 text-sm text-gray-500">
                게시글 목록을 업데이트하는 중...
              </div>
            ) : null}

            {!hasFetchedForSelection ? null : showLoading && posts.length === 0 ? (
              renderTrendingPostSkeletons(2)
            ) : error ? (
              <div className="py-8 text-center">
                <div className="mb-2 text-red-600">{error}</div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    if (selectedModel) {
                      // 재시도 로직 - useEffect가 다시 실행되도록 강제
                      setError(null)
                      setLoading(true)
                    }
                  }}>
                  다시 시도
                </Button>
              </div>
            ) : posts.length > 0 ? (
              posts.map(item => (
                <PostCard
                  key={item.id}
                  data={item}
                  onClick={() => {
                    window.location.href = `/post/${item.id}?from=trending`
                  }}
                />
              ))
            ) : (
              <div className="py-8 text-center">
                <div className="mb-2 text-base font-medium text-gray-700">
                  아직 등록된 리뷰가 없어요
                </div>
                <div className="text-sm text-gray-400">
                  {modelInfo?.related_model
                    ? `${selectedModel} 관련 리뷰가 아직 올라오지 않았습니다. 첫 리뷰를 작성해보세요.`
                    : `${selectedModel} 모델은 아직 PromptHub 모델 목록과 연결되지 않았습니다. 잠시 후 다시 확인해주세요.`}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {!selectedModel && !hideEmptyPrompt && (
        <Card className="border-0 bg-white/70 backdrop-blur-sm">
          <CardContent className="py-12 text-center">
            <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-gradient-to-r from-red-100 to-orange-100">
              <MessageCircle className="h-8 w-8 text-red-500" />
            </div>
            <h3 className="mb-2 text-lg font-semibold text-gray-700">모델을 선택해주세요</h3>
            <p className="text-gray-500">
              위의 카테고리에서 관심있는 AI 모델을 클릭하면 관련 게시물을 확인할 수 있습니다.
            </p>
          </CardContent>
        </Card>
      )}
    </>
  )
}
