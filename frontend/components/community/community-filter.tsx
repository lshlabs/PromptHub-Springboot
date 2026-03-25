/**
 * FilterPanel 컴포넌트
 *
 * 커뮤니티 페이지의 필터 패널을 표시합니다.
 * 플랫폼, 모델, 카테고리 필터링 기능을 제공합니다.
 * 백엔드 API와 연동하여 ID 기반 필터링을 수행합니다.
 */

'use client'
import type { Platform, Category } from '@/types/api'
import CustomButton from '@/components/common/custom-button'
import { useDelayedLoading } from '@/hooks/use-delayed-loading'

interface FilterPanelProps {
  showFilters: boolean
  selectedCategories: string[]
  selectedPlatforms: string[]
  selectedModels: string[]
  currentPlatformModel: string | null
  onToggleCategory: (categoryId: string) => void
  onTogglePlatform: (platformId: string) => void
  onToggleModel: (modelId: string) => void
  onClearAllFilters: () => void
  onClose: () => void
  onApplyFilters?: () => void

  onSelectAllPlatforms?: () => void
  onSelectAllModels?: () => void
  onSelectAllCategories?: () => void
  isAllPlatformsSelected?: () => boolean
  isAllModelsSelected?: () => boolean
  isAllCategoriesSelected?: () => boolean
  hasSelectedModelsForPlatform?: (platformId: string) => boolean
  platforms?: Platform[]
  categories?: Category[]
  models?: any[]
  loadingFilters?: boolean
}

