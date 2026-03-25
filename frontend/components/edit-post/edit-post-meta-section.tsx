/**
 * EditPostMetaSection 컴포넌트
 *
 * 게시글 메타 정보를 수정하는 섹션
 * 플랫폼, 모델, 카테고리, 태그, 만족도, 미리보기 뱃지로 구성
 * content-section 상단에 위치
 */

'use client'

import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { ChevronDown, Plus } from 'lucide-react'
// 실제 API 데이터 사용으로 변경
import CustomButton from '@/components/common/custom-button'
import CustomBadge from '@/components/common/custom-badge'
import StarRating from '@/components/common/star-rating'
import { useMetadataUtils } from '@/lib/utils'

// API 데이터 타입 정의
interface Platform {
  id: number
  name: string
}

interface AiModelOption {
  id: number
  name: string
  platform: number
  platform_name: string
}

interface Category {
  id: number
  name: string
}

interface EditPostMetaSectionProps {
  // PostEdit 타입과 일관성 유지
  satisfaction: number
  platform: string
  model: string
  modelEtc: string
  // 기본 모델의 상세 변형명 (예: GPT-5-high-fast)
  modelDetail?: string
  category: string
  categoryEtc: string
  tags: string[]
  tagInput: string
  activeSection: string | null
  showModelEtcInput: boolean
  showCategoryEtcInput: boolean

  // API 데이터
  platformsData: Platform[]
  modelsData: AiModelOption[]
  categoriesData: Category[]

  // 이벤트 핸들러들
  onSatisfactionChange: (rating: number) => void
  onPlatformChange: (platform: string) => void
  onModelChange: (model: string) => void
  onModelEtcValueChange: (model: string) => void
  onModelDetailValueChange?: (detail: string) => void
  onCategoryChange: (category: string) => void
  onCategoryEtcValueChange: (category: string) => void
  onTagsChange: (tags: string[]) => void
  onTagInputChange: (tag: string) => void
  onActiveSectionChange: (section: string | null) => void
  onUserInteraction: (isUserChange: boolean) => void
}

