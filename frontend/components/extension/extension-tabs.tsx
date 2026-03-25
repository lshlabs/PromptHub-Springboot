'use client'

import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Chrome, Star, Zap, Shield, CheckCircle, Play } from 'lucide-react'
import ChatGPTDemo from '@/components/extension/chatgpt-demo'
import { useState } from 'react'

interface ExtensionTabsProps {
  activeTab?: string
  onTabChange?: (value: string) => void
}

export default function ExtensionTabs({ activeTab = 'features', onTabChange }: ExtensionTabsProps) {
  const [internalActiveTab, setInternalActiveTab] = useState(activeTab)

  const currentTab = activeTab || internalActiveTab
  const handleTabChange = onTabChange || setInternalActiveTab

  return (
    <Tabs value={currentTab} onValueChange={handleTabChange} className="mb-12">
      <TabsList className="mb-8 grid w-full grid-cols-3 rounded-lg bg-gray-200">
        <TabsTrigger value="features">주요 기능</TabsTrigger>
        <TabsTrigger value="guide">설치 가이드</TabsTrigger>
        <TabsTrigger value="demo">체험하기</TabsTrigger>
      </TabsList>

      {/* 주요 기능 탭 */}
      <TabsContent value="features">
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <Card className="transition-shadow hover:shadow-md">
            <CardContent className="p-6">
              <div className="flex items-start gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-blue-100">
                  <Zap className="h-6 w-6 text-blue-600" />
                </div>
                <div>
                  <h3 className="mb-2 font-semibold text-gray-900">실시간 프롬프트 추천</h3>
                  <p className="text-sm text-gray-600">
                    ChatGPT 사용 중 상황에 맞는 최적의 프롬프트를 자동으로 추천합니다.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="transition-shadow hover:shadow-md">
            <CardContent className="p-6">
              <div className="flex items-start gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-green-100">
                  <Star className="h-6 w-6 text-green-600" />
                </div>
                <div>
                  <h3 className="mb-2 font-semibold text-gray-900">커뮤니티 검증 프롬프트</h3>
                  <p className="text-sm text-gray-600">
                    PromptHub 커뮤니티에서 검증된 고품질 프롬프트만 제공합니다.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="transition-shadow hover:shadow-md">
            <CardContent className="p-6">
              <div className="flex items-start gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-purple-100">
                  <Shield className="h-6 w-6 text-purple-600" />
                </div>
                <div>
                  <h3 className="mb-2 font-semibold text-gray-900">개인정보 보호</h3>
                  <p className="text-sm text-gray-600">
                    모든 데이터는 로컬에서 처리되며, 개인정보는 수집하지 않습니다.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="transition-shadow hover:shadow-md">
            <CardContent className="p-6">
              <div className="flex items-start gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-orange-100">
                  <CheckCircle className="h-6 w-6 text-orange-600" />
                </div>
                <div>
                  <h3 className="mb-2 font-semibold text-gray-900">원클릭 적용</h3>
                  <p className="text-sm text-gray-600">
                    추천된 프롬프트를 클릭 한 번으로 ChatGPT에 바로 적용할 수 있습니다.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </TabsContent>

      {/* 설치 가이드 탭 */}
      <TabsContent value="guide">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          <Card>
            <CardContent className="p-6 text-center">
              <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-blue-600 font-bold text-white">
                1
              </div>
              <h3 className="mb-2 font-semibold">웹스토어 방문</h3>
              <p className="text-sm text-gray-600">Chrome 웹스토어로 이동합니다</p>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6 text-center">
              <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-green-600 font-bold text-white">
                2
              </div>
              <h3 className="mb-2 font-semibold">확장프로그램 설치</h3>
              <p className="text-sm text-gray-600">'Chrome에 추가' 클릭</p>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6 text-center">
              <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-orange-600 font-bold text-white">
                3
              </div>
              <h3 className="mb-2 font-semibold">권한 허용</h3>
              <p className="text-sm text-gray-600">ChatGPT 접근 권한 허용</p>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6 text-center">
              <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-purple-600 font-bold text-white">
                4
              </div>
              <h3 className="mb-2 font-semibold">사용 시작</h3>
              <p className="text-sm text-gray-600">ChatGPT에서 바로 사용</p>
            </CardContent>
          </Card>
        </div>
      </TabsContent>

      {/* 체험해보기 탭 */}
      <TabsContent value="demo">
        <div className="w-full">
          <ChatGPTDemo />
        </div>
      </TabsContent>
    </Tabs>
  )
}

// CTA 섹션 컴포넌트
export function ExtensionCTA({ onDemoClick }: { onDemoClick: () => void }) {
  const handleInstallClick = () => {
    window.open('https://chromewebstore.google.com/', '_blank', 'noopener,noreferrer')
  }

  return (
    <div className="text-center">
      <Card className="border-0 bg-gradient-to-r from-blue-600 to-purple-600 text-white">
        <CardContent className="p-8">
          <h2 className="mb-4 text-2xl font-bold">지금 바로 시작하세요!</h2>
          <p className="mb-6 text-blue-100">
            PromptHub Extension으로 더 스마트한 AI 대화를 경험해보세요
          </p>
          <div className="flex flex-col gap-4 sm:flex-row sm:justify-center">
            <Button
              size="lg"
              className="bg-white text-blue-600 hover:bg-gray-100"
              onClick={handleInstallClick}>
              <Chrome className="mr-2 h-5 w-5" />
              무료로 설치하기
            </Button>
            <Button
              size="lg"
              variant="outline"
              className="border-white/30 bg-white/10 text-white hover:bg-white/20"
              onClick={onDemoClick}>
              <Play className="mr-2 h-5 w-5" />
              체험하기
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
