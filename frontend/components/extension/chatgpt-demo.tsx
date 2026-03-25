'use client'

import type React from 'react'

import { useState, useEffect, useRef } from 'react'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import {
  Plus,
  Paperclip,
  Mic,
  AudioWaveform,
  Star,
  Heart,
  Eye,
  Search,
  Loader2,
  X,
  ArrowUp,
  Copy,
  ThumbsUp,
  ThumbsDown,
  Volume2,
  Edit,
  Download,
  RotateCcw,
} from 'lucide-react'

interface PromptReview {
  id: string
  title: string
  rating: number
  likes: number
  views: number
  prompt: string
}

interface Message {
  id: string
  type: 'user' | 'assistant'
  content: string
  timestamp: Date
}

type DemoStep =
  | 'input'
  | 'extension-click'
  | 'searching'
  | 'results'
  | 'applied'
  | 'conversation'
  | 'completed'

export default function ChatGPTDemo() {
  const [userInput, setUserInput] = useState('')
  const [currentStep, setCurrentStep] = useState<DemoStep>('input')
  const [showExtension, setShowExtension] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [isSearching, setIsSearching] = useState(false)
  const [showResults, setShowResults] = useState(false)
  const [messages, setMessages] = useState<Message[]>([])
  const [isTyping, setIsTyping] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const [showSuccessMessage, setShowSuccessMessage] = useState(false)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const bottomTextareaRef = useRef<HTMLTextAreaElement>(null)
  const headerRef = useRef<HTMLDivElement>(null)

  // 샘플 프롬프트 리뷰 데이터
  const sampleReviews: PromptReview[] = [
    {
      id: '1',
      title: '창의적 글쓰기를 위한 완벽한 프롬프트',
      rating: 4.8,
      likes: 234,
      views: 1200,
      prompt:
        '당신은 창의적인 작가입니다. 다음 주제에 대해 독창적이고 흥미로운 이야기를 써주세요. 이야기는 독자의 감정을 자극하고, 예상치 못한 반전이 있어야 합니다. 주제: 창의적인 소설 쓰기',
    },
    {
      id: '2',
      title: '효과적인 스토리텔링 가이드',
      rating: 4.6,
      likes: 189,
      views: 890,
      prompt:
        '매력적인 스토리를 만들기 위해 다음 구조를 따라 이야기를 작성해주세요: 1) 흥미로운 도입부 2) 갈등 상황 제시 3) 클라이맥스 4) 해결과 교훈. 주제: 창의적인 소설 쓰기',
    },
    {
      id: '3',
      title: '독자 몰입도 높이는 글쓰기 기법',
      rating: 4.7,
      likes: 156,
      views: 670,
      prompt:
        '독자가 몰입할 수 있는 생생한 이야기를 써주세요. 오감을 자극하는 묘사와 대화를 포함하고, 독자가 마치 그 상황에 있는 것처럼 느낄 수 있게 해주세요. 주제: 창의적인 소설 쓰기',
    },
  ]

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({
      behavior: 'smooth',
      block: 'nearest',
      inline: 'nearest',
    })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  useEffect(() => {
    // Scroll to header when component mounts (when tab is activated)
    const timer = setTimeout(() => {
      if (headerRef.current) {
        const headerOffset = 90 // 헤더 높이 (px)
        const elementPosition = headerRef.current.getBoundingClientRect().top
        const offsetPosition = elementPosition + window.pageYOffset - headerOffset

        window.scrollTo({
          top: offsetPosition,
          behavior: 'smooth',
        })
      }
    }, 100) // Small delay to ensure tab transition is complete

    return () => clearTimeout(timer)
  }, [])

  // Textarea 높이 자동 조정 함수
  const adjustTextareaHeight = (textarea: HTMLTextAreaElement) => {
    textarea.style.height = 'auto'
    const scrollHeight = textarea.scrollHeight
    const maxHeight = 200 // 최대 높이 설정 (약 8-10줄)
    textarea.style.height = Math.min(scrollHeight, maxHeight) + 'px'
  }

  const handleInputChange = (value: string) => {
    setUserInput(value)
    if (value.trim() && currentStep === 'input') {
      setCurrentStep('extension-click')
    }

    // 높이 조정
    setTimeout(() => {
      if (textareaRef.current) {
        adjustTextareaHeight(textareaRef.current)
      }
      if (bottomTextareaRef.current) {
        adjustTextareaHeight(bottomTextareaRef.current)
      }
    }, 0)
  }

  const handleExtensionClick = () => {
    if (userInput.trim()) {
      setShowExtension(true)
      setSearchQuery(userInput)
      setCurrentStep('searching')
      setIsSearching(true)

      setTimeout(() => {
        setIsSearching(false)
        setShowResults(true)
        setCurrentStep('results')
      }, 2000)
    }
  }

  const handleReviewClick = (review: PromptReview) => {
    setUserInput(review.prompt)
    setShowExtension(false)
    setCurrentStep('applied')
    setShowSuccessMessage(true)

    // 높이 조정
    setTimeout(() => {
      if (textareaRef.current) {
        adjustTextareaHeight(textareaRef.current)
      }
    }, 0)

    // 3초 후 fade-out
    setTimeout(() => {
      setShowSuccessMessage(false)
    }, 3000)
  }

  const handleSendMessage = () => {
    if (userInput.trim() && currentStep === 'applied') {
      const userMessage: Message = {
        id: Date.now().toString(),
        type: 'user',
        content: userInput,
        timestamp: new Date(),
      }

      setMessages([userMessage])
      setUserInput('')
      setCurrentStep('conversation')
      setIsTyping(true)

      // AI 응답 시뮬레이션 - 더 똑똑하고 프롬프트에 맞는 답변
      setTimeout(() => {
        const aiResponse: Message = {
          id: (Date.now() + 1).toString(),
          type: 'assistant',
          content: `**"마지막 페이지"**

작가 이소연은 30년간 써온 소설의 마지막 문장을 앞에 두고 멈춰 섰다. 손가락이 키보드 위에서 떨렸다.

"끝이야..." 그녀가 중얼거렸다.

그 순간, 컴퓨터 화면이 깜빡이더니 새로운 문장이 나타났다.

*"아니야, 이제 시작이야."*

이소연은 뒤를 돌아봤다. 아무도 없었다. 다시 화면을 보니 또 다른 문장이 타이핑되고 있었다.

*"30년 동안 당신이 써온 모든 이야기 속 주인공들이 기다리고 있어요. 우리를 현실로 불러내 주세요."*

이소연의 심장이 뛰기 시작했다. 그녀가 창조한 수백 명의 캐릭터들이... 살아있다는 것인가?

*"당신이 '끝'이라고 쓰는 순간, 우리는 사라져요. 하지만 당신이 우리를 믿는다면..."*

화면이 다시 깜빡였다. 이번엔 그녀의 첫 소설 주인공인 소년 준호가 화면 속에서 손을 흔들고 있었다.

이소연은 깨달았다. 진짜 창작은 이제부터 시작이라는 것을.

---

**반전 포인트**: 작가가 소설을 끝내려는 순간, 자신이 창조한 캐릭터들이 실제로 존재한다는 것을 발견하게 됩니다. 이는 '창작'이라는 주제를 메타적으로 다루면서, 창작자와 창작물 간의 경계를 흐리는 독창적인 설정입니다.

**감정적 요소**: 
- 30년간의 창작 여정에 대한 향수
- 끝에 대한 두려움과 새로운 시작에 대한 설렘
- 창작자로서의 책임감과 사명감

이런 식으로 메타픽션 기법을 활용하여 '소설 쓰기'라는 주제 자체를 소재로 한 이야기를 만들어보았습니다. 더 구체적인 장르나 설정이 있으시다면 그에 맞춰 조정할 수 있습니다!`,
          timestamp: new Date(),
        }

        setMessages(prev => [...prev, aiResponse])
        setIsTyping(false)
        setCurrentStep('completed')
      }, 3000)
    }
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  const resetDemo = () => {
    setUserInput('')
    setCurrentStep('input')
    setShowExtension(false)
    setShowResults(false)
    setIsSearching(false)
    setSearchQuery('')
    setMessages([])
    setIsTyping(false)
    setShowSuccessMessage(false)
  }

  const closeExtension = () => {
    setShowExtension(false)
    setCurrentStep('input')
    setShowResults(false)
    setIsSearching(false)
    setSearchQuery('')
  }

  const getStepMessage = () => {
    switch (currentStep) {
      case 'input':
        return "1단계: 아래 입력창에 '창의적인 소설 쓰기'라고 입력해보세요"
      case 'extension-click':
        return '2단계: 우상단의 PromptHub 아이콘을 클릭하세요'
      case 'searching':
        return '3단계: 관련 프롬프트를 검색하고 있습니다...'
      case 'results':
        return '4단계: 마음에 드는 프롬프트 리뷰를 클릭하세요'
      case 'applied':
        return '5단계: 엔터키나 화살표 버튼을 눌러 메시지를 전송하세요'
      case 'conversation':
        return '6단계: AI가 응답하고 있습니다...'
      case 'completed':
        return '체험 완료! 다시 해보기 버튼으로 처음부터 시작할 수 있습니다'
      default:
        return ''
    }
  }

  return (
    <div className="relative">
      {/* ChatGPT UI 복제 */}
      <Card className="overflow-hidden border-gray-700 bg-gray-900 text-white">
        <CardContent className="p-0">
          {/* 상단 헤더 */}
          <div
            ref={headerRef}
            id="chatgpt-demo-header"
            className="flex items-center justify-between border-b border-gray-700 p-3 sm:p-4">
            <div className="flex flex-shrink-0 items-center gap-2">
              {/* ChatGPT 아이콘 */}
              <div className="flex h-6 w-6 items-center justify-center rounded-full border border-gray-300 bg-white">
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 40 40"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  className="text-gray-900">
                  <path
                    d="m22.495 36.5c-1.1 0-2.145-.209-3.135-.627-.99-.418-1.87-1.001-2.64-1.749-.836.286-1.705.429-2.607.429-1.474 0-2.838-.363-4.092-1.089s-2.266-1.716-3.036-2.97c-.748-1.254-1.122-2.651-1.122-4.191 0-.638.088-1.331.264-2.079-.88-.814-1.562-1.749-2.046-2.805-.484-1.078-.726-2.2-.726-3.366 0-1.188.253-2.332.759-3.432s1.21-2.046 2.112-2.838c.924-.814 1.991-1.375 3.201-1.683.242-1.254.748-2.376 1.518-3.366.792-1.012 1.76-1.804 2.904-2.376 1.144-.572 2.365-.858 3.663-.858 1.1 0 2.145.209 3.135.627.99.418 1.87 1.001 2.64 1.749.836-.286 1.705-.429 2.607-.429 1.474 0 2.838.363 4.092 1.089 1.254.726 2.255 1.716 3.003 2.97.77 1.254 1.155 2.651 1.155 4.191 0 .638-.088 1.331-.264 2.079.88.814 1.562 1.76 2.046 2.838.484 1.056.726 2.167.726 3.333 0 1.188-.253 2.332-.759 3.432-.506 1.1-1.221 2.057-2.145 2.871-.902.792-1.958 1.342-3.168 1.65-.242 1.254-.759 2.376-1.551 3.366-.77 1.012-1.727 1.804-2.871 2.376-1.144.572-2.365.858-3.663.858zM14.344 32.375c1.1 0 2.057-.231 2.871-.693l6.204-3.564c.22-.154.33-.363.33-.627v-2.838L15.763 29.24c-.484.286-.968.286-1.452 0L8.074 25.643c0 .066-.011.143-.033.231 0 .088 0 .22 0 .396 0 1.122.264 2.156.792 3.102.55.924 1.309 1.65 2.277 2.178.968.55 2.046.825 3.234.825zm.33-5.379c.132.066.253.099.363.099s.22-.033.33-.099l2.475-1.419-7.953-4.62c-.484-.286-.726-.715-.726-1.287V12.509c-1.1.484-1.98 1.232-2.64 2.244-.66.99-.99 2.09-.99 3.3 0 1.078.275 2.112.825 3.102.55.99 1.265 1.738 2.145 2.244L14.674 26.996zm7.821 7.326c1.166 0 2.222-.264 3.168-.792s1.694-1.254 2.244-2.178c.55-.924.825-1.958.825-3.102v-7.128c0-.264-.11-.462-.33-.594l-2.508-1.452v9.207c0 .572-.242 1.001-.726 1.287l-6.237 3.597c1.078.77 2.266 1.155 3.564 1.155zm1.254-12.078V17.756L20.02 15.644 16.258 17.756v4.488l3.762 2.112 3.729-2.112zm-9.636-10.527c0-.572.242-1.001.726-1.287l6.237-3.597c-1.078-.77-2.266-1.155-3.564-1.155-1.166 0-2.222.264-3.168.792-.946.528-1.694 1.254-2.244 2.178-.528.924-.792 1.958-.792 3.102v7.095c0 .264.11.473.33.627l2.475 1.452V11.717zm16.764 15.774c1.1-.484 1.969-1.232 2.607-2.244.66-1.012.99-2.112.99-3.3 0-1.078-.275-2.112-.825-3.102-.55-.99-1.265-1.738-2.145-2.244l-6.171-3.564c-.132-.088-.253-.121-.363-.099-.11 0-.22.033-.33.099l-2.475 1.386 7.986 4.653c.242.132.418.308.528.528.132.198.198.44.198.726v7.161zm-6.633-16.764c.484-.308.968-.308 1.452 0l6.27 3.663c0-.154 0-.352 0-.594 0-1.056-.264-2.057-.792-3.003-.506-.968-1.243-1.738-2.211-2.31-.946-.572-2.046-.858-3.3-.858-1.1 0-2.057.231-2.871.693L16.588 11.882c-.22.154-.33.363-.33.627v2.838l7.986-4.62z"
                    fill="currentColor"
                  />
                </svg>
              </div>
              <span className="hidden font-medium text-white sm:inline">ChatGPT</span>
            </div>

            {/* 중앙 안내 메시지 */}
            <div className="flex min-w-0 flex-1 justify-center px-2 sm:px-4">
              <div className="inline-flex max-w-full items-center gap-2 rounded-full bg-blue-50 px-2 py-2 text-xs text-blue-700 sm:px-4 sm:text-sm">
                <div className="h-2 w-2 flex-shrink-0 animate-pulse rounded-full bg-blue-500"></div>
                <span className="min-w-0 truncate">
                  <span className="hidden sm:inline">{getStepMessage()}</span>
                  <span className="sm:hidden">
                    {currentStep === 'input' && "1단계: '창의적인 소설 쓰기'를 입력해보세요"}
                    {currentStep === 'extension-click' && '2단계: 아이콘 클릭'}
                    {currentStep === 'searching' && '3단계: 검색 중...'}
                    {currentStep === 'results' && '4단계: 프롬프트 선택'}
                    {currentStep === 'applied' && '5단계: 메시지 전송'}
                    {currentStep === 'conversation' && '6단계: AI 응답 중...'}
                    {currentStep === 'completed' && '체험 완료!'}
                  </span>
                </span>
              </div>
            </div>

            <div className="flex flex-shrink-0 items-center gap-3">
              {/* PromptHub Extension 아이콘 */}
              <div className="relative">
                <Button
                  variant="ghost"
                  size="sm"
                  className={`rounded-lg p-2 transition-all duration-300 ${
                    currentStep === 'extension-click' && userInput.trim()
                      ? 'animate-pulse bg-blue-600 shadow-lg shadow-blue-500/50'
                      : 'hover:bg-gray-700'
                  }`}
                  onClick={handleExtensionClick}>
                  <div className="flex h-5 w-5 items-center justify-center rounded bg-gradient-to-br from-blue-500 to-purple-600">
                    <Star className="h-3 w-3 text-white" />
                  </div>
                </Button>
                {currentStep === 'extension-click' && userInput.trim() && (
                  <div className="absolute -right-2 -top-2 h-3 w-3 animate-bounce rounded-full bg-red-500"></div>
                )}
              </div>
            </div>
          </div>

          {/* 메인 콘텐츠 영역 */}
          <div className="flex h-[550px] flex-col xl:h-[650px]">
            {messages.length === 0 ? (
              /* 초기 상태 */
              <div className="flex flex-1 flex-col items-center justify-end p-4 sm:p-6 lg:p-8">
                {/* 입력창 */}
                <div className="relative w-full max-w-sm sm:max-w-lg lg:max-w-3xl">
                  <div
                    className={`rounded-2xl bg-gray-700 p-4 transition-all duration-300 ${
                      currentStep === 'input'
                        ? 'shadow-lg shadow-blue-500/20 ring-2 ring-blue-500'
                        : ''
                    }`}>
                    {/* 텍스트 입력 영역 */}
                    <textarea
                      ref={textareaRef}
                      value={userInput}
                      onChange={e => handleInputChange(e.target.value)}
                      onKeyPress={handleKeyPress}
                      placeholder="무엇이든 물어보세요"
                      className="mb-3 w-full resize-none overflow-hidden bg-transparent leading-6 text-white placeholder-gray-400 outline-none"
                      style={{
                        height: '24px',
                        minHeight: '24px',
                        maxHeight: '200px',
                        scrollbarWidth: 'none',
                        msOverflowStyle: 'none',
                      }}
                      rows={1}
                    />

                    {/* 도구 영역 */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <Plus className="h-5 w-5 text-gray-400" />
                        <Paperclip className="h-5 w-5 text-gray-400" />
                        <span className="text-sm text-gray-400">도구</span>
                      </div>

                      <div className="flex items-center gap-2">
                        {userInput.trim() && currentStep === 'applied' ? (
                          <Button
                            size="sm"
                            className="h-8 w-8 rounded-full bg-white p-0 hover:bg-gray-200"
                            onClick={handleSendMessage}>
                            <ArrowUp className="h-4 w-4 text-gray-900" />
                          </Button>
                        ) : (
                          <>
                            <Mic className="h-5 w-5 text-gray-400" />
                            <AudioWaveform className="h-5 w-5 text-gray-400" />
                          </>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* 타이핑 인디케이터 */}
                  {userInput.trim() && currentStep !== 'applied' && (
                    <div className="absolute -top-8 left-4 text-xs text-gray-400">
                      <span className="inline-flex items-center gap-1">
                        <span>입력 중</span>
                        <span className="flex gap-1">
                          <span className="h-1 w-1 animate-pulse rounded-full bg-red-500"></span>
                          <span
                            className="h-1 w-1 animate-pulse rounded-full bg-red-500"
                            style={{ animationDelay: '0.2s' }}></span>
                          <span
                            className="h-1 w-1 animate-pulse rounded-full bg-red-500"
                            style={{ animationDelay: '0.4s' }}></span>
                        </span>
                      </span>
                    </div>
                  )}
                </div>
              </div>
            ) : (
              /* 대화 상태 - 스크롤바 숨김 */
              <div className="scrollbar-hide flex-1 space-y-4 overflow-y-auto p-3 sm:p-4">
                {messages.map(message => (
                  <div
                    key={message.id}
                    className={`flex ${message.type === 'user' ? 'justify-end' : 'justify-start'}`}>
                    <div
                      className={`max-w-xs sm:max-w-lg lg:max-w-3xl ${message.type === 'user' ? 'bg-gray-700 text-white' : 'bg-gray-800 text-gray-100'} rounded-2xl p-3 sm:p-4`}>
                      {message.type === 'assistant' && (
                        <div className="mb-2 flex items-center gap-2"></div>
                      )}
                      <div className="whitespace-pre-wrap">{message.content}</div>
                      {message.type === 'assistant' && (
                        <div className="mt-3 flex items-center gap-2 border-t border-gray-700 pt-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-1 hover:bg-gray-700">
                            <Copy className="h-4 w-4 text-gray-400" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-1 hover:bg-gray-700">
                            <ThumbsUp className="h-4 w-4 text-gray-400" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-1 hover:bg-gray-700">
                            <ThumbsDown className="h-4 w-4 text-gray-400" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-1 hover:bg-gray-700">
                            <Volume2 className="h-4 w-4 text-gray-400" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-1 hover:bg-gray-700">
                            <Edit className="h-4 w-4 text-gray-400" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-1 hover:bg-gray-700">
                            <Download className="h-4 w-4 text-gray-400" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-1 hover:bg-gray-700">
                            <RotateCcw className="h-4 w-4 text-gray-400" />
                          </Button>
                        </div>
                      )}
                    </div>
                  </div>
                ))}

                {/* 타이핑 인디케이터 */}
                {isTyping && (
                  <div className="flex justify-start">
                    <div className="rounded-2xl bg-gray-800 p-4">
                      <div className="flex gap-1">
                        <div className="h-2 w-2 animate-bounce rounded-full bg-gray-400"></div>
                        <div
                          className="h-2 w-2 animate-bounce rounded-full bg-gray-400"
                          style={{ animationDelay: '0.1s' }}></div>
                        <div
                          className="h-2 w-2 animate-bounce rounded-full bg-gray-400"
                          style={{ animationDelay: '0.2s' }}></div>
                      </div>
                    </div>
                  </div>
                )}

                {/* 다시 해보기 버튼을 대화 영역 안으로 이동 */}
                {currentStep === 'completed' && (
                  <div className="mt-4 flex justify-center">
                    <Button
                      onClick={resetDemo}
                      variant="ghost"
                      className="rounded-lg px-4 py-2 text-sm text-gray-400 hover:bg-gray-700 hover:text-white">
                      <RotateCcw className="mr-2 h-4 w-4" />
                      다시 해보기
                    </Button>
                  </div>
                )}

                <div ref={messagesEndRef} />
              </div>
            )}

            {/* 하단 입력창 (대화 중일 때) */}
            {messages.length > 0 && !isTyping && currentStep !== 'completed' && (
              <div className="border-t border-gray-700 p-3 sm:p-4">
                <div className="rounded-2xl bg-gray-700 p-4">
                  {/* 텍스트 입력 영역 */}
                  <textarea
                    ref={bottomTextareaRef}
                    value={userInput}
                    onChange={e => handleInputChange(e.target.value)}
                    onKeyPress={handleKeyPress}
                    placeholder="무엇이든 물어보세요"
                    className="mb-3 w-full resize-none overflow-hidden bg-transparent leading-6 text-white placeholder-gray-400 outline-none"
                    style={{
                      height: '24px',
                      minHeight: '24px',
                      maxHeight: '200px',
                      scrollbarWidth: 'none',
                      msOverflowStyle: 'none',
                    }}
                    rows={1}
                  />

                  {/* 도구 영역 */}
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <Plus className="h-5 w-5 text-gray-400" />
                      <span className="text-sm text-gray-400">도구</span>
                    </div>

                    <div className="flex items-center gap-2">
                      <Mic className="h-5 w-5 text-gray-400" />
                      <AudioWaveform className="h-5 w-5 text-gray-400" />
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* PromptHub Extension 팝업 - 스크롤바 숨김 */}
      {showExtension && (
        <div className="absolute right-2 top-16 z-50 w-72 rounded-lg border border-gray-200 bg-white shadow-2xl duration-300 animate-in slide-in-from-top-2 sm:right-4 sm:w-80 lg:w-96">
          <div className="border-b border-gray-200 p-4">
            <div className="mb-3 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="flex h-6 w-6 items-center justify-center rounded bg-gradient-to-br from-blue-500 to-purple-600">
                  <Star className="h-4 w-4 text-white" />
                </div>
                <span className="font-semibold text-gray-900">PromptHub</span>
              </div>
              <Button variant="ghost" size="sm" onClick={closeExtension}>
                <X className="h-4 w-4" />
              </Button>
            </div>

            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 transform text-gray-400" />
              <input
                type="text"
                value={searchQuery}
                readOnly
                className="w-full rounded-lg border border-gray-300 bg-gray-50 py-2 pl-10 pr-4 text-sm"
                placeholder="프롬프트 검색..."
              />
            </div>
          </div>

          <div className="scrollbar-hide max-h-60 overflow-y-auto p-4 sm:max-h-80 lg:max-h-96">
            {isSearching ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="mr-2 h-6 w-6 animate-spin text-blue-600" />
                <span className="text-gray-600">관련 프롬프트 검색 중...</span>
              </div>
            ) : showResults ? (
              <div className="space-y-3">
                <div className="mb-3 text-sm text-gray-600">
                  {sampleReviews.length}개의 관련 프롬프트를 찾았습니다
                </div>
                {sampleReviews.map(review => (
                  <div
                    key={review.id}
                    className="group cursor-pointer rounded-lg border border-gray-200 p-3 transition-all duration-200 hover:border-blue-300 hover:bg-blue-50"
                    onClick={() => handleReviewClick(review)}>
                    <div className="mb-2 flex items-start justify-between">
                      <h4 className="line-clamp-2 text-sm font-medium text-gray-900 group-hover:text-blue-700">
                        {review.title}
                      </h4>
                    </div>

                    <div className="flex items-center gap-4 text-xs text-gray-500">
                      <div className="flex items-center gap-1">
                        <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" />
                        <span>{review.rating}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <Heart className="h-3 w-3 text-red-500" />
                        <span>{review.likes}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <Eye className="h-3 w-3" />
                        <span>{review.views}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : null}
          </div>
        </div>
      )}

      {/* 성공 메시지 */}
      {showSuccessMessage && (
        <div
          className={`absolute left-1/2 top-1/3 -translate-x-1/2 -translate-y-1/2 transform rounded-lg bg-green-500 px-4 py-2 text-white shadow-lg transition-all duration-500 sm:px-6 sm:py-3 ${
            showSuccessMessage
              ? 'opacity-100 animate-in zoom-in-95'
              : 'opacity-0 animate-out fade-out-0'
          }`}>
          <div className="flex items-center gap-2">
            <div className="flex h-5 w-5 items-center justify-center rounded-full bg-white">
              <div className="h-2 w-2 rounded-full bg-green-500"></div>
            </div>
            <span className="text-sm font-medium sm:text-base">
              프롬프트가 성공적으로 적용되었습니다!
            </span>
          </div>
        </div>
      )}
    </div>
  )
}
