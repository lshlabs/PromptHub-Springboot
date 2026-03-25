'use client'

import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'

interface AvatarWithColorsProps {
  username?: string
  email?: string
  avatarUrl?: string | null
  avatarColor1?: string
  avatarColor2?: string
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

const sizeClasses = {
  sm: 'h-8 w-8 text-sm',
  md: 'h-10 w-10 text-base',
  lg: 'h-12 w-12 text-lg',
}

export default function AvatarWithColors({
  username,
  email,
  avatarUrl,
  avatarColor1 = '#6B73FF',
  avatarColor2 = '#9EE5FF',
  size = 'md',
  className = '',
}: AvatarWithColorsProps) {
  // 이니셜 생성 (사용자명의 첫 글자 또는 이메일의 첫 글자)
  const getInitial = () => {
    if (username) return username.charAt(0).toUpperCase()
    if (email) return email.charAt(0).toUpperCase()
    return '?'
  }

  // 그라데이션 배경 스타일
  const gradientStyle = {
    background: `linear-gradient(135deg, ${avatarColor1} 0%, ${avatarColor2} 100%)`,
  }

  return (
    <Avatar className={`${sizeClasses[size]} ${className}`}>
      {avatarUrl ? (
        <AvatarImage src={avatarUrl} alt={username || email || 'User avatar'} />
      ) : null}
      <AvatarFallback
        style={gradientStyle}
        className="text-white font-medium border-0"
      >
        {getInitial()}
      </AvatarFallback>
    </Avatar>
  )
}