'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardContent } from '@/components/ui/card'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import AvatarWithColors from '@/components/common/avatar-with-colors'
import { AccountInfoSection } from '@/components/profile/settings/account-info-section'
import { NotificationSection } from '@/components/profile/settings/notification-section'
import { PrivacySection } from '@/components/profile/settings/privacy-section'
import { SecuritySection } from '@/components/profile/settings/security-section'
import { AccountManagement } from '@/components/profile/settings/account-management'
import { useAuthContext } from '@/components/layout/auth-provider'
import { authApi } from '@/lib/api'

export default function SettingsPage() {
  const router = useRouter()
  const { user, logout } = useAuthContext()

  // 저장 실패 시 바로 되돌릴 수 있게 각 설정값은 화면 상태로 먼저 들고 있는다.
  const accountEmail = user?.email ?? ''
  const [accountNotificationsEnabled, setAccountNotificationsEnabled] = useState(true)
  const [accountInAppNotificationsEnabled, setAccountInAppNotificationsEnabled] = useState(true)
  const [accountPublicProfile, setAccountPublicProfile] = useState(true)
  const [accountDataSharing, setAccountDataSharing] = useState(false)
  const [securityTwoFactorAuth, setSecurityTwoFactorAuth] = useState(false)
  const [settingsLoadError, setSettingsLoadError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState<
    'account' | 'notification' | 'privacy' | 'security' | 'manage'
  >('account')

  // 초기 설정 로드
  useEffect(() => {
    const initSettings = async () => {
      try {
        const s = await authApi.getSettings()
        setAccountNotificationsEnabled(!!s.email_notifications_enabled)
        setAccountInAppNotificationsEnabled(!!s.in_app_notifications_enabled)
        setAccountPublicProfile(!!s.public_profile)
        setAccountDataSharing(!!s.data_sharing)
        setSecurityTwoFactorAuth(!!s.two_factor_auth_enabled)
        setSettingsLoadError(null)
      } catch (e) {
        const message = e instanceof Error ? e.message : '설정을 불러오지 못했습니다.'
        setSettingsLoadError(message)
      }
    }
    initSettings()
  }, [])

  const handleBack = () => router.back()

  // 실시간 저장 함수들
  const handleNotificationsChange = async (value: boolean) => {
    const prev = accountNotificationsEnabled
    setAccountNotificationsEnabled(value)
    try {
      await authApi.updateSettings({ email_notifications_enabled: value })
    } catch (e) {
      setAccountNotificationsEnabled(prev)
      alert('설정 저장 중 오류가 발생했습니다.')
    }
  }

  const handleInAppNotificationsChange = async (value: boolean) => {
    const prev = accountInAppNotificationsEnabled
    setAccountInAppNotificationsEnabled(value)
    try {
      await authApi.updateSettings({ in_app_notifications_enabled: value })
    } catch (e) {
      setAccountInAppNotificationsEnabled(prev)
      alert('설정 저장 중 오류가 발생했습니다.')
    }
  }

  const handlePublicProfileChange = async (value: boolean) => {
    const prev = accountPublicProfile
    setAccountPublicProfile(value)
    try {
      await authApi.updateSettings({ public_profile: value })
    } catch (e) {
      setAccountPublicProfile(prev)
      alert('설정 저장 중 오류가 발생했습니다.')
    }
  }

  const handleDataSharingChange = async (value: boolean) => {
    const prev = accountDataSharing
    setAccountDataSharing(value)
    try {
      await authApi.updateSettings({ data_sharing: value })
    } catch (e) {
      setAccountDataSharing(prev)
      alert('설정 저장 중 오류가 발생했습니다.')
    }
  }

  const handleTwoFactorAuthChange = async (value: boolean) => {
    const prev = securityTwoFactorAuth
    setSecurityTwoFactorAuth(value)
    try {
      await authApi.updateSettings({ two_factor_auth_enabled: value })
    } catch (e) {
      setSecurityTwoFactorAuth(prev)
      alert('설정 저장 중 오류가 발생했습니다.')
    }
  }

  const handleDeleteAccount = async (confirmation: string) => {
    if (confirmation !== '계정 삭제') {
      alert('확인 문구를 정확히 입력해주세요.')
      return
    }
    try {
      const res = await authApi.deleteAccount(confirmation)
      alert(res.message || '계정이 성공적으로 삭제되었습니다.')
      // 계정 삭제 성공 시 서버 토큰/사용자가 이미 무효화되므로 추가 logout API 호출은 생략
      await logout({ skipBackendRequest: true })
      router.push('/')
    } catch (error: any) {
      const message = error?.message || '계정 삭제 중 오류가 발생했습니다.'
      alert(message)
    }
  }

  const handleTerminateSession = (sessionId: number) => {
    alert('세션이 종료되었습니다.')
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4">
        <div className="mx-auto max-w-3xl space-y-6">
          {/* 상단 영역 */}
          <div className="relative overflow-hidden rounded-2xl border border-gray-200 bg-white">
            <div className="pointer-events-none absolute inset-0 opacity-50 [background:radial-gradient(1200px_600px_at_100%_-10%,rgba(59,130,246,0.15),transparent_60%),radial-gradient(1200px_600px_at_0%_-10%,rgba(168,85,247,0.15),transparent_60%)]" />
            <div className="relative flex items-center gap-4 p-6">
              <AvatarWithColors
                username={user?.username || undefined}
                email={user?.email || undefined}
                avatarUrl={(user as any)?.profile_image || null}
                avatarColor1={(user as any)?.avatar_color1}
                avatarColor2={(user as any)?.avatar_color2}
                size="lg"
              />
              <div className="min-w-0">
                <h1 className="truncate text-2xl font-bold text-gray-900">설정</h1>
                <p className="truncate text-sm text-muted-foreground">{accountEmail}</p>
              </div>
              <div className="ml-auto">
                <Button variant="ghost" onClick={handleBack} className="text-muted-foreground">
                  뒤로
                </Button>
              </div>
            </div>
            <div className="h-1 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500" />
          </div>

          {settingsLoadError ? (
            <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              설정 로드 실패: {settingsLoadError}
            </div>
          ) : null}

          {/* 설정 탭 */}
          <Card className="overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
            <Tabs value={activeTab} onValueChange={v => setActiveTab(v as any)}>
              <CardHeader className="p-0">
                <div className="border-b border-gray-100 bg-white/60 p-2">
                  <TabsList className="grid w-full grid-cols-5">
                    <TabsTrigger value="account">계정</TabsTrigger>
                    <TabsTrigger value="notification">알림</TabsTrigger>
                    <TabsTrigger value="privacy">개인정보</TabsTrigger>
                    <TabsTrigger value="security">보안</TabsTrigger>
                    <TabsTrigger value="manage">관리</TabsTrigger>
                  </TabsList>
                </div>
              </CardHeader>
              <CardContent className="p-6">
                <TabsContent value="account" className="m-0">
                  <AccountInfoSection email={accountEmail} />
                </TabsContent>
                <TabsContent value="notification" className="m-0">
                  <NotificationSection
                    emailNotificationsEnabled={accountNotificationsEnabled}
                    inAppNotificationsEnabled={accountInAppNotificationsEnabled}
                    onEmailNotificationsChange={handleNotificationsChange}
                    onInAppNotificationsChange={handleInAppNotificationsChange}
                  />
                </TabsContent>
                <TabsContent value="privacy" className="m-0">
                  <PrivacySection
                    publicProfile={accountPublicProfile}
                    dataSharing={accountDataSharing}
                    onPublicProfileChange={handlePublicProfileChange}
                    onDataSharingChange={handleDataSharingChange}
                  />
                </TabsContent>
                <TabsContent value="security" className="m-0">
                  <SecuritySection
                    twoFactorAuth={securityTwoFactorAuth}
                    onTwoFactorAuthChange={handleTwoFactorAuthChange}
                    onTerminateSession={handleTerminateSession}
                  />
                </TabsContent>
                <TabsContent value="manage" className="m-0">
                  <AccountManagement onDeleteAccount={handleDeleteAccount} />
                </TabsContent>
              </CardContent>
            </Tabs>
          </Card>
        </div>
      </div>
    </div>
  )
}
