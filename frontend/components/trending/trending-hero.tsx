import { TrendingUp } from 'lucide-react'

export default function TrendingHero() {
  return (
    <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
      <div className="h-1 bg-gradient-to-r from-orange-500 via-red-500 to-pink-500"></div>
      <div className="p-6 text-center">
        <div className="mb-3 flex items-center justify-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-orange-500 to-pink-500 sm:h-10 sm:w-10">
            <TrendingUp className="h-4 w-4 text-white sm:h-5 sm:w-5" />
          </div>
          <h2 className="font-bold text-gray-900">AI 모델 성능 랭킹</h2>
        </div>
        <p className="text-gray-600">
          카테고리별 최고 성능 AI 모델과 API 제공업체를 <br className="block sm:hidden" />
          한눈에 비교해보세요
        </p>
        <p className="mb-6 text-orange-600">
          최신 모델을 포함하지 않은 <br className="block sm:hidden" />
          벤치마크가 있을 수 있습니다
        </p>

        {/* Data Source */}
        <div className="text-sm text-gray-500">
          데이터 출처:{' '}
          <a
            href="https://www.vellum.ai/llm-leaderboard?utm_source=direct&utm_medium=none"
            target="_blank"
            rel="noopener noreferrer"
            className="text-orange-600 hover:underline">
            vellum.ai
          </a>{' '}
          | 최종 업데이트: 2026년 2월
        </div>
      </div>
    </div>
  )
}
