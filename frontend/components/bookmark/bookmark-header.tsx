import { Bookmark } from 'lucide-react'

export function BookmarkHeader() {
  return (
    <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
      <div className="h-1 bg-gradient-to-r from-green-500 via-emerald-500 to-teal-500"></div>
      <div className="p-6 text-center">
        <div className="mb-3 flex items-center justify-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-green-500 to-emerald-600 sm:h-10 sm:w-10">
            <Bookmark className="h-4 w-4 text-white sm:h-5 sm:w-5" />
          </div>
          <h1 className="text-gray-900">내 북마크</h1>
        </div>
        <p className="text-sm text-gray-600">북마크한 리뷰를 관리하고 다시 찾아보세요</p>
      </div>
    </div>
  )
}
