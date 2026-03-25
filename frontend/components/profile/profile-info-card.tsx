'use client'

import { useState, useEffect, useRef } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { MapPin, Github, Camera, RefreshCcw, X, LocateFixed, Loader2 } from 'lucide-react'
import {
  getAvatarGradientStyle,
  getAvatarInitialFromUsername,
  generateRandomAvatarColors,
} from '@/lib/utils'

const MAX_PROFILE_IMAGE_SIZE_BYTES = 5 * 1024 * 1024 // 5MB
const ALLOWED_PROFILE_IMAGE_TYPES = new Set([
  'image/jpeg',
  'image/png',
  'image/webp',
  'image/gif',
])
const ALLOWED_PROFILE_IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'webp', 'gif']

interface UserData {
  username: string
  bio: string
  location: string
  githubHandle: string
  profileImage: string | null
  avatarColor1: string
  avatarColor2: string
}

interface ProfileInfoCardProps {
  userData: UserData
  isEditing: boolean
  onEdit: () => void
  onSave: (data: UserData, profileImageFile?: File | null) => void
  onCancel: () => void
  onAccountSettings: () => void
  onRequestLocationUpdate?: () => Promise<string | null>
  isLoading?: boolean
}

export function ProfileInfoCard({
  userData,
  isEditing,
  onEdit,
  onSave,
  onCancel,
  onAccountSettings,
  onRequestLocationUpdate,
  isLoading = false,
}: ProfileInfoCardProps) {
  const [localData, setLocalData] = useState<UserData>(userData)
  const [selectedProfileImageFile, setSelectedProfileImageFile] = useState<File | null>(null)
  const [imageUploadError, setImageUploadError] = useState<string>('')
  const [isLocationUpdating, setIsLocationUpdating] = useState(false)
  const [locationUpdateError, setLocationUpdateError] = useState('')
  const profileImageInputRef = useRef<HTMLInputElement | null>(null)

  useEffect(() => {
    setLocalData(userData)
    setSelectedProfileImageFile(null)
    setImageUploadError('')
    setIsLocationUpdating(false)
    setLocationUpdateError('')
  }, [userData])

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const input = e.target
    const file = e.target.files?.[0]
    if (!file) return

    const extension = file.name.split('.').pop()?.toLowerCase() || ''
    const isAllowedType = ALLOWED_PROFILE_IMAGE_TYPES.has(file.type)
    const isAllowedExtension = ALLOWED_PROFILE_IMAGE_EXTENSIONS.includes(extension)

    if (!isAllowedType && !isAllowedExtension) {
      setImageUploadError('JPG, PNG, WEBP, GIF 형식만 업로드할 수 있습니다.')
      input.value = ''
      return
    }

    if (file.size > MAX_PROFILE_IMAGE_SIZE_BYTES) {
      setImageUploadError('이미지 용량은 5MB 이하만 업로드할 수 있습니다.')
      input.value = ''
      return
    }

    setImageUploadError('')
    setSelectedProfileImageFile(file)
    const reader = new FileReader()
    reader.onload = e => {
      setLocalData(prev => ({
        ...prev,
        profileImage: e.target?.result as string,
      }))
    }
    reader.readAsDataURL(file)
  }

  const handleSave = () => {
    onSave(localData, selectedProfileImageFile)
  }

  const handleCancel = () => {
    setLocalData(userData)
    setSelectedProfileImageFile(null)
    setImageUploadError('')
    setLocationUpdateError('')
    onCancel()
  }

  const handleRequestLocationUpdate = async () => {
    if (!onRequestLocationUpdate || isLocationUpdating) return

    try {
      setIsLocationUpdating(true)
      setLocationUpdateError('')
      const nextLocation = await onRequestLocationUpdate()
      if (nextLocation) {
        setLocalData(prev => ({ ...prev, location: nextLocation }))
      } else {
        setLocationUpdateError('위치를 가져오지 못했습니다. 직접 입력해주세요.')
      }
    } catch (error: any) {
      setLocationUpdateError(error?.message || '위치를 가져오지 못했습니다. 직접 입력해주세요.')
    } finally {
      setIsLocationUpdating(false)
    }
  }

  return (
    <div className="rounded-lg border border-border bg-background p-6 shadow-sm">
      {!isEditing ? (
        <div className="text-center">
          <h2 className="mb-6 text-xl font-bold text-foreground">프로필</h2>
          <div className="flex flex-col items-center gap-4">
            {isLoading ? (
              <div className="h-24 w-24 animate-pulse rounded-full bg-gray-200"></div>
            ) : (
              <div
                className="flex h-24 w-24 items-center justify-center overflow-hidden rounded-full text-4xl font-bold text-white"
                style={{
                  background: userData.profileImage
                    ? 'transparent'
                    : getAvatarGradientStyle(userData.avatarColor1, userData.avatarColor2),
                }}>
                {userData.profileImage ? (
                  <img
                    src={userData.profileImage}
                    alt={`${userData.username} profile`}
                    className="h-full w-full object-cover"
                  />
                ) : (
                  getAvatarInitialFromUsername(userData.username, 'A')
                )}
              </div>
            )}
            <div className="w-full">
              {isLoading ? (
                <>
                  <div className="mx-auto h-8 w-32 animate-pulse rounded bg-accent"></div>
                  <div className="mx-auto mt-2 h-4 w-48 animate-pulse rounded bg-accent"></div>
                  <div className="mt-4 flex flex-col items-center gap-2">
                    <div className="flex items-center gap-2">
                      <div className="h-4 w-4 animate-pulse rounded bg-accent"></div>
                      <div className="h-4 w-32 animate-pulse rounded bg-accent"></div>
                    </div>
                    <div className="flex items-center gap-2">
                      <div className="h-4 w-4 animate-pulse rounded bg-accent"></div>
                      <div className="h-4 w-40 animate-pulse rounded bg-accent"></div>
                    </div>
                  </div>
                </>
              ) : (
                <>
                  <h1 className="text-2xl font-bold text-foreground">{userData.username}</h1>
                  {userData.bio ? (
                    <p className="mt-2 text-foreground/80">{userData.bio}</p>
                  ) : (
                    <p className="mt-2 italic text-muted-foreground">
                      멋진 말로 자신을 소개해볼까요?
                    </p>
                  )}
                  <div className="mt-4 flex flex-col items-center gap-2 text-muted-foreground">
                    {userData.location ? (
                      <div className="flex items-center gap-2">
                        <MapPin className="h-4 w-4" />
                        <span>{userData.location}</span>
                      </div>
                    ) : (
                      <div className="flex items-center gap-2 italic text-muted-foreground">
                        <MapPin className="h-4 w-4" />
                        <span>위치를 확인할 수 없습니다</span>
                      </div>
                    )}
                    {userData.githubHandle ? (
                      <div className="flex items-center gap-2">
                        <Github className="h-4 w-4" />
                        <span>{userData.githubHandle}</span>
                      </div>
                    ) : (
                      <div className="flex items-center gap-2 italic text-muted-foreground">
                        <Github className="h-4 w-4" />
                        <span>GitHub를 사용하고 있으신가요?</span>
                      </div>
                    )}
                  </div>
                </>
              )}
              <div className="mt-6 flex flex-col gap-2">
                <Button
                  variant="outline"
                  className="w-full border-border bg-background text-foreground hover:bg-accent"
                  onClick={onEdit}
                  disabled={isLoading}>
                  프로필 수정
                </Button>
                <Button
                  variant="outline"
                  className="w-full border-border bg-background text-foreground hover:bg-accent"
                  onClick={onAccountSettings}
                  disabled={isLoading}>
                  계정 설정
                </Button>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div>
          <h2 className="mb-6 text-center text-xl font-bold text-foreground">프로필 수정</h2>
          <div className="space-y-4">
            <div className="flex flex-col items-center gap-4">
              <div className="group relative">
                <div
                  className="flex h-24 w-24 items-center justify-center overflow-hidden rounded-full text-4xl font-bold text-white"
                  style={{
                    background: localData.profileImage
                      ? 'transparent'
                      : getAvatarGradientStyle(localData.avatarColor1, localData.avatarColor2),
                  }}>
                  {localData.profileImage ? (
                    <img
                      src={localData.profileImage}
                      alt="Profile"
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    getAvatarInitialFromUsername(localData.username, 'A')
                  )}
                </div>

                <button
                  type="button"
                  onClick={() => {
                    profileImageInputRef.current?.click()
                  }}
                  className="absolute bottom-0 right-0 cursor-pointer rounded-full bg-white p-1 shadow-md hover:bg-gray-50">
                  <Camera className="h-4 w-4 text-gray-600" />
                </button>

                <button
                  type="button"
                  onClick={() => {
                    const next = generateRandomAvatarColors()
                    setLocalData(prev => ({
                      ...prev,
                      avatarColor1: next.color1,
                      avatarColor2: next.color2,
                    }))
                  }}
                  title="아바타 그라디언트 재생성"
                  className="absolute bottom-0 left-0 rounded-full bg-white p-1 shadow-md transition-colors hover:bg-gray-50">
                  <RefreshCcw className="h-4 w-4 text-gray-600" />
                </button>
                <input
                  ref={profileImageInputRef}
                  id="profile-image"
                  type="file"
                  accept=".jpg,.jpeg,.png,.webp,.gif"
                  onChange={handleImageUpload}
                  className="sr-only"
                />

                {localData.profileImage && (
                  <button
                    onClick={() => {
                      setLocalData(prev => ({ ...prev, profileImage: null }))
                      setSelectedProfileImageFile(null)
                      setImageUploadError('')
                    }}
                    className="absolute right-0 top-0 rounded-full bg-red-500 p-1 text-white opacity-0 shadow-md transition-opacity duration-200 hover:bg-red-600 group-hover:opacity-100">
                    <X className="h-3 w-3" />
                  </button>
                )}
              </div>
              {imageUploadError ? (
                <p className="max-w-xs text-center text-xs text-red-600">{imageUploadError}</p>
              ) : (
                <p className="text-xs text-muted-foreground">JPG, PNG, WEBP, GIF / 최대 5MB</p>
              )}
            </div>
            <div className="grid gap-2">
              <Label htmlFor="username" className="text-foreground">
                닉네임
              </Label>
              <Input
                id="username"
                value={localData.username}
                onChange={e => setLocalData(prev => ({ ...prev, username: e.target.value }))}
                className="border-input bg-background text-foreground placeholder:text-muted-foreground"
                placeholder="닉네임을 입력해주세요"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="bio" className="text-foreground">
                자기소개
              </Label>
              <Textarea
                id="bio"
                value={localData.bio}
                onChange={e => setLocalData(prev => ({ ...prev, bio: e.target.value }))}
                className="border-input bg-background text-foreground placeholder:text-muted-foreground"
                placeholder="간단한 자기소개를 입력해주세요"
                rows={3}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="location" className="text-foreground">
                위치
              </Label>
              <div className="relative">
                <Input
                  id="location"
                  value={localData.location}
                  onChange={e => setLocalData(prev => ({ ...prev, location: e.target.value }))}
                  className="border-input bg-background pr-10 text-foreground placeholder:text-muted-foreground"
                  placeholder="위치"
                />
                <button
                  type="button"
                  onClick={handleRequestLocationUpdate}
                  disabled={!onRequestLocationUpdate || isLocationUpdating}
                  title="현재 위치 가져오기"
                  className="absolute right-1 top-1/2 inline-flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-md text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700 disabled:cursor-not-allowed disabled:opacity-50">
                  {isLocationUpdating ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <LocateFixed className="h-4 w-4" />
                  )}
                </button>
              </div>
              {locationUpdateError ? (
                <p className="text-xs text-red-600">{locationUpdateError}</p>
              ) : (
                <p className="text-xs text-muted-foreground">
                  GPS 아이콘을 눌러 현재 위치를 채울 수 있습니다.
                </p>
              )}
            </div>
            <div className="grid gap-2">
              <Label htmlFor="github" className="text-foreground">
                GitHub
              </Label>
              <Input
                id="github"
                value={localData.githubHandle}
                onChange={e => setLocalData(prev => ({ ...prev, githubHandle: e.target.value }))}
                className="border-input bg-background text-foreground placeholder:text-muted-foreground"
                placeholder="https://github.com/example"
              />
            </div>
            <div className="flex flex-col gap-2 pt-4">
              <Button
                onClick={handleSave}
                className="w-full bg-gray-900 text-gray-100 hover:bg-gray-800">
                저장
              </Button>
              <Button
                onClick={handleCancel}
                variant="outline"
                className="w-full border-gray-300 bg-white text-gray-900 hover:bg-gray-100">
                취소
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