export function FilterPanel({
  showFilters,
  selectedCategories,
  selectedPlatforms,
  selectedModels,
  currentPlatformModel,
  onToggleCategory,
  onTogglePlatform,
  onToggleModel,
  onClearAllFilters,
  onClose,
  onApplyFilters,

  onSelectAllPlatforms,
  onSelectAllModels,
  onSelectAllCategories,
  isAllPlatformsSelected,
  isAllModelsSelected,
  isAllCategoriesSelected,
  hasSelectedModelsForPlatform,
  platforms = [],
  categories = [],
  models = [],
  loadingFilters = false,
}: FilterPanelProps) {
  const showLoadingState = useDelayedLoading(loadingFilters, { delayMs: 150, minVisibleMs: 280 })

  const getActiveFiltersCount = () => {
    // 플랫폼은 카운트에서 제외하고 모델과 카테고리만 카운트합니다.
    return selectedCategories.length + selectedModels.length
  }

  if (!showFilters) return null

  if (loadingFilters) {
    return (
      <div className="animate-fadeIn overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
        <div className="h-1 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500"></div>
        <div className="p-6">
          {showLoadingState ? (
            <div className="flex items-center justify-center py-8">
              <div className="text-gray-500">필터 데이터를 불러오는 중...</div>
            </div>
          ) : (
            <div className="space-y-5" aria-hidden="true">
              <div className="h-5 w-40 animate-pulse rounded bg-gray-200" />
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                {Array.from({ length: 6 }).map((_, idx) => (
                  <div key={idx} className="h-9 animate-pulse rounded-lg bg-gray-100" />
                ))}
              </div>
              <div className="h-5 w-24 animate-pulse rounded bg-gray-200" />
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                {Array.from({ length: 6 }).map((_, idx) => (
                  <div key={`c-${idx}`} className="h-9 animate-pulse rounded-lg bg-gray-100" />
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="animate-fadeIn overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
      <div className="h-1 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500"></div>
      <div className="p-6">
        <div className="grid grid-cols-1 gap-8 sm:grid-cols-2">
          <div>
            <h4 className="mb-4 flex items-center gap-2 text-sm font-semibold text-gray-700">
              <div className="h-2 w-2 rounded-full bg-blue-500"></div>
              AI 플랫폼 & 모델
              {selectedModels.length > 0 && (
                <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs text-blue-700">
                  {selectedModels.length}개 선택
                </span>
              )}
            </h4>

            <div className="space-y-4">
              <div>
                <div className="mb-2 flex items-center justify-between">
                  <p className="text-xs font-medium text-gray-500">플랫폼</p>
                  <button
                    onClick={onSelectAllPlatforms}
                    className="text-xs text-blue-600 hover:text-blue-700 hover:underline">
                    {isAllPlatformsSelected?.() ? '전체 해제' : '전체선택'}
                  </button>
                </div>
                <div className="grid grid-cols-2 gap-2 sm:grid-cols-2 lg:grid-cols-3">
                  {platforms
                    .slice()
                    .sort((a, b) => {
                      const isOtherA = a?.id === 0 || a?.name === '기타'
                      const isOtherB = b?.id === 0 || b?.name === '기타'
                      if (isOtherA && !isOtherB) return 1
                      if (!isOtherA && isOtherB) return -1
                      return a.id - b.id
                    })
                    .map(platform => {
                      const hasSelectedModels =
                        hasSelectedModelsForPlatform?.(platform.id.toString()) || false
                      const isCurrentPlatform = currentPlatformModel === platform.id.toString()

                      return (
                        <button
                          key={platform.id}
                          onClick={() => onTogglePlatform(platform.id.toString())}
                          className={`rounded-lg border px-2.5 py-1.5 text-left text-sm transition-all duration-200 hover:scale-105 ${
                            hasSelectedModels
                              ? 'border-blue-200 bg-blue-50 text-blue-700 shadow-sm'
                              : isCurrentPlatform
                                ? 'border-gray-300 bg-gray-100 text-gray-700 shadow-sm'
                                : 'border-gray-200 bg-gray-50 text-gray-600 hover:border-gray-300 hover:bg-gray-100'
                          }`}>
                          {platform.name}
                        </button>
                      )
                    })}
                </div>
              </div>

              {(() => currentPlatformModel && models.length > 0)() && (
                <div>
                  <div className="mb-2 flex items-center justify-between">
                    <p className="text-xs font-medium text-gray-500">
                      {currentPlatformModel === '기타'
                        ? '기타 모델'
                        : `${platforms.find(p => p.id.toString() === currentPlatformModel)?.name || currentPlatformModel} 모델`}
                    </p>
                    {currentPlatformModel !== '기타' && (
                      <button
                        onClick={onSelectAllModels}
                        className="text-xs text-sky-600 hover:text-sky-700 hover:underline">
                        {isAllModelsSelected?.() ? '전체 해제' : '전체선택'}
                      </button>
                    )}
                  </div>
                  <div className="grid grid-cols-2 gap-2 sm:grid-cols-2 lg:grid-cols-3">
                    {models
                      .filter(model => model.platform?.toString() === currentPlatformModel)
                      .sort((a, b) => {
                        const isOtherA = a?.id === 0 || a?.name === '기타' || a?.slug === 'other'
                        const isOtherB = b?.id === 0 || b?.name === '기타' || b?.slug === 'other'
                        if (isOtherA && !isOtherB) return 1
                        if (!isOtherA && isOtherB) return -1

                        const orderA =
                          typeof a?.sort_order === 'number' && a.sort_order > 0
                            ? a.sort_order
                            : 999999
                        const orderB =
                          typeof b?.sort_order === 'number' && b.sort_order > 0
                            ? b.sort_order
                            : 999999
                        if (orderA !== orderB) return orderA - orderB

                        return a.id - b.id
                      })
                      .map(model => (
                        <button
                          key={model.id}
                          onClick={() => onToggleModel(model.id.toString())}
                          className={`rounded-lg border px-2.5 py-1.5 text-left text-sm transition-all duration-200 hover:scale-105 ${
                            selectedModels.includes(model.id.toString())
                              ? 'border-sky-200 bg-sky-50 text-sky-700 shadow-sm'
                              : 'border-gray-200 bg-gray-50 text-gray-600 hover:border-gray-300 hover:bg-gray-100'
                          }`}>
                          {model.name}
                        </button>
                      ))}
                  </div>
                </div>
              )}
            </div>
          </div>

          <div>
            <h4 className="mb-4 flex items-center gap-2 text-sm font-semibold text-gray-700">
              <div className="h-2 w-2 rounded-full bg-green-500"></div>
              카테고리
              {selectedCategories.length > 0 && (
                <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs text-green-700">
                  {selectedCategories.length}개 선택
                </span>
              )}
            </h4>

            <div className="space-y-4">
              <div>
                <div className="mb-2 flex items-center justify-between">
                  <p className="text-xs font-medium text-gray-500">카테고리</p>
                  <button
                    onClick={onSelectAllCategories}
                    className="text-xs text-green-600 hover:text-green-700 hover:underline">
                    {isAllCategoriesSelected?.() ? '전체 해제' : '전체선택'}
                  </button>
                </div>
                <div className="grid grid-cols-2 gap-2 sm:grid-cols-2 lg:grid-cols-3">
                  {categories
                    .slice()
                    .sort((a, b) => {
                      const isOtherA = a?.id === 0 || a?.name === '기타'
                      const isOtherB = b?.id === 0 || b?.name === '기타'
                      if (isOtherA && !isOtherB) return 1
                      if (!isOtherA && isOtherB) return -1
                      return a.id - b.id
                    })
                    .map(category => (
                      <button
                        key={category.id}
                        onClick={() => onToggleCategory(category.id.toString())}
                        className={`rounded-lg border px-2.5 py-1.5 text-left text-sm transition-all duration-200 hover:scale-105 ${
                          selectedCategories.includes(category.id.toString())
                            ? 'border-green-200 bg-green-50 text-green-700 shadow-sm'
                            : 'border-gray-200 bg-gray-50 text-gray-600 hover:border-gray-300 hover:bg-gray-100'
                        }`}>
                        {category.name}
                      </button>
                    ))}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="mt-6 border-t border-gray-100 pt-4">
          <div className="flex items-center justify-between">
            <p className="text-gray-600">
              <span className="font-medium">{getActiveFiltersCount()}개</span>의 필터 적용 중
            </p>
            <div className="flex items-center gap-3">
              <CustomButton
                color="flat"
                border="none"
                shape="square"
                size="responsive"
                onClick={onClearAllFilters}
                className="text-gray-500 hover:bg-red-50 hover:text-red-500">
                전체 해제
              </CustomButton>
              <CustomButton
                color="gradient"
                border="none"
                shape="square"
                size="responsive"
                onClick={onApplyFilters}
                className="rounded-xl">
                필터 적용하기
              </CustomButton>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