export default function EditPostMetaSection({
  satisfaction,
  platform,
  model,
  modelEtc,
  modelDetail,
  category,
  categoryEtc,
  tags,
  tagInput,
  activeSection,
  showModelEtcInput,
  showCategoryEtcInput,
  platformsData,
  modelsData,
  categoriesData,
  onSatisfactionChange,
  onPlatformChange,
  onModelChange,
  onModelEtcValueChange,
  onModelDetailValueChange,
  onCategoryChange,
  onCategoryEtcValueChange,
  onTagsChange,
  onTagInputChange,
  onActiveSectionChange,
  onUserInteraction,
}: EditPostMetaSectionProps) {
  const {
    getModelId,
    getCategoryId,
    getModelDisplayNameFromBackend,
    getCategoryDisplayNameFromBackend,
  } = useMetadataUtils()

  // API 데이터에서 플랫폼과 카테고리 목록 가져오기
  const platforms = platformsData || []
  const categories = categoriesData || []

  // 섹션 토글 함수 - 한 번에 하나만 펼칠 수 있도록
  const toggleSection = (section: string) => {
    onActiveSectionChange(activeSection === section ? null : section)
  }

  // 모델 선택 UI - 플랫폼, 모델 드롭다운 + 상세/기타 입력 사용
  const renderPlatformSection = () => {
    const sortedPlatforms = platforms.slice().sort((a, b) => a.id - b.id)
    const currentPlatform = platforms.find(p => p.name === platform) || sortedPlatforms[0]
    // DB에서 정렬된 순서를 그대로 사용 (sort_order → name)
    const modelsOfPlatform = currentPlatform
      ? modelsData.filter(m => Number(m.platform) === Number(currentPlatform.id))
      : []

    // '기타' 옵션 보장
    const hasOther = modelsOfPlatform.some(m => m.name === '기타')
    const finalModels = hasOther
      ? modelsOfPlatform
      : [
          ...modelsOfPlatform,
          {
            id: -1,
            name: '기타',
            platform: Number(currentPlatform?.id || -1),
            platform_name: platform,
          },
        ]

    const handlePlatformSelect = (value: string) => {
      const selectedPlat = platforms.find(p => p.name === value)
      if (!selectedPlat) return
      onUserInteraction(true)
      onPlatformChange(selectedPlat.name)
      // 플랫폼 변경 시 모델/기타/상세 초기화
      onModelChange('')
      onModelEtcValueChange('')
      onModelDetailValueChange && onModelDetailValueChange('')
    }

    const handleModelSelect = (value: string) => {
      onUserInteraction(true)
      onModelChange(value)
      // 모델이 '기타'가 아니면 etc 초기화, 상세는 비움(사용자가 입력)
      if (value !== '기타') {
        onModelEtcValueChange('')
      }
      onModelDetailValueChange && onModelDetailValueChange('')
    }

    return (
      <div className="animate-fadeIn space-y-4">
        {/* 플랫폼 드롭다운 */}
        <div>
          <p className="mb-2 block text-sm font-bold text-gray-700">
            플랫폼 <span className="text-red-500">*</span>
          </p>
          <Select value={currentPlatform?.name || ''} onValueChange={handlePlatformSelect}>
            <SelectTrigger className="w-full rounded-xl border-gray-200 bg-gray-50 text-xs sm:text-sm">
              <SelectValue placeholder="플랫폼을 선택하세요" />
            </SelectTrigger>
            <SelectContent className="text-xs sm:text-sm">
              {sortedPlatforms.map(p => (
                <SelectItem key={p.id} value={p.name} className="text-xs sm:text-sm">
                  {p.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* 모델 드롭다운 */}
        <div>
          <p className="mb-2 block text-sm font-bold text-gray-700">
            AI 모델 <span className="text-red-500">*</span>
          </p>
          <Select value={model || ''} onValueChange={handleModelSelect}>
            <SelectTrigger className="w-full rounded-xl border-gray-200 bg-gray-50 text-xs sm:text-sm">
              <SelectValue placeholder="AI 모델을 선택하세요" />
            </SelectTrigger>
            <SelectContent className="text-xs sm:text-sm">
              {finalModels.map(m => (
                <SelectItem key={m.id} value={m.name} className="text-xs sm:text-sm">
                  {m.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* 상세 모델명 입력 (선택) - 모델이 '기타'가 아닐 때 */}
        {model && model !== '기타' && (
          <div>
            <p className="mb-2 block text-sm font-bold text-gray-700">상세 모델명</p>
            <Input
              value={modelDetail || ''}
              onChange={e => onModelDetailValueChange && onModelDetailValueChange(e.target.value)}
              className="w-full rounded-xl border-gray-200 bg-gray-50 text-xs sm:text-sm"
              placeholder={'상세 모델 변형명을 입력하세요 (예: high, mini, heavy, v2, 20B)'}
            />
          </div>
        )}

        {/* 기타 모델명 입력 (선택) - 모델이 '기타'일 때 */}
        {model === '기타' && (
          <div>
            <p className="mb-2 block text-sm font-bold text-gray-700">
              기타 모델명 <span className="text-red-500">*</span>
            </p>
            <Input
              value={modelEtc}
              onChange={e => onModelEtcValueChange(e.target.value)}
              className="w-full rounded-xl border-gray-200 bg-gray-50 text-xs sm:text-sm"
              placeholder="사용한 모델명을 정확히 입력하세요"
            />
          </div>
        )}
      </div>
    )
  }

  // 카테고리 UI
  const renderCategorySection = () => {
    return (
      <div className="animate-fadeIn space-y-4">
        <div>
          <Select value={category} onValueChange={onCategoryChange}>
            <SelectTrigger className="w-full rounded-xl border-gray-200 bg-gray-50 text-xs sm:text-sm">
              <SelectValue placeholder="카테고리를 선택하세요" />
            </SelectTrigger>
            <SelectContent className="text-xs sm:text-sm">
              {categories
                .slice()
                .sort((a, b) => a.id - b.id)
                .map(categoryData => (
                  <SelectItem
                    key={categoryData.id}
                    value={categoryData.name}
                    className="text-xs sm:text-sm">
                    {categoryData.name}
                  </SelectItem>
                ))}
            </SelectContent>
          </Select>
        </div>

        {/* 직접 입력 필드 */}
        {showCategoryEtcInput && (
          <div className="animate-fadeIn">
            <Input
              value={categoryEtc}
              onChange={e => onCategoryEtcValueChange(e.target.value)}
              className="w-full rounded-xl border-gray-200 bg-gray-50 text-xs sm:text-sm"
              placeholder="카테고리를 입력하세요 (선택사항)"
            />
          </div>
        )}
      </div>
    )
  }

  // 태그 UI
  const renderTagSection = () => {
    return (
      <div className="animate-fadeIn space-y-4">
        {/* 기존 태그 표시 */}
        <div className="flex flex-wrap gap-2">
          {tags.map((tag, index) => (
            <CustomBadge
              key={index}
              variant="green"
              size="responsive"
              removable
              onRemove={() => onTagsChange(tags.filter((_, i) => i !== index))}>
              {tag}
            </CustomBadge>
          ))}
        </div>

        {/* 새 태그 추가 */}
        <div className="flex gap-2">
          <Input
            value={tagInput}
            onChange={e => onTagInputChange(e.target.value)}
            onKeyDown={e => {
              if (e.key === 'Enter' && tagInput.trim()) {
                e.preventDefault()
                if (!tags.includes(tagInput.trim())) {
                  onTagsChange([...tags, tagInput.trim()])
                }
                onTagInputChange('')
              }
            }}
            className="flex-1 rounded-xl border-gray-200 bg-white text-xs focus:border-gray-400 focus:outline-none focus:ring-0 sm:text-sm"
            placeholder="새 태그를 입력하세요..."
          />
          <CustomButton
            color="gradient"
            border="none"
            shape="square"
            size="responsive"
            icon={<Plus className="h-4 w-4" />}
            onClick={() => {
              if (tagInput.trim() && !tags.includes(tagInput.trim())) {
                onTagsChange([...tags, tagInput.trim()])
                onTagInputChange('')
              }
            }}>
            <span className="ml-2 hidden sm:inline">추가</span>
          </CustomButton>
        </div>

        {/* 추천 태그 */}
        <div className="space-y-2">
          <p className="text-gray-500">추천 태그</p>
          <div className="flex flex-wrap gap-2">
            {['ChatGPT', 'Claude', 'Grok', '창작', '분석', '코딩', '번역'].map(suggestedTag => (
              <CustomBadge
                key={suggestedTag}
                variant="gray"
                size="responsive"
                icon={<Plus className="mr-1 h-3 w-3" />}
                onClick={() => {
                  if (!tags.includes(suggestedTag)) {
                    onTagsChange([...tags, suggestedTag])
                  }
                }}>
                {suggestedTag}
              </CustomBadge>
            ))}
          </div>
        </div>
      </div>
    )
  }

  // 접기/펼치기 버튼 스타일 통일
  const renderCollapsibleHeader = (title: string, section: string, icon = null) => {
    const isExpanded = activeSection === section

    // 섹션별 색상 정의
    const sectionColors = {
      category: {
        bg: 'bg-gray-50',
        hoverBg: 'hover:bg-gray-100',
        expandedBg: 'bg-gray-100',
        expandedBorder: 'border-gray-300',
        expandedText: 'text-gray-600',
      },
      platform: {
        bg: 'bg-blue-50',
        hoverBg: 'hover:bg-blue-100',
        expandedBg: 'bg-blue-100',
        expandedBorder: 'border-blue-300',
        expandedText: 'text-blue-600',
      },
      rating: {
        bg: 'bg-yellow-50',
        hoverBg: 'hover:bg-yellow-100',
        expandedBg: 'bg-yellow-100',
        expandedBorder: 'border-yellow-300',
        expandedText: 'text-yellow-600',
      },
      tag: {
        bg: 'bg-green-50',
        hoverBg: 'hover:bg-green-100',
        expandedBg: 'bg-green-100',
        expandedBorder: 'border-green-300',
        expandedText: 'text-green-600',
      },
    }

    const colors = sectionColors[section as keyof typeof sectionColors]

    return (
      <button
        onClick={() => toggleSection(section)}
        className={`flex w-full items-center justify-between p-3 ${colors.bg} ${colors.hoverBg} group rounded-xl transition-all duration-200 ${
          isExpanded
            ? `${colors.expandedBg} ${colors.expandedBorder} border-2`
            : 'border border-transparent'
        }`}>
        <div className="flex items-center gap-2">
          {icon && (
            <div
              className={`text-gray-400 transition-colors group-hover:text-gray-600 ${isExpanded ? colors.expandedText : ''}`}>
              {icon}
            </div>
          )}
          <span
            className={`text-xs font-medium text-gray-700 transition-colors group-hover:text-gray-900 sm:text-sm ${isExpanded ? colors.expandedText : ''}`}>
            {title}
          </span>
        </div>
        <div
          className={`flex h-6 w-6 items-center justify-center rounded-full border border-gray-200 bg-white text-gray-400 transition-all duration-300 group-hover:border-gray-300 group-hover:text-gray-600 ${
            isExpanded ? `rotate-180` : ''
          }`}>
          <ChevronDown className="h-3.5 w-3.5" />
        </div>
      </button>
    )
  }

  // 확장된 섹션 내용 렌더링
  const renderExpandedContent = () => {
    switch (activeSection) {
      case 'rating':
        return (
          <div className="animate-fadeIn flex flex-col items-center gap-3">
            <p>평점을 선택하세요</p>
            <StarRating
              rating={satisfaction}
              onRatingChange={onSatisfactionChange}
              showValue={true}
              size="lg"
            />
          </div>
        )
      case 'platform':
        return renderPlatformSection()
      case 'category':
        return renderCategorySection()
      case 'tag':
        return renderTagSection()
      default:
        return null
    }
  }

  return (
    <div className="space-y-6">
      {/* 2x2 그리드 레이아웃 */}
      <div className="grid grid-cols-2 gap-4">
        {/* 카테고리 선택 */}
        <div>{renderCollapsibleHeader('카테고리', 'category')}</div>

        {/* 모델 검색 */}
        <div>{renderCollapsibleHeader('AI 모델', 'platform')}</div>

        {/* 평점 */}
        <div>{renderCollapsibleHeader('평점', 'rating')}</div>

        {/* 태그 선택 */}
        <div>{renderCollapsibleHeader('태그', 'tag')}</div>
      </div>

      {/* 공통 확장 영역 - 2x2 그리드 하단에 표시 */}
      {activeSection && (
        <div
          key={activeSection} // key prop으로 강제 리렌더링
          className={`animate-fadeIn mb-6 rounded-xl border-2 p-4 ${
            activeSection === 'category'
              ? 'border-gray-200'
              : activeSection === 'platform'
                ? 'border-blue-200'
                : activeSection === 'rating'
                  ? 'border-yellow-200'
                  : activeSection === 'tag'
                    ? 'border-green-200'
                    : 'border-gray-200'
          }`}>
          {renderExpandedContent()}
        </div>
      )}

      {/* 선택된 태그 미리보기 */}
      <div className="flex flex-wrap gap-2">
        <CustomBadge variant="blue" size="responsive">
          {(() => {
            if (modelDetail && modelDetail.trim()) {
              return modelDetail
            }
            // Name → ID 변환 후 다시 표시명 결정
            const modelId = getModelId(model)
            return getModelDisplayNameFromBackend({
              modelId: modelId,
              modelEtc: modelEtc,
            })
          })()}
        </CustomBadge>
        <CustomBadge variant="gray" size="responsive">
          {(() => {
            // Name → ID 변환 후 다시 표시명 결정
            const categoryId = getCategoryId(category)
            return categoryId
              ? getCategoryDisplayNameFromBackend({
                  categoryId: categoryId,
                  categoryEtc: categoryEtc,
                })
              : category
          })()}
        </CustomBadge>
        {tags.map((tag, index) => (
          <CustomBadge key={index} variant="green" size="responsive">
            {tag}
          </CustomBadge>
        ))}
      </div>

      {/* 구분선 */}
      <div className="h-px w-full bg-gradient-to-r from-transparent via-gray-200 to-transparent"></div>
    </div>
  )
}
