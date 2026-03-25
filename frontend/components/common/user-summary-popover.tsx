'use client'

import type { ReactNode } from 'react'
import { useEffect, useRef, useState } from 'react'
import { useRouter } from 'next/navigation'

import AvatarWithColors from '@/components/common/avatar-with-colors'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { authApi } from '@/lib/api'
import type { UserSummaryDTO } from '@/types/api'

interface UserSummaryPopoverProps {
  username?: string | null
  children: ReactNode
  align?: 'start' | 'center' | 'end'
  sideOffset?: number
  className?: string
}

const summaryCache = new Map<string, UserSummaryDTO>()

const formatCount = (value: number) => value.toLocaleString('ko-KR')

const formatCreatedAt = (value: string) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleDateString('ko-KR')
}

export function UserSummaryPopover({
  username,
  children,
  align = 'start',
  sideOffset = 8,
  className,
}: UserSummaryPopoverProps) {
  const router = useRouter()
  const normalizedUsername = username?.trim()
  const [open, setOpen] = useState(false)
  const [data, setData] = useState<UserSummaryDTO | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [canHover, setCanHover] = useState(false)
  const openTimerRef = useRef<number | null>(null)
  const closeTimerRef = useRef<number | null>(null)

  useEffect(() => {
    if (typeof window === 'undefined') return

    const mediaQuery = window.matchMedia('(hover: hover) and (pointer: fine)')
    const syncCanHover = () => setCanHover(mediaQuery.matches)

    syncCanHover()
    mediaQuery.addEventListener?.('change', syncCanHover)

    return () => {
      mediaQuery.removeEventListener?.('change', syncCanHover)
    }
  }, [])

  const clearTimers = () => {
    if (typeof window === 'undefined') return

    if (openTimerRef.current !== null) {
      window.clearTimeout(openTimerRef.current)
      openTimerRef.current = null
    }
    if (closeTimerRef.current !== null) {
      window.clearTimeout(closeTimerRef.current)
      closeTimerRef.current = null
    }
  }

  const openByHover = () => {
    if (!canHover || typeof window === 'undefined') return
    if (closeTimerRef.current !== null) {
      window.clearTimeout(closeTimerRef.current)
      closeTimerRef.current = null
    }
    if (open) return
    if (openTimerRef.current !== null) return
    openTimerRef.current = window.setTimeout(() => {
      setOpen(true)
      openTimerRef.current = null
    }, 180)
  }

  const scheduleCloseByHover = () => {
    if (!canHover || typeof window === 'undefined') return
    if (openTimerRef.current !== null) {
      window.clearTimeout(openTimerRef.current)
      openTimerRef.current = null
    }
    if (!open) return
    closeTimerRef.current = window.setTimeout(() => {
      setOpen(false)
      closeTimerRef.current = null
    }, 120)
  }

  useEffect(() => {
    return () => {
      clearTimers()
    }
  }, [])

  useEffect(() => {
    if (!open || !normalizedUsername) return

    const cached = summaryCache.get(normalizedUsername)
    if (cached) {
      setData(cached)
      setError(null)
      return
    }

    let cancelled = false
    setLoading(true)
    setError(null)

    authApi
      .getUserSummary(normalizedUsername)
      .then(result => {
        if (cancelled) return
        summaryCache.set(normalizedUsername, result)
        setData(result)
      })
      .catch(err => {
        if (cancelled) return
        const status = (err as { status?: number } | undefined)?.status
        setError(status === 404 ? '존재하지 않는 사용자입니다.' : '사용자 정보를 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [open, normalizedUsername])

  if (!normalizedUsername) {
    return <>{children}</>
  }

  const handlePostsClick = () => {
    clearTimers()
    setOpen(false)
    router.push(`/community?search=${encodeURIComponent(normalizedUsername)}&search_type=author`, {
      scroll: true,
    })
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <button
          type="button"
          className={className || 'inline-flex items-center text-left'}
          onClick={e => {
            e.stopPropagation()
            clearTimers()
            setOpen(prev => !prev)
          }}
          onPointerDown={e => e.stopPropagation()}
          onMouseEnter={openByHover}
          onMouseLeave={scheduleCloseByHover}>
          {children}
        </button>
      </PopoverTrigger>
      <PopoverContent
        align={align}
        sideOffset={sideOffset}
        className="w-80 p-0"
        onClick={e => e.stopPropagation()}
        onPointerDown={e => e.stopPropagation()}
        onMouseEnter={openByHover}
        onMouseLeave={scheduleCloseByHover}>
        <div className="rounded-md border border-slate-100 bg-white p-4">
          {loading && !data ? (
            <p className="text-sm text-slate-500">사용자 정보를 불러오는 중...</p>
          ) : error ? (
            <p className="text-sm text-rose-600">{error}</p>
          ) : data ? (
            <div className="space-y-3">
              <div className="flex items-center gap-3">
                <AvatarWithColors
                  username={data.username}
                  avatarUrl={data.avatar_url ?? null}
                  avatarColor1={data.avatar_color1}
                  avatarColor2={data.avatar_color2}
                  size="md"
                />
                <div className="min-w-0">
                  <p className="truncate font-semibold text-slate-900">{data.username}</p>
                  <p className="text-xs text-slate-500">가입일 {formatCreatedAt(data.created_at)}</p>
                </div>
              </div>

              <p className="min-h-10 text-sm leading-5 text-slate-700">
                {data.bio?.trim() || '등록된 소개가 없습니다.'}
              </p>

              <div className="grid grid-cols-2 gap-2 text-xs">
                <button
                  type="button"
                  onClick={e => {
                    e.stopPropagation()
                    handlePostsClick()
                  }}
                  onPointerDown={e => e.stopPropagation()}
                  className="rounded-md bg-slate-50 p-2 text-left transition-colors hover:bg-slate-100">
                  <p className="text-slate-500">게시글</p>
                  <p className="font-semibold text-slate-900">{formatCount(data.post_count)}</p>
                </button>
                <div className="rounded-md bg-slate-50 p-2">
                  <p className="text-slate-500">조회수</p>
                  <p className="font-semibold text-slate-900">{formatCount(data.total_views)}</p>
                </div>
                <div className="rounded-md bg-slate-50 p-2">
                  <p className="text-slate-500">받은 좋아요</p>
                  <p className="font-semibold text-slate-900">
                    {formatCount(data.total_likes_received)}
                  </p>
                </div>
                <div className="rounded-md bg-slate-50 p-2">
                  <p className="text-slate-500">받은 북마크</p>
                  <p className="font-semibold text-slate-900">
                    {formatCount(data.total_bookmarks_received)}
                  </p>
                </div>
              </div>
            </div>
          ) : (
            <p className="text-sm text-slate-500">사용자 정보를 불러올 수 없습니다.</p>
          )}
        </div>
      </PopoverContent>
    </Popover>
  )
}
