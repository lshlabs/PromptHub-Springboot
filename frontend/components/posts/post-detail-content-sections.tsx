/**
 * PostContentSections 컴포넌트
 *
 * 게시글 상세 페이지의 내용 섹션들
 * 프롬프트, AI 응답, 추가 의견, 태그를 포함
 */

'use client'

import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { Copy, Hash } from 'lucide-react'
import type { PostDetail } from '@/types/api'
import CustomBadge from '@/components/common/custom-badge'
import { useToast } from '@/hooks/use-toast'

interface PostContentSectionsProps {
  // 개별 필드 버전
  prompt?: string
  aiResponse?: string
  additionalOpinion?: string
  tags?: string[]
  className?: string
  // PostDetail 전체 객체 버전
  post?: PostDetail
}

export function PostContentSections({
  prompt,
  aiResponse,
  additionalOpinion,
  tags,
  className = '',
  post,
}: PostContentSectionsProps) {
  const { toast } = useToast()

  // post 객체가 전달되면 개별 필드 추출
  const finalPrompt = prompt || post?.prompt || ''
  const finalAiResponse = aiResponse || post?.aiResponse || ''
  const finalAdditionalOpinion = additionalOpinion || post?.additionalOpinion || ''
  const finalTags = tags || post?.tags || []

  const looksLikeCode = (text: string): boolean => {
    const trimmed = text.trim()
    if (!trimmed) return false
    if (trimmed.includes('```')) return true
    const lines = trimmed.split('\n').filter(Boolean)
    if (lines.length < 2) return false

    const codeLikeLineCount = lines.filter(line =>
      /[{}();=<>\[\]]/.test(line) ||
      /^\s*(const|let|var|function|class|if|for|while|return|import|export|def)\b/.test(line) ||
      /^\s*#include\b/.test(line) ||
      /^\s*<\/?[a-zA-Z]/.test(line),
    ).length

    return codeLikeLineCount >= Math.max(2, Math.ceil(lines.length * 0.35))
  }

  const renderRichText = (text: string, options?: { disableHeuristicCodeDetection?: boolean }) => {
    const normalized = (text || '').replace(/\r\n/g, '\n')
    const fencedMatches = [...normalized.matchAll(/```(\w+)?\n?([\s\S]*?)```/g)]

    // 코드 펜스가 없으면 일반 텍스트(줄바꿈 보존) 또는 전체 코드 블록으로 렌더링
    if (fencedMatches.length === 0) {
      if (!options?.disableHeuristicCodeDetection && looksLikeCode(normalized)) {
        return (
          <pre className="overflow-x-auto rounded-lg bg-gray-900 p-4 text-sm text-gray-100">
            <code className="whitespace-pre">{normalized}</code>
          </pre>
        )
      }

      return <p className="whitespace-pre-wrap break-words text-gray-600">{normalized}</p>
    }

    // fenced code + 일반 텍스트 혼합 렌더링
    const parts: Array<{ type: 'text' | 'code'; content: string; lang?: string }> = []
    let lastIndex = 0
    for (const match of fencedMatches) {
      const full = match[0]
      const lang = (match[1] || '').trim()
      const code = (match[2] || '').replace(/\n$/, '')
      const start = match.index ?? 0

      if (start > lastIndex) {
        const textPart = normalized.slice(lastIndex, start)
        if (textPart.trim()) parts.push({ type: 'text', content: textPart })
      }

      parts.push({ type: 'code', content: code, lang })
      lastIndex = start + full.length
    }

    if (lastIndex < normalized.length) {
      const tail = normalized.slice(lastIndex)
      if (tail.trim()) parts.push({ type: 'text', content: tail })
    }

    return (
      <div className="space-y-3">
        {parts.map((part, index) =>
          part.type === 'code' ? (
            <div key={index} className="overflow-hidden rounded-lg border border-gray-700 bg-gray-900">
              {part.lang ? (
                <div className="border-b border-gray-700 px-3 py-1 text-xs text-gray-300">
                  {part.lang}
                </div>
              ) : null}
              <pre className="overflow-x-auto p-4 text-sm text-gray-100">
                <code className="whitespace-pre">{part.content}</code>
              </pre>
            </div>
          ) : (
            <p key={index} className="whitespace-pre-wrap break-words text-gray-600">
              {part.content.trim()}
            </p>
          ),
        )}
      </div>
    )
  }

  // 클립보드 복사 함수
  const copyToClipboard = async (text: string, sectionName: string) => {
    try {
      await navigator.clipboard.writeText(text)
      toast({
        title: '복사 완료',
        description: `${sectionName}이(가) 클립보드에 복사되었습니다.`,
      })
    } catch (error) {
      toast({
        title: '복사 실패',
        description: '클립보드 복사에 실패했습니다.',
        variant: 'destructive',
      })
    }
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* 프롬프트 섹션 */}
      <div className="rounded-xl border border-gray-200 bg-white">
        <div className="rounded-t-xl border-b border-gray-200 bg-blue-50 px-4 py-3">
          <div className="flex items-center justify-between gap-2">
            <h3 className="font-bold text-gray-800">프롬프트</h3>
            <CustomBadge
              variant="white"
              size="responsive"
              icon={<Copy className="mr-1 h-3 w-3" />}
              onClick={() => copyToClipboard(finalPrompt, '프롬프트')}
              className="cursor-pointer transition-colors hover:bg-gray-100">
              복사
            </CustomBadge>
          </div>
        </div>
        <div className="scrollbar-force max-h-[32rem] overflow-y-auto p-4">
          {renderRichText(finalPrompt)}
        </div>
      </div>

      {/* AI 응답 섹션 */}
      <div className="rounded-xl border border-gray-200 bg-white">
        <div className="rounded-t-xl border-b border-gray-200 bg-blue-50 px-4 py-3">
          <div className="flex items-center justify-between gap-2">
            <h3 className="font-bold text-gray-800">AI 응답</h3>
            <CustomBadge
              variant="white"
              size="responsive"
              icon={<Copy className="mr-1 h-3 w-3" />}
              onClick={() => copyToClipboard(finalAiResponse, 'AI 응답')}
              className="cursor-pointer transition-colors hover:bg-gray-100">
              복사
            </CustomBadge>
          </div>
        </div>
        <div className="scrollbar-force max-h-[32rem] overflow-y-auto p-4">
          {renderRichText(finalAiResponse, { disableHeuristicCodeDetection: true })}
        </div>
      </div>

      {/* 추가 의견 섹션 */}
      {finalAdditionalOpinion && (
        <div className="rounded-xl border border-gray-200 bg-white">
          <div className="rounded-t-xl border-b border-gray-200 bg-blue-50 px-4 py-3">
            <h3 className="font-bold text-gray-800">추가 의견</h3>
          </div>
          <div className="scrollbar-force max-h-[32rem] overflow-y-auto p-4">
            {renderRichText(finalAdditionalOpinion)}
          </div>
        </div>
      )}

      {/* 태그 섹션 */}
      {finalTags.length > 0 && (
        <div className="rounded-xl border border-gray-200 bg-white">
          <div className="rounded-t-xl border-b border-gray-200 bg-gray-50 px-4 py-3">
            <h3 className="font-bold text-gray-800">태그</h3>
          </div>
          <div className="p-4">
            <TooltipProvider>
              <div className="flex flex-wrap gap-2">
                {finalTags.map((tag, index) => {
                  const isMobileHidden = index >= 3
                  return (
                    <CustomBadge
                      key={index}
                      variant="green"
                      size="responsive"
                      icon={<Hash className="mr-1 h-3 w-3" />}
                      className={isMobileHidden ? 'hidden sm:flex' : ''}>
                      {tag}
                    </CustomBadge>
                  )
                })}

                {finalTags.length > 3 && (
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <CustomBadge variant="green" size="responsive" className="sm:hidden">
                        +{finalTags.length - 3}
                      </CustomBadge>
                    </TooltipTrigger>
                    <TooltipContent side="bottom" className="max-w-xs">
                      <div className="flex flex-wrap gap-1">
                        {finalTags.slice(3).map((tag, index) => (
                          <CustomBadge
                            key={index}
                            variant="green"
                            size="responsive"
                            icon={<Hash className="mr-1 h-2.5 w-2.5" />}
                            className="sm:hidden">
                            {tag}
                          </CustomBadge>
                        ))}
                      </div>
                    </TooltipContent>
                  </Tooltip>
                )}
              </div>
            </TooltipProvider>
          </div>
        </div>
      )}
    </div>
  )
}
