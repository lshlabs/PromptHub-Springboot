/**
 * SortSelector 컴포넌트
 *
 * 공용 정렬 선택 컴포넌트입니다.
 * 백엔드 core 앱의 정렬 API를 사용합니다.
 *
 * 지원 정렬 옵션:
 * - latest: 최신순
 * - oldest: 오래된순
 * - popular: 인기순
 * - views: 조회순
 * - likes: 좋아요순
 * - bookmarks: 북마크순
 * - satisfaction: 만족도순
 * - title: 제목순
 * - author: 작성자순
 * - trending: 트렌딩순
 * - engagement: 참여도순
 */

'use client'

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

export type SortOption =
  | 'latest'
  | 'oldest'
  | 'popular'
  | 'satisfaction' // 백엔드와 일치하도록 'rating' → 'satisfaction'으로 변경
  | 'views'

interface SortSelectorProps {
  value: SortOption
  onValueChange: (value: SortOption) => void
  options?: SortOption[] // 커스텀 옵션 설정 가능
  className?: string
  placeholder?: string
}

const defaultOptions: SortOption[] = ['latest', 'oldest', 'popular', 'satisfaction', 'views']

const sortLabels: Record<SortOption, string> = {
  latest: '최신순',
  oldest: '오래된순',
  popular: '인기순',
  satisfaction: '만족도순', // 백엔드와 일치하도록 변경
  views: '조회순',
}

export function SortSelector({
  value,
  onValueChange,
  options = defaultOptions,
  className = '',
  placeholder = '정렬',
}: SortSelectorProps) {
  return (
    <Select value={value} onValueChange={onValueChange}>
      <SelectTrigger
        className={`w-32 rounded-xl border border-gray-200 bg-white text-sm ${className}`}>
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {options.map(option => (
          <SelectItem key={option} value={option}>
            {sortLabels[option]}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}

export default SortSelector
