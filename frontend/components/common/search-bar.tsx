/**
 * SearchBar 컴포넌트
 *
 * 공용 검색 기능을 제공합니다.
 * 백엔드 core 앱의 검색 API를 사용합니다.
 */

'use client'

import { Clock3, Search, X } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

interface SearchBarProps {
  onSearch: (query: string, searchType?: string) => void
  placeholder?: string
  className?: string
  query?: string
  searchTypeValue?: string
}

interface RecentSearchItem {
  query: string
  searchType: string
  updatedAt: number
}

const RECENT_SEARCHES_KEY = 'prompthub_recent_searches_v1'
const RECENT_SEARCHES_MAX = 7

const searchTypes = [
  { value: 'all', label: '전체' },
  { value: 'title', label: '제목' },
  { value: 'content', label: '내용' },
  { value: 'title_content', label: '제목+내용' },
  { value: 'author', label: '작성자' },
]

export function SearchBar({
  onSearch,
  placeholder = '검색어를 입력하세요...',
  className = '',
  query,
  searchTypeValue,
}: SearchBarProps) {
  const rootRef = useRef<HTMLDivElement | null>(null)
  const inputRef = useRef<HTMLInputElement | null>(null)
  const [searchTerm, setSearchTerm] = useState(query ?? '')
  const [searchType, setSearchType] = useState(searchTypeValue ?? 'all')
  const [recentSearches, setRecentSearches] = useState<RecentSearchItem[]>([])
  const [showRecentSearches, setShowRecentSearches] = useState(false)

  const searchTypeLabelMap = useMemo(
    () => Object.fromEntries(searchTypes.map(type => [type.value, type.label])),
    [],
  )

  useEffect(() => {
    if (query !== undefined) {
      setSearchTerm(query)
    }
  }, [query])

  useEffect(() => {
    if (searchTypeValue !== undefined) {
      setSearchType(searchTypeValue)
    }
  }, [searchTypeValue])

  useEffect(() => {
    if (typeof window === 'undefined') return
    try {
      const raw = window.localStorage.getItem(RECENT_SEARCHES_KEY)
      if (!raw) return
      const parsed = JSON.parse(raw) as RecentSearchItem[]
      if (!Array.isArray(parsed)) return
      setRecentSearches(
        parsed.filter(item => item && typeof item.query === 'string' && item.query.trim()),
      )
    } catch {
      // noop: corrupted localStorage should not break search form
    }
  }, [])

  useEffect(() => {
    const handleOutsidePointer = (event: MouseEvent) => {
      if (!rootRef.current) return
      if (rootRef.current.contains(event.target as Node)) return
      setShowRecentSearches(false)
    }

    document.addEventListener('mousedown', handleOutsidePointer)
    return () => document.removeEventListener('mousedown', handleOutsidePointer)
  }, [])

  const saveRecentSearches = (next: RecentSearchItem[]) => {
    setRecentSearches(next)
    if (typeof window === 'undefined') return
    try {
      window.localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(next))
    } catch {
      // noop
    }
  }

  const pushRecentSearch = (nextQuery: string, nextSearchType: string) => {
    const normalizedQuery = nextQuery.trim()
    if (!normalizedQuery) return

    const normalizedType = nextSearchType || 'all'
    const deduped = recentSearches.filter(
      item =>
        !(
          item.query.toLowerCase() === normalizedQuery.toLowerCase() &&
          item.searchType === normalizedType
        ),
    )

    const nextItems: RecentSearchItem[] = [
      { query: normalizedQuery, searchType: normalizedType, updatedAt: Date.now() },
      ...deduped,
    ].slice(0, RECENT_SEARCHES_MAX)

    saveRecentSearches(nextItems)
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value)
  }

  const handleSearch = () => {
    const normalizedQuery = searchTerm.trim()
    const normalizedType = searchType || 'all'
    onSearch(normalizedQuery, normalizedType)
    pushRecentSearch(normalizedQuery, normalizedType)
    setShowRecentSearches(false)
  }

  const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      handleSearch()
    }
  }

  return (
    <div ref={rootRef} className={`mx-auto max-w-2xl ${className}`}>
      <div className="flex gap-2">
      {/* 검색 타입 선택 드롭다운 */}
      <Select value={searchType} onValueChange={setSearchType}>
        <SelectTrigger className="w-32 rounded-xl border border-gray-200 bg-white text-sm">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {searchTypes.map(type => (
            <SelectItem key={type.value} value={type.value} className="text-sm">
              {type.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {/* 검색 입력창 */}
      <div className="relative flex-1">
        <input
          ref={inputRef}
          type="text"
          value={searchTerm}
          placeholder={placeholder}
          onChange={handleInputChange}
          onKeyPress={handleKeyPress}
          onFocus={() => setShowRecentSearches(true)}
          className="w-full rounded-xl border border-gray-200 bg-white py-2.5 pl-4 pr-20 text-sm transition-all duration-200 focus:border-blue-300 focus:outline-none focus:ring-0"
        />
        {searchTerm ? (
          <button
            type="button"
            aria-label="검색어 지우기"
            onClick={() => {
              setSearchTerm('')
              inputRef.current?.focus()
            }}
            className="absolute inset-y-0 right-9 flex items-center">
            <span className="inline-flex h-5 w-5 items-center justify-center rounded-full bg-gray-100 text-gray-500 transition-colors duration-200 hover:bg-gray-200 hover:text-gray-700">
              <X className="h-3.5 w-3.5" />
            </span>
          </button>
        ) : null}
        <button
          type="button"
          onClick={handleSearch}
          className="absolute inset-y-0 right-0 flex items-center rounded-r-xl pr-3 transition-colors duration-200">
          <Search className="h-4 w-4 text-gray-400 hover:text-blue-500" />
        </button>
      </div>
      </div>

      {showRecentSearches && recentSearches.length > 0 ? (
        <div className="mt-2 overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-gray-100 px-3 py-2">
            <div className="flex items-center gap-2 text-xs font-medium text-gray-500">
              <Clock3 className="h-3.5 w-3.5" />
              최근 검색어
            </div>
            <button
              type="button"
              onClick={() => saveRecentSearches([])}
              className="text-xs text-gray-400 transition-colors hover:text-gray-600">
              전체 삭제
            </button>
          </div>
          <ul className="max-h-64 overflow-y-auto py-1">
            {recentSearches.map(item => (
              <li key={`${item.searchType}:${item.query}`}>
                <div className="flex items-center gap-2 px-2 py-1">
                  <button
                    type="button"
                    onClick={() => {
                      setSearchTerm(item.query)
                      setSearchType(item.searchType || 'all')
                      onSearch(item.query, item.searchType || 'all')
                      pushRecentSearch(item.query, item.searchType || 'all')
                      setShowRecentSearches(false)
                    }}
                    className="flex min-w-0 flex-1 items-center gap-2 rounded-lg px-2 py-2 text-left transition-colors hover:bg-gray-50">
                    <span className="shrink-0 rounded-md bg-gray-100 px-2 py-0.5 text-[11px] text-gray-600">
                      {searchTypeLabelMap[item.searchType] || '전체'}
                    </span>
                    <span className="truncate text-sm text-gray-700">{item.query}</span>
                  </button>
                  <button
                    type="button"
                    aria-label="최근 검색어 삭제"
                    onClick={() =>
                      saveRecentSearches(
                        recentSearches.filter(
                          target =>
                            !(
                              target.query === item.query &&
                              target.searchType === item.searchType
                            ),
                        ),
                      )
                    }
                    className="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-md text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600">
                    <X className="h-3.5 w-3.5" />
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </div>
  )
}
