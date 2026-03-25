/**
 * NotificationSection 컴포넌트
 *
 * 설정 페이지의 알림 설정 섹션을 표시합니다.
 * 이메일 알림과 앱 내 알림 토글을 포함합니다.
 */

'use client'

import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'

interface NotificationSectionProps {
  emailNotificationsEnabled: boolean
  inAppNotificationsEnabled: boolean
  onEmailNotificationsChange: (value: boolean) => void
  onInAppNotificationsChange: (value: boolean) => void
}

export function NotificationSection({
  emailNotificationsEnabled,
  inAppNotificationsEnabled,
  onEmailNotificationsChange,
  onInAppNotificationsChange,
}: NotificationSectionProps) {
  return (
    <div className="grid gap-4">
      <div className="flex items-center justify-between">
        <div>
          <Label htmlFor="email-notifications">이메일 알림</Label>
          <p className="text-sm text-gray-500">중요한 업데이트를 이메일로 받습니다</p>
        </div>
        <Switch
          id="email-notifications"
          checked={emailNotificationsEnabled}
          onCheckedChange={onEmailNotificationsChange}
        />
      </div>
      <div className="flex items-center justify-between">
        <div>
          <Label htmlFor="in-app-notifications">앱 내 알림</Label>
          <p className="text-sm text-gray-500">앱 사용 중 실시간 알림을 받습니다</p>
        </div>
        <Switch
          id="in-app-notifications"
          checked={inAppNotificationsEnabled}
          onCheckedChange={onInAppNotificationsChange}
        />
      </div>
    </div>
  )
}
