'use client'

import { useState, useEffect } from 'react'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { ChevronLeft, ChevronRight, Save, Plus, X } from 'lucide-react'
import { postsApi } from '@/lib/api'
import type {
  Platform,
  AiModel,
  Category,
  ModelSuggestion,
  PostCreateRequest,
} from '@/types/api'
import CustomButton from './custom-button'
import CustomBadge from './custom-badge'
import StarRating from './star-rating'
import ModelAutocomplete from './model-autocomplete'
import { useMetadataUtils } from '@/lib/utils'
import { logger } from '@/lib/logger'

interface CreatePostDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSuccess?: () => void
  editingReview?: PostCreateRequest | null
}

export function CreatePostDialog({
  open,
  onOpenChange,
  onSuccess,
  editingReview,
}: CreatePostDialogProps) {
  const { getCategoryId, setMetadata } = useMetadataUtils()
  // 전역 스타일 추가
  useEffect(() => {
    const style = document.createElement('style')
    style.innerHTML = `
      input:focus, textarea:focus, select:focus, button:focus {
        outline: none !important;
        box-shadow: none !important;
      }
    `
    document.head.appendChild(style)

    return () => {
      document.head.removeChild(style)
    }
  }, [])

  const [currentStep, setCurrentStep] = useState(1)

  // Step 1: 제목 + 프롬프트 (필수)
  const [title, setTitle] = useState('')
  const [prompt, setPrompt] = useState('')

  // Step 2: AI 응답 (필수)
  const [aiResponse, setAiResponse] = useState('')

  // Step 3: 평점 + 추가의견 (평점 필수, 추가의견 선택)
  const [rating, setRating] = useState(0)
  const [additionalOpinion, setAdditionalOpinion] = useState('')

  // Step 4: 모델 (필수)
  const [selectedModel, setSelectedModel] = useState('')
  const [customModel, setCustomModel] = useState('')
  const [showCustomModelInput, setShowCustomModelInput] = useState(false)
  const [selectedModelData, setSelectedModelData] = useState<AiModel | null>(null)
  const [modelDetail, setModelDetail] = useState('')
  const [customPlatformId, setCustomPlatformId] = useState<number | null>(null)

  // Step 5: 카테고리 + 태그 (카테고리 필수)
  const [selectedCategory, setSelectedCategory] = useState('')
  const [customCategory, setCustomCategory] = useState('')
  const [showCustomCategoryInput, setShowCustomCategoryInput] = useState(false)
  const [tags, setTags] = useState<string[]>([])
  const [newTag, setNewTag] = useState('')

  // API 데이터 상태
  const [platforms, setPlatforms] = useState<Platform[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [allModels, setAllModels] = useState<AiModel[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const totalSteps = 5

  // 컴포넌트 마운트 시 메타데이터 로드
  useEffect(() => {
    const loadMetadata = async () => {
      try {
        setLoading(true)
        setError('')

        // 실제 백엔드 API 사용
        const [platformsRes, categoriesRes, modelsRes] = await Promise.all([
          postsApi.getPlatforms(),
          postsApi.getCategories(),
          postsApi.getModels(),
        ])

        setPlatforms(platformsRes.data)
        setCategories(categoriesRes.data)
        setAllModels(modelsRes.data)

        // 메타데이터 유틸리티에 설정
        setMetadata(platformsRes.data, modelsRes.data, categoriesRes.data)
      } catch (err) {
        logger.error('메타데이터 로드 실패:', err)
        setError('데이터를 불러오는데 실패했습니다.')
      } finally {
        setLoading(false)
      }
    }

    if (open) {
      loadMetadata()
    }
  }, [open])

  // 입력값 변경: 선택된 기본 모델 정보는 유지하고 텍스트만 변경 (model_detail 용)
  const handleModelInputChange = (modelName: string) => {
    setSelectedModel(modelName)
    setShowCustomModelInput(false)
  }

  // 드롭다운에서 모델 선택: 기본 모델 메타 고정
  const handleModelSelect = (model: ModelSuggestion) => {
    const modelData = allModels.find(item => item.id === model.id)
    if (modelData?.name === '기타') {
      // "기타" 선택은 검색 선택 모드와 충돌하므로 커스텀 입력 모드로 강제 전환
      handleCustomModelToggle(true)
      setCustomPlatformId(modelData.platform)
      return
    }

    setSelectedModel(model.name)
    setShowCustomModelInput(false)
    setCustomModel('')
    setSelectedModelData(modelData || null)
    if (modelData && modelData.name !== '기타') {
      setModelDetail(modelData.name)
    } else {
      setModelDetail('')
    }
  }

  // 커스텀 모델 입력 모드 토글
  const handleCustomModelToggle = (isCustom: boolean) => {
    if (isCustom) {
      setShowCustomModelInput(true)
      setSelectedModel('')
      setSelectedModelData(null)
      setModelDetail('')
      // 기본 플랫폼: id=1 우선, 없으면 id 오름차순 첫 번째
      const sortedPlatforms = platforms.slice().sort((a, b) => a.id - b.id)
      const defaultPlat = platforms.find(p => p.id === 1) || sortedPlatforms[0]
      setCustomPlatformId(defaultPlat ? defaultPlat.id : null)
    } else {
      setShowCustomModelInput(false)
      setCustomModel('')
      setCustomPlatformId(null)
    }
  }

  // 카테고리 변경 시 직접 입력 필드 표시 여부 결정
  useEffect(() => {
    if (selectedCategory === '기타') {
      setShowCustomCategoryInput(true)
    } else {
      setShowCustomCategoryInput(false)
    }
  }, [selectedCategory])

  const preloadedModelSuggestions: ModelSuggestion[] = allModels
    .filter(model => model.name !== '기타')
    .map(model => ({
      id: model.id,
      name: model.name,
      platform: {
        id: model.platform,
        name: platforms.find(p => p.id === model.platform)?.name || model.platformName || 'Unknown',
      },
    }))

  // 유효성 검사
  const validateStep = (step: number) => {
    switch (step) {
      case 1:
        // 백엔드/모델 검증과 동일: 제목 최소 5자, 프롬프트 최소 10자
        return title.trim().length >= 5 && prompt.trim().length >= 10
      case 2:
        // 백엔드 검증과 동일: AI 응답 최소 10자
        return aiResponse.trim().length >= 10
      case 3:
        return rating > 0
      case 4:
        return showCustomModelInput
          ? customModel.trim() !== '' && customPlatformId !== null
          : selectedModelData !== null
      case 5:
        return showCustomCategoryInput ? customCategory.trim() !== '' : selectedCategory !== ''
      default:
        return true
    }
  }

  const handleNext = () => {
    if (validateStep(currentStep) && currentStep < totalSteps) {
      setCurrentStep(currentStep + 1)
    }
  }

  const handlePrevious = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1)
    }
  }

  const handleSave = async () => {
    if (!validateStep(currentStep)) return

    try {
      setLoading(true)
      setError('')

      const categoryId = getCategoryId(selectedCategory)
      if (!categoryId) {
        throw new Error('카테고리 정보가 없습니다.')
      }

      let platformId: number
      let modelId: number | undefined
      let modelEtcToSave: string | undefined
      let modelDetailToSave: string | undefined

      if (showCustomModelInput) {
        // 커스텀 모델 입력 모드
        const trimmedCustomModel = customModel.trim()
        if (!trimmedCustomModel) {
          throw new Error('모델명을 입력해주세요.')
        }

        if (!customPlatformId) {
          throw new Error('플랫폼을 선택해주세요.')
        }
        platformId = customPlatformId
        modelId = undefined
        modelEtcToSave = trimmedCustomModel
        modelDetailToSave = undefined
      } else {
        // 검색된 모델 선택 모드
        if (!selectedModelData) {
          throw new Error('선택된 모델 정보를 찾을 수 없습니다.')
        }

        platformId = selectedModelData.platform
        modelId = selectedModelData.id
        modelEtcToSave = undefined
        const baseName = selectedModelData.name
        const trimmedDetail = modelDetail.trim()
        modelDetailToSave =
          baseName !== '기타' && trimmedDetail && trimmedDetail !== baseName
            ? trimmedDetail
            : undefined
      }

      const postData: PostCreateRequest = {
        title: title.trim(),
        platform: platformId,
        model: modelId,
        model_etc: modelEtcToSave,
        // @ts-ignore: 백엔드에서 수용
        model_detail: modelDetailToSave,
        category: categoryId,
        category_etc: showCustomCategoryInput ? customCategory.trim() : undefined,
        tags: tags,
        satisfaction: rating,
        prompt: prompt.trim(),
        ai_response: aiResponse.trim(),
        additional_opinion: additionalOpinion.trim() || undefined,
      }

      logger.debug('저장할 데이터:', postData)

      // 실제 백엔드 API 사용
      await postsApi.createPost(postData)

      // 성공시 폼 리셋
      setCurrentStep(1)
      setTitle('')
      setPrompt('')
      setAiResponse('')
      setRating(0)
      setAdditionalOpinion('')
      setSelectedModel('')
      setCustomModel('')
      setSelectedModelData(null)
      setSelectedCategory('')
      setCustomCategory('')
      setTags([])
      setNewTag('')
      setShowCustomModelInput(false)
      setShowCustomCategoryInput(false)

      onOpenChange(false)
      if (onSuccess) onSuccess()
    } catch (err) {
      logger.error('게시글 저장 실패:', err)
      const errorData = (err as any)?.response?.data ?? (err as any)?.errors ?? null
      const fieldErrors = errorData?.errors
      if (fieldErrors && typeof fieldErrors === 'object') {
        const firstFieldError = Object.values(fieldErrors)[0]
        const firstMessage = Array.isArray(firstFieldError)
          ? firstFieldError[0]
          : typeof firstFieldError === 'string'
            ? firstFieldError
            : null
        setError(firstMessage || errorData?.message || '입력값을 다시 확인해주세요.')
      } else {
        setError(
          errorData?.message ||
            (typeof (err as any)?.message === 'string' ? (err as any).message : null) ||
            '게시글 저장에 실패했습니다.',
        )
      }
    } finally {
      setLoading(false)
    }
  }

  const renderStep = () => {
    switch (currentStep) {
      case 1:
        return (
          <div className="space-y-6">
            <div className="text-center">
              <h2 className="mb-2 font-bold text-gray-900">제목과 프롬프트를 입력하세요</h2>
              <p className="text-gray-600">프롬프트의 제목과 전체 내용을 입력해주세요</p>
            </div>

            {/* 제목 입력 */}
            <div>
              <h3 className="mb-3 block text-gray-700">
                제목 <span className="text-red-500">*</span>
              </h3>
              <div className="rounded-xl border border-gray-200 bg-white transition-all focus-within:border-blue-300">
                <Input
                  value={title}
                  onChange={e => setTitle(e.target.value)}
                  className="w-full border-none bg-transparent text-xs text-gray-600 placeholder:text-gray-400 sm:text-base"
                  placeholder="예: AI 코딩 어시스턴트 프롬프트"
                />
              </div>
            </div>

            {/* 프롬프트 입력 */}
            <div>
              <h3 className="mb-3 block text-gray-700">
                프롬프트 전문 <span className="text-red-500">*</span>
              </h3>
              <div className="overflow-hidden rounded-xl border border-gray-200 bg-white transition-all focus-within:border-blue-300">
                <Textarea
                  value={prompt}
                  onChange={e => setPrompt(e.target.value)}
                  className="scrollbar-force w-full resize-none overflow-y-auto border-none bg-transparent text-xs text-gray-600 placeholder:text-gray-400 sm:text-base"
                  rows={10}
                  placeholder="AI에게 입력한 프롬프트의 전체 내용을 입력하세요..."
                />
              </div>
            </div>
          </div>
        )

      case 2:
        return (
          <div className="space-y-6">
            <div className="text-center">
              <h2 className="mb-2 font-bold text-gray-900">AI 응답을 입력하세요</h2>
              <p className="text-gray-600">AI가 생성한 응답 내용을 입력해주세요</p>
            </div>

            <div>
              <h3 className="mb-3 block text-gray-700">
                AI 응답 <span className="text-red-500">*</span>
              </h3>
              <div className="overflow-hidden rounded-xl border border-gray-200 bg-white transition-all focus-within:border-blue-300">
                <Textarea
                  value={aiResponse}
                  onChange={e => setAiResponse(e.target.value)}
                  className="scrollbar-force w-full resize-none overflow-y-auto border-none bg-transparent text-xs text-gray-600 placeholder:text-gray-400 sm:text-base"
                  rows={10}
                  placeholder="AI가 생성한 응답의 전체 내용을 입력하세요..."
                />
              </div>
            </div>
          </div>
        )

      case 3:
        return (
          <div className="space-y-6">
            <div className="text-center">
              <h2 className="mb-2 font-bold text-gray-900">만족도를 평가해주세요</h2>
              <p className="text-gray-600">AI 응답에 대한 만족도와 추가 의견을 입력해주세요</p>
            </div>

            {/* 평점 */}
            <div>
              <h3 className="mb-3 block text-gray-700">
                평점 <span className="text-red-500">*</span>
              </h3>
              <div className="rounded-xl border border-gray-200 bg-white p-6">
                <div className="flex flex-col items-center gap-4">
                  <p className="text-sm text-gray-600">별점을 선택해주세요</p>
                  <StarRating
                    rating={rating}
                    onRatingChange={setRating}
                    showValue={true}
                    size="lg"
                  />
                </div>
              </div>
            </div>

            {/* 추가 의견 */}
            <div>
              <h3 className="mb-3 block text-gray-700">추가 의견 (선택사항)</h3>
              <div className="overflow-hidden rounded-xl border border-gray-200 bg-white transition-all focus-within:border-blue-300">
                <Textarea
                  value={additionalOpinion}
                  onChange={e => setAdditionalOpinion(e.target.value)}
                  className="scrollbar-force w-full resize-none overflow-y-auto border-none bg-transparent text-xs text-gray-600 placeholder:text-gray-400 sm:text-base"
                  rows={4}
                  placeholder="AI 응답에 대한 추가 의견이나 개선점을 입력하세요..."
                />
              </div>
            </div>
          </div>
        )

      case 4:
        return (
          <div className="space-y-4">
            <div className="text-center">
              <h2 className="mb-1 font-bold text-gray-900">사용한 AI 모델을 알려주세요</h2>
              <p className="text-sm text-gray-600">
                모델명을 검색해서 선택하거나 직접 입력할 수 있습니다
              </p>
            </div>

            {!showCustomModelInput ? (
              <div>
                <h3 className="mb-3 block text-gray-700">
                  AI 모델 입력 <span className="text-red-500">*</span>
                </h3>
                <ModelAutocomplete
                  value={selectedModel}
                  onChange={handleModelInputChange}
                  onModelSelect={handleModelSelect}
                  onCustomModelToggle={handleCustomModelToggle}
                  preloadedSuggestions={preloadedModelSuggestions}
                  placeholder="사용한 플랫폼 또는 AI 모델을 검색하세요 (예: OpenAI, GPT-5, Claude, Grok...)"
                  showCustomOption={true}
                  className="w-full"
                />

                {selectedModelData && (
                  <div className="mt-2 rounded-lg border border-green-200 bg-green-50 p-2">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-medium text-green-800">
                          {selectedModelData.name}
                        </p>
                        <p className="text-xs text-green-600">
                          {platforms.find(p => p.id === selectedModelData.platform)?.name} 플랫폼
                        </p>
                      </div>
                      <button
                        onClick={() => {
                          setSelectedModel('')
                          setSelectedModelData(null)
                        }}
                        className="text-green-600 hover:text-green-800">
                        <X className="h-3 w-3" />
                      </button>
                    </div>
                  </div>
                )}

                {selectedModelData && selectedModelData.name !== '기타' && (
                  <div className="mt-3">
                    <h3 className="mb-2 block text-gray-700">상세 모델명</h3>
                    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white transition-all focus-within:border-blue-300">
                      <Input
                        value={modelDetail}
                        onChange={e => setModelDetail(e.target.value)}
                        className="w-full border-none bg-transparent text-xs text-gray-600 placeholder:text-gray-400 sm:text-base"
                        placeholder={`${selectedModelData.name}-high, ${selectedModelData.name}-fast 등 변형명을 입력하세요`}
                      />
                    </div>
                    <p className="mt-1 text-xs text-gray-500">
                      기본 모델 선택 후 필요 시 상세 모델명을 자유롭게 입력하세요.
                    </p>
                  </div>
                )}
              </div>
            ) : (
              <div>
                <div className="mb-3 flex items-center justify-end">
                  <button
                    onClick={() => handleCustomModelToggle(false)}
                    className="text-sm text-blue-600 hover:text-blue-800">
                    ← 검색으로 돌아가기
                  </button>
                </div>

                {/* 플랫폼 선택 드롭다운 */}
                <div className="mb-3">
                  <h3 className="mb-2 block text-gray-700">
                    플랫폼 선택 <span className="text-red-500">*</span>
                  </h3>
                  <Select
                    value={customPlatformId !== null ? String(customPlatformId) : ''}
                    onValueChange={val => setCustomPlatformId(parseInt(val))}>
                    <SelectTrigger className="w-full rounded-xl border-gray-200 bg-white focus:border-blue-300">
                      <SelectValue placeholder="플랫폼을 선택하세요" />
                    </SelectTrigger>
                    <SelectContent>
                      {platforms
                        .slice()
                        .sort((a, b) => a.id - b.id)
                        .map(p => (
                          <SelectItem key={p.id} value={String(p.id)}>
                            {p.name}
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="mb-3">
                  <h3 className="mb-2 block text-gray-700">
                    AI 모델명 입력 <span className="text-red-500">*</span>
                  </h3>
                  <div className="rounded-xl border border-gray-200 bg-white transition-all focus-within:border-blue-300">
                    <Input
                      value={customModel}
                      onChange={e => setCustomModel(e.target.value)}
                      className="w-full border-none bg-transparent text-xs text-gray-600 placeholder:text-gray-400 sm:text-base"
                      placeholder="사용한 모델명을 정확히 입력하세요 (예: GPT-3 Turbo, o3-high 등)"
                    />
                  </div>
                </div>
              </div>
            )}
          </div>
        )

      case 5:
        return (
          <div className="space-y-6">
            <div className="text-center">
              <h2 className="mb-2 font-bold text-gray-900">카테고리와 태그를 설정하세요</h2>
              <p className="text-gray-600">
                프롬프트의 카테고리를 선택하고 관련 태그를 추가해주세요
              </p>
            </div>

            {/* 카테고리 선택 */}
            <div>
              <h3 className="mb-3 block text-gray-700">
                카테고리 <span className="text-red-500">*</span>
              </h3>
              <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                <SelectTrigger className="w-full rounded-xl border-gray-200 bg-white focus:border-blue-300">
                  <SelectValue placeholder="카테고리를 선택하세요" />
                </SelectTrigger>
                <SelectContent>
                  {categories
                    .slice()
                    .sort((a, b) => a.id - b.id)
                    .map(category => (
                      <SelectItem key={category.id} value={category.name}>
                        {category.name}
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </div>

            {/* 직접 입력 필드 */}
            {showCustomCategoryInput && (
              <div>
                <h3 className="mb-3 block text-gray-700">카테고리 직접 입력</h3>
                <div className="rounded-xl border border-gray-200 bg-white transition-all focus-within:border-blue-300">
                  <Input
                    value={customCategory}
                    onChange={e => setCustomCategory(e.target.value)}
                    className="w-full border-none bg-transparent text-xs text-gray-600 placeholder:text-gray-400 sm:text-base"
                    placeholder="입력하지 않을 시 '기타'로 설정됩니다"
                  />
                </div>
              </div>
            )}

            {/* 태그 */}
            <div>
              <h3 className="mb-3 block text-gray-700">태그 (선택사항)</h3>

              {/* 추가된 태그 표시 */}
              {tags.length > 0 && (
                <div className="mb-4 flex flex-wrap gap-2">
                  {tags.map((tag, index) => (
                    <CustomBadge
                      key={index}
                      variant="green"
                      size="xs"
                      removable
                      onRemove={() => setTags(tags.filter((_, i) => i !== index))}>
                      {tag}
                    </CustomBadge>
                  ))}
                </div>
              )}

              {/* 새 태그 추가 */}
              <div className="flex gap-2">
                <div className="flex-1 rounded-xl border border-gray-200 bg-white transition-all focus-within:border-blue-300">
                  <Input
                    value={newTag}
                    onChange={e => setNewTag(e.target.value)}
                    onKeyPress={e => {
                      if (e.key === 'Enter' && newTag.trim()) {
                        e.preventDefault()
                        if (!tags.includes(newTag.trim())) {
                          setTags([...tags, newTag.trim()])
                        }
                        setNewTag('')
                      }
                    }}
                    className="w-full border-none bg-transparent text-xs text-gray-600 placeholder:text-gray-400 sm:text-base"
                    placeholder="새 태그를 입력하세요..."
                  />
                </div>
                <CustomButton
                  color="gradient"
                  border="none"
                  shape="square"
                  size="responsive"
                  icon={<Plus className="h-4 w-4" />}
                  onClick={() => {
                    if (newTag.trim() && !tags.includes(newTag.trim())) {
                      setTags([...tags, newTag.trim()])
                      setNewTag('')
                    }
                  }}>
                  추가
                </CustomButton>
              </div>

              {/* 추천 태그 */}
              <div className="mt-4">
                <p className="mb-2 text-xs text-gray-500">추천 태그</p>
                <div className="flex flex-wrap gap-2">
                  {['GPT-5', 'Claude', 'Gemini', '창작', '분석', '코딩', '번역'].map(
                    suggestedTag => (
                      <CustomBadge
                        key={suggestedTag}
                        variant="gray"
                        size="responsive"
                        icon={<Plus className="h-3 w-3" />}
                        onClick={() => {
                          if (!tags.includes(suggestedTag)) {
                            setTags([...tags, suggestedTag])
                          }
                        }}>
                        {suggestedTag}
                      </CustomBadge>
                    ),
                  )}
                </div>
              </div>
            </div>
          </div>
        )

      default:
        return null
    }
  }

  // 단계별 다이얼로그 높이 계산
  const getDialogHeight = () => {
    switch (currentStep) {
      case 1: // 프롬프트 입력 단계 - 긴 텍스트 영역
        return 'max-h-[92vh]'
      case 2: // AI 응답 입력 단계 - 긴 텍스트 영역
        return 'max-h-[92vh]'
      case 4: // 모델 검색 단계 - 다른 단계와 동일
        return 'max-h-[88vh]'
      default:
        return 'max-h-[88vh]'
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className={`${getDialogHeight()} max-w-2xl overflow-y-auto`}>
        <DialogHeader>
          <DialogTitle className="font-bold text-gray-900">
            {editingReview ? '리뷰 수정' : '새 리뷰 작성'}
          </DialogTitle>
          <DialogDescription className="sr-only">
            {editingReview
              ? '리뷰 수정 다이얼로그입니다. 단계별 입력값을 수정한 뒤 저장할 수 있습니다.'
              : '새 리뷰 작성 다이얼로그입니다. 단계별 입력값을 작성한 뒤 저장할 수 있습니다.'}
          </DialogDescription>
          <div className="space-y-2">
            <div className="flex items-center justify-between text-sm text-gray-600">
              <span>
                단계 {currentStep} / {totalSteps}
              </span>
              <span>{Math.round((currentStep / totalSteps) * 100)}% 완료</span>
            </div>
            <div className="h-2 w-full rounded-full bg-gray-200">
              <div
                className="h-2 rounded-full bg-gradient-to-r from-blue-500 to-purple-600 transition-all duration-300"
                style={{ width: `${(currentStep / totalSteps) * 100}%` }}></div>
            </div>
          </div>
        </DialogHeader>

        <div className={`${currentStep === 4 ? 'py-4' : 'py-6'}`}>
          {error && <div className="mb-4 rounded-lg bg-red-50 p-3 text-red-700">{error}</div>}
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="text-gray-500">로딩 중...</div>
            </div>
          ) : (
            renderStep()
          )}
        </div>

        <div
          className={`flex justify-between border-t border-gray-100 ${currentStep === 4 ? 'pt-3' : 'pt-4'}`}>
          {currentStep > 1 && (
            <CustomButton
              color="flat"
              border="none"
              shape="rounded"
              size="responsive"
              icon={<ChevronLeft className="h-4 w-4" />}
              onClick={handlePrevious}>
              이전
            </CustomButton>
          )}

          <div className={`flex gap-3 ${currentStep === 1 ? 'ml-auto' : ''}`}>
            {currentStep < totalSteps ? (
              <CustomButton
                color="gradient"
                border="none"
                shape="rounded"
                size="responsive"
                icon={<ChevronRight className="h-4 w-4" />}
                iconPosition="right"
                onClick={handleNext}
                disabled={!validateStep(currentStep)}>
                다음
              </CustomButton>
            ) : (
              <CustomButton
                color="gradient"
                border="none"
                shape="rounded"
                size="responsive"
                icon={<Save className="h-4 w-4" />}
                onClick={handleSave}
                disabled={!validateStep(currentStep) || loading}>
                {loading ? '저장 중...' : editingReview ? '수정 완료' : '리뷰 작성 완료'}
              </CustomButton>
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
