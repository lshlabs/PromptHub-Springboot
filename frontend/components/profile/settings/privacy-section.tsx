/**
 * PrivacySection 컴포넌트
 *
 * 설정 페이지의 개인정보 설정 섹션을 표시합니다.
 * 프로필 공개와 데이터 공유 토글을 포함합니다.
 */

'use client'

import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'

interface PrivacySectionProps {
  publicProfile: boolean
  dataSharing: boolean
  onPublicProfileChange: (value: boolean) => void
  onDataSharingChange: (value: boolean) => void
}

export function PrivacySection({
  publicProfile,
  dataSharing,
  onPublicProfileChange,
  onDataSharingChange,
}: PrivacySectionProps) {
  return (
    <div className="grid gap-4">
      <div className="flex items-center justify-between">
        <div>
          <Label htmlFor="public-profile">프로필 공개</Label>
          <p className="text-sm text-gray-500">다른 사용자가 내 프로필을 볼 수 있습니다</p>
        </div>
        <Switch
          id="public-profile"
          checked={publicProfile}
          onCheckedChange={onPublicProfileChange}
        />
      </div>
      <div className="flex items-center justify-between">
        <div>
          <Label htmlFor="data-sharing">데이터 공유</Label>
          <p className="text-sm text-gray-500">서비스 개선을 위한 익명 데이터 공유에 동의합니다</p>
        </div>
        <Switch id="data-sharing" checked={dataSharing} onCheckedChange={onDataSharingChange} />
      </div>
    </div>
  )
}
