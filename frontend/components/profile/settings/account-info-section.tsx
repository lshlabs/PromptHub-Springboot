/**
 * AccountInfoSection 컴포넌트
 *
 * 설정 페이지의 계정 정보 섹션을 표시합니다.
 * 이메일 표시와 비밀번호 변경 기능을 포함합니다.
 */

'use client'

import { useState } from 'react'
import { authApi } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ChevronDown, ChevronUp, Eye, EyeOff } from 'lucide-react'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'

interface AccountInfoSectionProps {
  email: string
}

export function AccountInfoSection({ email }: AccountInfoSectionProps) {
  // 비밀번호 변경 상태
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmNewPassword, setConfirmNewPassword] = useState('')
  const [passwordChangeError, setPasswordChangeError] = useState('')
  const [isPasswordChangeOpen, setIsPasswordChangeOpen] = useState(false)
  const [showCurrentPassword, setShowCurrentPassword] = useState(false)
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [showConfirmNewPassword, setShowConfirmNewPassword] = useState(false)

  // 비밀번호 변경 핸들러
  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault()
    setPasswordChangeError('')

    if (newPassword !== confirmNewPassword) {
      setPasswordChangeError('비밀번호가 일치하지 않습니다.')
      return
    }
    // 회원가입과 동일하게 클라이언트에서는 길이 검증을 HTML 속성으로 처리
    try {
      const res = await authApi.changePassword({
        current_password: currentPassword,
        new_password: newPassword,
        new_password_confirm: confirmNewPassword,
      })
      setCurrentPassword('')
      setNewPassword('')
      setConfirmNewPassword('')
      setIsPasswordChangeOpen(false)
      alert(res.message || '비밀번호가 성공적으로 변경되었습니다.')
    } catch (error: any) {
      setPasswordChangeError(error?.message || '비밀번호 변경 중 오류가 발생했습니다.')
    }
  }

  return (
    <div className="grid gap-4">
      <div className="grid gap-2">
        <Label htmlFor="account-email">이메일</Label>
        <Input
          id="account-email"
          type="email"
          value={email}
          readOnly
          disabled
          className="cursor-not-allowed bg-accent"
        />
        <p className="text-xs text-muted-foreground">이메일은 변경할 수 없습니다.</p>
      </div>

      {/* 비밀번호 변경 아코디언 */}
      <Collapsible open={isPasswordChangeOpen} onOpenChange={setIsPasswordChangeOpen}>
        <div className="flex items-center justify-between">
          <div>
            <span className="font-medium text-foreground">비밀번호 변경</span>
            <p className="text-sm text-muted-foreground">
              계정 보안을 위해 정기적으로 비밀번호를 변경하세요
            </p>
          </div>
          <CollapsibleTrigger asChild>
            <Button variant="ghost" size="sm" className="h-6 w-6 p-0 hover:bg-accent">
              {isPasswordChangeOpen ? (
                <ChevronUp className="h-4 w-4 text-muted-foreground" />
              ) : (
                <ChevronDown className="h-4 w-4 text-muted-foreground" />
              )}
            </Button>
          </CollapsibleTrigger>
        </div>
        <CollapsibleContent className="mt-3">
          <div className="rounded-lg border border-border bg-accent p-4">
            <form onSubmit={handleChangePassword} className="grid gap-4">
              <div className="grid gap-2">
                <Label htmlFor="current-password">현재 비밀번호</Label>
                <div className="relative">
                  <Input
                    id="current-password"
                    type={showCurrentPassword ? 'text' : 'password'}
                    value={currentPassword}
                    onChange={e => setCurrentPassword(e.target.value)}
                    className="bg-background pr-10"
                    required
                  />
                  <button
                    type="button"
                    aria-label="현재 비밀번호 표시/숨김"
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                    onClick={() => setShowCurrentPassword(v => !v)}>
                    {showCurrentPassword ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="new-password">새 비밀번호</Label>
                <div className="relative">
                  <Input
                    id="new-password"
                    type={showNewPassword ? 'text' : 'password'}
                    value={newPassword}
                    onChange={e => setNewPassword(e.target.value)}
                    className="bg-background pr-10"
                    minLength={8}
                    required
                  />
                  <button
                    type="button"
                    aria-label="새 비밀번호 표시/숨김"
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                    onClick={() => setShowNewPassword(v => !v)}>
                    {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="confirm-new-password">새 비밀번호 확인</Label>
                <div className="relative">
                  <Input
                    id="confirm-new-password"
                    type={showConfirmNewPassword ? 'text' : 'password'}
                    value={confirmNewPassword}
                    onChange={e => setConfirmNewPassword(e.target.value)}
                    className="bg-background pr-10"
                    required
                  />
                  <button
                    type="button"
                    aria-label="새 비밀번호 확인 표시/숨김"
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                    onClick={() => setShowConfirmNewPassword(v => !v)}>
                    {showConfirmNewPassword ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>
              </div>
              {passwordChangeError && <p className="text-sm text-red-500">{passwordChangeError}</p>}
              <div className="flex justify-end gap-2 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    setCurrentPassword('')
                    setNewPassword('')
                    setConfirmNewPassword('')
                    setPasswordChangeError('')
                    setIsPasswordChangeOpen(false)
                  }}>
                  취소
                </Button>
                <Button
                  type="submit"
                  size="sm"
                  className="bg-blue-500 text-white hover:bg-blue-600">
                  변경
                </Button>
              </div>
            </form>
          </div>
        </CollapsibleContent>
      </Collapsible>
    </div>
  )
}
