/**
 * ExtensionHeader 컴포넌트
 *
 * 확장프로그램 페이지의 헤더 섹션을 표시합니다.
 * 통계 카드들과 확장프로그램 소개 정보를 포함합니다.
 */

'use client'

import { Users, Download, Star, Zap, Chrome } from 'lucide-react'

export function ExtensionHeader() {
  return (
    <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
      <div className="h-1 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500"></div>
      <div className="p-6 text-center">
        <div className="mb-3 flex items-center justify-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-blue-500 to-purple-600 sm:h-10 sm:w-10">
            <Chrome className="h-4 w-4 text-white sm:h-5 sm:w-5" />
          </div>
          <h1 className="text-gray-900">확장프로그램</h1>
        </div>
        <p className="mb-6 text-gray-600">
          전문가가 검증한 프롬프트를 ChatGPT에서 바로 사용하고, <br className="block sm:hidden" />
          당신의 경험을 공유하세요
        </p>

        {/* 통계 카드들 */}
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
          <div className="rounded-xl border border-blue-100 bg-blue-50 p-4">
            <div className="mb-2 flex items-center justify-center gap-2">
              <Users className="h-4 w-4 text-blue-600" />
              <h1 className="text-blue-700">12K+</h1>
            </div>
            <p className="text-xs text-blue-600 sm:text-sm">활성 사용자</p>
          </div>
          <div className="rounded-xl border border-green-100 bg-green-50 p-4">
            <div className="mb-2 flex items-center justify-center gap-2">
              <Download className="h-4 w-4 text-green-600" />
              <h1 className="text-green-700">25K+</h1>
            </div>
            <p className="text-xs text-green-600 sm:text-sm">다운로드 수</p>
          </div>
          <div className="rounded-xl border border-yellow-100 bg-yellow-50 p-4">
            <div className="mb-2 flex items-center justify-center gap-2">
              <Star className="h-4 w-4 text-yellow-600" />
              <h1 className="text-yellow-700">4.8</h1>
            </div>
            <p className="text-xs text-yellow-600 sm:text-sm">평균 만족도</p>
          </div>
          <div className="rounded-xl border border-purple-100 bg-purple-50 p-4">
            <div className="mb-2 flex items-center justify-center gap-2">
              <Zap className="h-4 w-4 text-purple-600" />
              <h1 className="text-purple-700">5K+</h1>
            </div>
            <p className="text-xs text-purple-600 sm:text-sm">프롬프트 DB</p>
          </div>
        </div>
      </div>
    </div>
  )
}
