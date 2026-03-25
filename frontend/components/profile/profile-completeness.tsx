'use client'

import { CheckCircle } from 'lucide-react'
import { useState, useEffect, useRef } from 'react'

interface ProfileCompletenessProps {
  completeness: {
    percentage: number
    completed_fields: number
    total_fields: number
    missing_fields: string[]
  }
}

export function ProfileCompleteness({ completeness }: ProfileCompletenessProps) {
  const [showCompletion, setShowCompletion] = useState(false)
  const hasShownCompletionRef = useRef(false)
  const isInitializedRef = useRef(false)

  // 로컬 스토리지에서 완성도 달성 상태 확인 (한 번만 실행)
  useEffect(() => {
    if (isInitializedRef.current) return // 이미 초기화된 경우 스킵

    const storedCompletion = localStorage.getItem('profile_completion_achieved')
    if (storedCompletion === 'true') {
      hasShownCompletionRef.current = true
    }
    isInitializedRef.current = true
  }, [])

  // 100% 달성 시 애니메이션 효과 및 로컬 스토리지 저장
  useEffect(() => {
    if (completeness.percentage === 100 && !hasShownCompletionRef.current) {
      hasShownCompletionRef.current = true
      setShowCompletion(true)

      // 로컬 스토리지에 완성도 달성 상태 저장
      localStorage.setItem('profile_completion_achieved', 'true')

      // 3초 후 완성 메시지 숨김
      const timer = setTimeout(() => {
        setShowCompletion(false)
      }, 3000)

      return () => clearTimeout(timer)
    }

    // 완성도가 100% 미만으로 떨어졌을 때 상태 리셋
    if (completeness.percentage < 100 && hasShownCompletionRef.current) {
      hasShownCompletionRef.current = false
      localStorage.removeItem('profile_completion_achieved')
    }
  }, [completeness.percentage])

  // 100% 달성 시 축하 메시지 표시 (애니메이션 포함, 한 번만 표시)
  if (completeness.percentage === 100 && showCompletion) {
    return (
      <div className="rounded-lg border border-green-200 bg-green-50 p-4 duration-500 animate-in slide-in-from-top-2">
        <div className="flex items-center">
          <CheckCircle className="mr-2 h-5 w-5 animate-pulse text-green-600" />
          <span className="font-medium text-green-800">프로필이 완성되었습니다! 🎉</span>
        </div>
      </div>
    )
  }

  // 100% 달성 후에는 완전히 숨김 (공간도 차지하지 않음)
  if (completeness.percentage === 100) {
    return null
  }

  return (
    <div className="rounded-lg border border-blue-200 bg-blue-50 p-4">
      <div className="mb-2 flex items-center justify-between">
        <span className="font-medium text-blue-800">프로필 완성도: {completeness.percentage}%</span>
        <span className="text-sm text-blue-600">
          {completeness.completed_fields}/{completeness.total_fields}
        </span>
      </div>
      <div className="h-2 w-full rounded-full bg-blue-200">
        <div
          className="h-2 rounded-full bg-blue-600 transition-all duration-300"
          style={{ width: `${completeness.percentage}%` }}
        />
      </div>
      {completeness.missing_fields.length > 0 && (
        <div className="mt-2 text-sm text-blue-700">
          <p>아직 설정하지 않은 항목:</p>
          <ul className="mt-1 list-inside list-disc">
            {completeness.missing_fields.map(field => (
              <li key={field}>{getFieldDisplayName(field)}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

function getFieldDisplayName(field: string): string {
  const fieldNames: Record<string, string> = {
    username: '사용자명',
    bio: '자기소개',
    location: '위치',
    github_handle: 'GitHub 핸들',
  }
  return fieldNames[field] || field
}
