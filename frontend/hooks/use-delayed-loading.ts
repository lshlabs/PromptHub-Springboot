'use client'

import { useEffect, useRef, useState } from 'react'

interface UseDelayedLoadingOptions {
  delayMs?: number
  minVisibleMs?: number
}

export function useDelayedLoading(
  loading: boolean,
  options: UseDelayedLoadingOptions = {},
): boolean {
  const { delayMs = 180, minVisibleMs = 320 } = options

  const [visible, setVisible] = useState(false)
  const shownAtRef = useRef<number | null>(null)
  const delayTimerRef = useRef<number | null>(null)
  const hideTimerRef = useRef<number | null>(null)

  useEffect(() => {
    const clearDelayTimer = () => {
      if (delayTimerRef.current !== null) {
        window.clearTimeout(delayTimerRef.current)
        delayTimerRef.current = null
      }
    }

    const clearHideTimer = () => {
      if (hideTimerRef.current !== null) {
        window.clearTimeout(hideTimerRef.current)
        hideTimerRef.current = null
      }
    }

    if (loading) {
      clearHideTimer()

      if (visible) {
        return () => {
          clearDelayTimer()
          clearHideTimer()
        }
      }

      clearDelayTimer()
      delayTimerRef.current = window.setTimeout(() => {
        shownAtRef.current = Date.now()
        setVisible(true)
        delayTimerRef.current = null
      }, delayMs)
    } else {
      clearDelayTimer()

      if (!visible) {
        return () => {
          clearDelayTimer()
          clearHideTimer()
        }
      }

      const shownAt = shownAtRef.current ?? Date.now()
      const elapsed = Date.now() - shownAt
      const remaining = Math.max(0, minVisibleMs - elapsed)

      clearHideTimer()
      hideTimerRef.current = window.setTimeout(() => {
        shownAtRef.current = null
        setVisible(false)
        hideTimerRef.current = null
      }, remaining)
    }

    return () => {
      clearDelayTimer()
      clearHideTimer()
    }
  }, [delayMs, loading, minVisibleMs, visible])

  return visible
}
