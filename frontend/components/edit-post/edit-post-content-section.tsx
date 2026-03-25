/**
 * EditPostContentSection 컴포넌트
 *
 * 게시글 수정 페이지의 본문 내용 수정 섹션
 * 제목, 프롬프트, AI 응답, 추가 의견 수정 기능을 포함
 */

'use client'

import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'

interface EditPostContentSectionProps {
  // 개별 필드 버전
  title?: string
  prompt?: string
  aiResponse?: string
  additionalOpinion?: string
  onTitleChange?: (value: string) => void
  onPromptChange?: (value: string) => void
  onAiResponseChange?: (value: string) => void
  onAdditionalOpinionChange?: (value: string) => void
  className?: string
}

export default function EditPostContentSection({
  title = '',
  prompt = '',
  aiResponse = '',
  additionalOpinion = '',
  onTitleChange,
  onPromptChange,
  onAiResponseChange,
  onAdditionalOpinionChange,
  className = '',
}: EditPostContentSectionProps) {
  return (
    <div className={`space-y-6 py-6 ${className}`}>
      {/* 제목 수정 */}
      <div className="rounded-xl border border-gray-200 bg-white">
        <div className="rounded-t-xl border-b border-gray-200 bg-blue-50 px-4 py-2">
          <h3 className="text-gray-800">
            제목 <span className="text-red-500">*</span>
          </h3>
        </div>
        <div>
          <Input
            value={title}
            onChange={e => onTitleChange?.(e.target.value)}
            className="w-full resize-none border-none bg-transparent text-xs text-gray-600 placeholder:text-gray-400 focus:outline-none focus:ring-0 sm:text-base"
            placeholder="제목을 입력하세요"
          />
        </div>
      </div>

      {/* 프롬프트 섹션 */}
      <div className="rounded-xl border border-gray-200 bg-white">
        <div className="rounded-t-xl border-b border-gray-200 bg-blue-50 px-4 py-2">
          <h3 className="text-gray-800">
            프롬프트 <span className="text-red-500">*</span>
          </h3>
        </div>
        <div>
          <Textarea
            value={prompt}
            onChange={e => onPromptChange?.(e.target.value)}
            className="scrollbar-force w-full resize-none border-none bg-transparent text-xs text-gray-600 placeholder:text-gray-400 focus:outline-none focus:ring-0 sm:text-base"
            rows={10}
            placeholder="프롬프트 내용을 입력하세요"
          />
        </div>
      </div>

      {/* AI 응답 섹션 */}
      <div className="rounded-xl border border-gray-200 bg-white">
        <div className="rounded-t-xl border-b border-gray-200 bg-blue-50 px-4 py-2">
          <h3 className="text-gray-800">
            AI 응답 <span className="text-red-500">*</span>
          </h3>
        </div>
        <div>
          <Textarea
            value={aiResponse}
            onChange={e => onAiResponseChange?.(e.target.value)}
            className="scrollbar-force w-full resize-none border-none bg-transparent text-xs text-gray-600 placeholder:text-gray-400 focus:outline-none focus:ring-0 sm:text-base"
            rows={10}
            placeholder="AI 응답 내용을 입력하세요"
          />
        </div>
      </div>

      {/* 추가 의견 섹션 */}
      <div className="rounded-xl border border-gray-200 bg-white">
        <div className="rounded-t-xl border-b border-gray-200 bg-blue-50 px-4 py-2">
          <h3 className="text-gray-800">추가 의견</h3>
        </div>
        <div>
          <Textarea
            value={additionalOpinion}
            onChange={e => onAdditionalOpinionChange?.(e.target.value)}
            className="scrollbar-force w-full resize-none border-none bg-transparent text-xs text-gray-600 placeholder:text-gray-400 focus:outline-none focus:ring-0 sm:text-base"
            rows={1}
            placeholder="추가 의견을 입력하세요 (선택사항)"
          />
        </div>
      </div>
    </div>
  )
}
