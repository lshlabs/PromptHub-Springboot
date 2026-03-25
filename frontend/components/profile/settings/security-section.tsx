/**
 * SecuritySection 컴포넌트
 *
 * 설정 페이지의 보안 섹션을 표시합니다.
 * 2단계 인증과 활성 세션 관리 기능을 포함합니다.
 */

'use client'

import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Badge } from '@/components/ui/badge'
import { ChevronDown, ChevronUp, Monitor, Smartphone, Tablet } from 'lucide-react'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { authApi } from '@/lib/api'
import type { UserSessionDTO } from '@/types/api'

interface SecuritySectionProps {
  twoFactorAuth: boolean
  onTwoFactorAuthChange: (value: boolean) => void
  onTerminateSession: (sessionId: number) => void
}

export function SecuritySection({
  twoFactorAuth,
  onTwoFactorAuthChange,
  onTerminateSession,
}: SecuritySectionProps) {
  const [isActiveSessionsOpen, setIsActiveSessionsOpen] = useState(false)
  const [sessions, setSessions] = useState<UserSessionDTO[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const getCurrentSessionKey = () => {
    if (typeof window === 'undefined') return null
    // 기본 구현: 세션 키를 로컬스토리지에 저장해 두었다고 가정. 없으면 null
    return localStorage.getItem('prompthub_session_key')
  }

  const fetchSessions = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await authApi.getSessions()
      // 방어적 처리: 백엔드가 종료된 세션을 포함해도 화면에는 활성 세션만 표시
      setSessions(data.filter(session => !session.revoked_at))
    } catch (e: any) {
      setError(e?.message || '세션 목록을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (isActiveSessionsOpen) {
      fetchSessions()
    }
  }, [isActiveSessionsOpen])

  return (
    <div className="grid gap-4">
      <div className="flex items-center justify-between">
        <div>
          <Label htmlFor="two-factor-auth">2단계 인증</Label>
          <p className="text-sm text-muted-foreground">로그인 시 추가 보안 단계를 거칩니다</p>
        </div>
        <Switch
          id="two-factor-auth"
          checked={twoFactorAuth}
          onCheckedChange={onTwoFactorAuthChange}
        />
      </div>

      {/* 활성 세션 관리 아코디언 */}
      <Collapsible open={isActiveSessionsOpen} onOpenChange={setIsActiveSessionsOpen}>
        <div className="flex items-center justify-between">
          <div>
            <span className="font-medium text-foreground">활성 세션 관리</span>
            <p className="text-sm text-muted-foreground">
              현재 로그인된 기기들을 확인하고 관리하세요
            </p>
          </div>
          <CollapsibleTrigger asChild>
            <Button variant="ghost" size="sm" className="h-6 w-6 p-0 hover:bg-accent">
              {isActiveSessionsOpen ? (
                <ChevronUp className="h-4 w-4 text-muted-foreground" />
              ) : (
                <ChevronDown className="h-4 w-4 text-muted-foreground" />
              )}
            </Button>
          </CollapsibleTrigger>
        </div>
        <CollapsibleContent className="mt-3">
          <div className="rounded-lg border border-border bg-accent p-4">
            {loading ? (
              <div className="py-4 text-center text-sm text-muted-foreground">불러오는 중...</div>
            ) : error ? (
              <div className="py-4 text-center text-sm text-red-600">{error}</div>
            ) : sessions.length === 0 ? (
              <div className="py-4 text-center text-sm text-muted-foreground">
                활성 세션이 없습니다.
              </div>
            ) : (
              <div className="space-y-3">
                {sessions.map(s => {
                  const isCurrent = getCurrentSessionKey() && s.key === getCurrentSessionKey()
                  const Icon =
                    s.device?.toLowerCase().includes('iphone') ||
                    s.device?.toLowerCase().includes('android')
                      ? Smartphone
                      : s.device?.toLowerCase().includes('ipad')
                        ? Tablet
                        : Monitor
                  return (
                    <div
                      key={s.key}
                      className="flex items-center justify-between rounded-lg border border-border bg-background p-3">
                      <div className="flex items-center gap-3">
                        <Icon className="h-5 w-5 text-muted-foreground" />
                        <div>
                          <div className="flex items-center gap-2">
                            <p className="text-sm font-medium">{s.device || 'Unknown device'}</p>
                            {isCurrent && (
                              <Badge variant="secondary" className="text-xs">
                                현재
                              </Badge>
                            )}
                          </div>
                          <p className="text-sm text-muted-foreground">
                            {s.browser || 'Unknown browser'} •{' '}
                            {s.location || s.ip_address || 'Unknown'}
                          </p>
                          <p className="text-xs text-muted-foreground/70">
                            최근 활동: {new Date(s.last_active).toLocaleString()}
                          </p>
                        </div>
                      </div>
                      {!isCurrent && !s.revoked_at && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={async () => {
                            try {
                              await authApi.endSession(s.key)
                              setSessions(prev =>
                                prev.map(x =>
                                  x.key === s.key
                                    ? { ...x, revoked_at: new Date().toISOString() }
                                    : x,
                                ),
                              )
                            } catch (e) {
                              alert('세션 종료 실패')
                            }
                          }}>
                          종료
                        </Button>
                      )}
                    </div>
                  )
                })}
                <div className="flex justify-end">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={async () => {
                      try {
                        await authApi.endOtherSessions()
                        setSessions(prev => prev.filter(x => x.key === getCurrentSessionKey()))
                      } catch (e) {
                        alert('다른 세션 종료 실패')
                      }
                    }}>
                    다른 모든 세션 종료
                  </Button>
                </div>
              </div>
            )}
          </div>
        </CollapsibleContent>
      </Collapsible>
    </div>
  )
}
