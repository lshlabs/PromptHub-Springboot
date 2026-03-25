'use client'

import { useState, useEffect } from 'react'
import { use } from 'react'
import { Save, X } from 'lucide-react'
import EditPostMetaSection from '@/components/edit-post/edit-post-meta-section'
import EditPostContentSection from '@/components/edit-post/edit-post-content-section'
import { GoBackButton } from '@/components/common/go-back-button'
import CustomButton from '@/components/common/custom-button'
import { postsApi } from '@/lib/api'
import type { PostUpdateRequest, PostEditData } from '@/types/api'
import { useToast } from '@/hooks/use-toast'
import { useAuth } from '@/hooks/use-auth'
import { useRouter } from 'next/navigation'
import {
  getPlatformName,
  getModelName,
  getCategoryName,
  getDomainErrorMessage,
} from '@/lib/utils'

export default function EditPost({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params)
  const { toast } = useToast()
  const { isAuthenticated } = useAuth()
  const router = useRouter()

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

  const [title, setTitle] = useState('')
  const [satisfaction, setSatisfaction] = useState(0)
  const [prompt, setPrompt] = useState('')
  const [aiResponse, setAiResponse] = useState('')
  const [additionalOpinion, setAdditionalOpinion] = useState('')

  const [expandedSection, setExpandedSection] = useState<string | null>(null)

  const [platform, setPlatform] = useState('OpenAI')
  const [model, setModel] = useState('GPT-5')
  const [modelEtc, setModelEtc] = useState('')
  const [modelDetail, setModelDetail] = useState('')
  const [showModelEtcInput, setShowModelEtcInput] = useState(false)

  const [category, setCategory] = useState('기타')
  const [categoryEtc, setCategoryEtc] = useState('')
  const [showCategoryEtcInput, setShowCategoryEtcInput] = useState(false)

  const [tags, setTags] = useState<string[]>([])
  const [newTag, setNewTag] = useState('')

  const [isSaving, setIsSaving] = useState(false)

  const [platformsData, setPlatformsData] = useState<any[]>([])
  const [modelsData, setModelsData] = useState<any[]>([])
  const [categoriesData, setCategoriesData] = useState<any[]>([])
  const [isLoadingMetadata, setIsLoadingMetadata] = useState(true)
  const [metadataError, setMetadataError] = useState<string | null>(null)

  const [isLoadingPost, setIsLoadingPost] = useState(true)
  const [postError, setPostError] = useState<string | null>(null)

  useEffect(() => {
    const loadMetadata = async () => {
      try {
        setMetadataError(null)
        const [platformsRes, modelsRes, categoriesRes] = await Promise.all([
          postsApi.getPlatforms(),
          postsApi.getModels(),
          postsApi.getCategories(),
        ])

        setPlatformsData(platformsRes.data || [])
        setModelsData(modelsRes.data || [])
        setCategoriesData(categoriesRes.data || [])
      } catch (error) {
        setMetadataError(
          getDomainErrorMessage(
            error,
            '플랫폼/모델 목록을 최신값으로 받지 못해 임시 선택지로 편집 화면을 열었습니다. 저장 전 모델과 카테고리를 한 번 더 확인해 주세요.',
          ),
        )

        setPlatformsData([
          { id: 1, name: 'OpenAI' },
          { id: 2, name: 'Google' },
          { id: 3, name: 'Anthropic' },
          { id: 99, name: '기타' },
        ])
        setModelsData([
          { id: 1, name: 'o3', platform: 1, platform_name: 'OpenAI' },
          { id: 2, name: 'Gemini 2.5 Pro', platform: 2, platform_name: 'Google' },
          { id: 3, name: 'Claude 3.5 Sonnet', platform: 3, platform_name: 'Anthropic' },
          { id: 99, name: '기타', platform: 99, platform_name: '기타' },
        ])
        setCategoriesData([
          { id: 1, name: '창작' },
          { id: 2, name: '분석' },
          { id: 3, name: '코딩' },
          { id: 99, name: '기타' },
        ])
      } finally {
        setIsLoadingMetadata(false)
      }
    }

    loadMetadata()
  }, [])

  useEffect(() => {
    const loadPostData = async () => {
      if (!id) return
      if (
        isLoadingMetadata ||
        platformsData.length === 0 ||
        modelsData.length === 0 ||
        categoriesData.length === 0
      ) {
        return
      }

      try {
        setPostError(null)
        setIsLoadingPost(true)

        const response = await postsApi.getPost(Number(id))
        const postDetail = response.data

        const post: PostEditData = {
          id: postDetail.id,
          title: postDetail.title,
          platformId: postDetail.platformId,
          modelId: postDetail.modelId,
          categoryId: postDetail.categoryId,
          modelEtc: postDetail.modelEtc,
          categoryEtc: postDetail.categoryEtc,
          satisfaction: postDetail.satisfaction,
          prompt: postDetail.prompt,
          aiResponse: postDetail.aiResponse,
          additionalOpinion: postDetail.additionalOpinion,
          tags: postDetail.tags,
        }

        setTitle(post.title || '')
        setSatisfaction(Number(post.satisfaction) || 0)
        setPrompt(post.prompt || '')
        setAiResponse(post.aiResponse || '')
        setAdditionalOpinion(post.additionalOpinion || '')

        const platformName = getPlatformName(post.platformId, platformsData)
        const modelName = getModelName(post.modelId, post.modelEtc || null, modelsData)
        const categoryName = getCategoryName(
          post.categoryId,
          post.categoryEtc || null,
          categoriesData,
        )
        const modelDetailFromApi = (
          (postDetail as any).model_detail ||
          (postDetail as any).modelDetail ||
          ''
        ).toString()

        setPlatform(platformName)

        if (post.modelEtc && post.modelEtc.trim()) {
          setModel('기타')
          setModelEtc(post.modelEtc)
          setShowModelEtcInput(true)
          setModelDetail('')
        } else if (modelName === '기타') {
          setModel('기타')
          setModelEtc('')
          setShowModelEtcInput(true)
          setModelDetail('')
        } else {
          setModel(modelName)
          setModelEtc('')
          setModelDetail(
            modelDetailFromApi && modelDetailFromApi !== modelName ? modelDetailFromApi : '',
          )
          setShowModelEtcInput(true)
        }

        setCategory(categoryName)

        if (post.categoryEtc && post.categoryEtc.trim()) {
          setCategory('기타')
          setCategoryEtc(post.categoryEtc)
          setShowCategoryEtcInput(true)
        } else if (categoryName === '기타') {
          setCategory('기타')
          setCategoryEtc('')
          setShowCategoryEtcInput(true)
        } else {
          setCategory(categoryName)
          setCategoryEtc('')
          setShowCategoryEtcInput(false)
        }

        setTags(post.tags || [])

      } catch (error) {
        setPostError(
          getDomainErrorMessage(
            error,
            '수정 대상 게시글을 읽어오지 못했습니다. 삭제되었거나 접근 권한이 바뀌었을 수 있으니 목록에서 다시 선택해 주세요.',
            {
              unauthorized:
                '로그인 세션이 만료되어 수정 화면 접근이 중단되었습니다. 다시 로그인한 뒤 수정 페이지를 다시 열어주세요.',
              forbidden:
                '이 게시글은 현재 계정으로 수정할 수 없습니다. 작성자 계정인지 확인한 뒤 다시 접근해 주세요.',
              notFound:
                '요청한 게시글이 더 이상 존재하지 않습니다. 목록 화면으로 돌아가 다른 게시글을 선택해 주세요.',
            },
          ),
        )
      } finally {
        setIsLoadingPost(false)
      }
    }

    loadPostData()
  }, [id, platformsData, modelsData, categoriesData, isLoadingMetadata])

  const handlePlatformChange = async (newPlatform: string) => {
    setPlatform(newPlatform)

    if (newPlatform === '기타') {
      const 기타_플랫폼 = platformsData.find(p => p.name === '기타')
      if (기타_플랫폼) {
        const 기타_모델 = modelsData.find(m => m.platform === 기타_플랫폼.id && m.name === '기타')
        if (기타_모델) {
          setModel('기타')
          setShowModelEtcInput(true)
          setModelEtc('')
        }
      }
    } else {
      try {
        const platformData = platformsData.find(p => p.name === newPlatform)
        if (platformData) {
          const response = await postsApi.getPlatformModels(platformData.id)
          const { default_model } = response.data

          if (default_model) {
            setModel(default_model.name)
            setShowModelEtcInput(true)
            setModelEtc('')
            setModelDetail('')
          }
        }
      } catch (error) {
        toast({
          title: '플랫폼 모델 로드 실패',
          description: getDomainErrorMessage(
            error,
            '선택한 플랫폼의 기본 모델 조회가 실패해 임시 모델로 대체했습니다. 저장 전 모델명이 맞는지 직접 확인해 주세요.',
          ),
          variant: 'destructive',
        })
        setModel('GPT-5')
        setShowModelEtcInput(true)
        setModelEtc('')
        setModelDetail('')
      }
    }
  }

  const handleModelChange = (newModel: string) => {
    setModel(newModel)

    if (newModel === '기타') {
      setShowModelEtcInput(true)
      setModelEtc('')
      setModelDetail('')
    } else {
      setShowModelEtcInput(true)
      setModelEtc('')
      if (!modelDetail || modelDetail === model) {
        setModelDetail('')
      }
    }
  }

  const handleCategoryChange = (newCategory: string) => {
    setCategory(newCategory)

    if (newCategory === '기타') {
      setShowCategoryEtcInput(true)
    } else {
      setShowCategoryEtcInput(false)
      setCategoryEtc('')
    }
  }

  useEffect(() => {
    if (!showCategoryEtcInput && categoryEtc) {
      setCategoryEtc('')
    }
  }, [showCategoryEtcInput, categoryEtc])

  const handleSave = async () => {
    if (!isAuthenticated) {
      toast({
        title: '로그인 필요',
        description: '게시글을 수정하려면 로그인이 필요합니다.',
        variant: 'destructive',
      })
      return
    }

    if (!title.trim()) {
      toast({
        title: '입력 오류',
        description: '제목을 입력해주세요.',
        variant: 'destructive',
      })
      return
    }

    if (!prompt.trim()) {
      toast({
        title: '입력 오류',
        description: '프롬프트를 입력해주세요.',
        variant: 'destructive',
      })
      return
    }

    if (!aiResponse.trim()) {
      toast({
        title: '입력 오류',
        description: 'AI 응답을 입력해주세요.',
        variant: 'destructive',
      })
      return
    }

    try {
      setIsSaving(true)

      const platformData = platformsData.find(p => p.name === platform)
      const modelData = modelsData.find(m => m.name === model && m.platform === platformData?.id)
      const categoryData = categoriesData.find(c => c.name === category)

      if (!platformData) {
        toast({
          title: '입력 오류',
          description: '유효하지 않은 플랫폼입니다.',
          variant: 'destructive',
        })
        return
      }

      if (!categoryData) {
        toast({
          title: '입력 오류',
          description: '유효하지 않은 카테고리입니다.',
          variant: 'destructive',
        })
        return
      }

      const trimmedEtc = (modelEtc || '').trim()
      const trimmedDetail = (modelDetail || '').trim()
      const baseName = model
      const detailForSave =
        baseName !== '기타' && trimmedDetail && trimmedDetail !== baseName ? trimmedDetail : ''

      const updateData: PostUpdateRequest = {
        id: Number(id),
        title: title.trim(),
        satisfaction: satisfaction,
        platform: platformData.id,
        model: modelData?.id || null, // 기본 모델 ID 저장
        model_etc: baseName === '기타' ? trimmedEtc : '',
        model_detail: baseName !== '기타' ? detailForSave : '',
        category: categoryData.id,
        category_etc: category === '기타' ? categoryEtc.trim() : '',
        tags: tags,
        prompt: prompt.trim(),
        ai_response: aiResponse.trim(),
        additional_opinion: additionalOpinion.trim(),
      }

      await postsApi.updatePost(Number(id), updateData)

      toast({
        title: '저장 완료',
        description: '게시글이 성공적으로 수정되었습니다.',
      })

      router.push(`/post/${id}`)
    } catch (error) {
      const errorMessage = getDomainErrorMessage(
        error,
        '게시글 저장이 완료되지 않았습니다. 제목/본문 변경사항을 확인하고 저장 버튼을 다시 눌러주세요.',
        {
          unauthorized:
            '로그인 세션이 만료되어 저장 요청이 거절되었습니다. 다시 로그인한 뒤 같은 내용을 다시 저장해 주세요.',
          forbidden:
            '현재 계정에는 이 게시글 수정 권한이 없습니다. 작성자 계정으로 로그인했는지 확인해 주세요.',
          conflict:
            '다른 탭 또는 사용자에 의해 게시글이 먼저 수정되었습니다. 페이지를 새로고침해 최신 내용과 병합 후 다시 저장해 주세요.',
        },
      )
      toast({
        title: '저장 실패',
        description: errorMessage,
        variant: 'destructive',
      })
    } finally {
      setIsSaving(false)
    }
  }

  const handleCancel = () => {
    window.history.back()
  }

  if (isLoadingPost || isLoadingMetadata) {
    return (
      <div className="min-h-screen bg-gray-50">
        <div className="container mx-auto px-4 py-8">
          <div className="flex min-h-[400px] items-center justify-center">
            <div className="text-center">
              <div className="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-b-2 border-blue-500"></div>
              <p className="text-gray-600">게시글을 불러오는 중입니다...</p>
            </div>
          </div>
        </div>
      </div>
    )
  }

  if (postError && !title) {
    return (
      <div className="min-h-screen bg-gray-50">
        <div className="container mx-auto px-4 py-8">
          <div className="container mx-auto px-2 py-2 pb-4 sm:px-4 sm:py-4">
            <div className="mx-auto max-w-7xl">
              <div className="px-2 py-2">
                <div className="flex justify-start pb-2">
                  <GoBackButton />
                </div>
                <div className="flex min-h-[400px] items-center justify-center">
                  <div className="text-center">
                    <div className="max-w-md rounded-lg border border-red-200 bg-red-50 p-6">
                      <div className="mb-4 text-red-600">
                        <svg
                          className="mx-auto h-12 w-12"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24">
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.962-.833-2.732 0L4.082 15.5c-.77.833.192 2.5 1.732 2.5z"
                          />
                        </svg>
                      </div>
                      <h3 className="mb-2 text-lg font-semibold text-red-800">데이터 로딩 실패</h3>
                      <p className="mb-4 text-red-700">{postError}</p>
                      <button
                        onClick={() => window.location.reload()}
                        className="rounded-lg bg-red-600 px-4 py-2 text-white transition-colors hover:bg-red-700">
                        다시 시도
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        <div className="container mx-auto px-2 py-2 pb-4 sm:px-4 sm:py-4">
          <div className="mx-auto max-w-7xl">
            <div className="px-2 py-2">
              {/* 목록으로 가기 버튼 */}
              <div className="flex justify-start pb-2">
                <GoBackButton />
              </div>

              {/* 메타데이터 에러 알림 */}
              {metadataError && (
                <div className="mb-4 rounded-lg border border-yellow-200 bg-yellow-50 p-4">
                  <div className="flex items-center">
                    <div className="mr-3 text-yellow-600">
                      <svg
                        className="h-5 w-5"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24">
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.962-.833-2.732 0L4.082 15.5c-.77.833.192 2.5 1.732 2.5z"
                        />
                      </svg>
                    </div>
                    <p className="text-sm text-yellow-800">{metadataError}</p>
                  </div>
                </div>
              )}

              {/* 게시글 에러 알림 */}
              {postError && title && (
                <div className="mb-4 rounded-lg border border-orange-200 bg-orange-50 p-4">
                  <div className="flex items-center">
                    <div className="mr-3 text-orange-600">
                      <svg
                        className="h-5 w-5"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24">
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                        />
                      </svg>
                    </div>
                    <p className="text-sm text-orange-800">
                      {postError} 샘플 데이터를 사용하여 편집이 가능합니다.
                    </p>
                  </div>
                </div>
              )}

              {/* 메인 포스트 카드 - 수정 모드 */}
              <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
                {/* 상단 그라데이션 바 */}
                <div className="h-1 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500"></div>

                <div className="p-6">
                  {/* 메타 섹션 */}
                  <EditPostMetaSection
                    satisfaction={satisfaction}
                    platform={platform}
                    model={model}
                    modelEtc={modelEtc}
                    modelDetail={modelDetail}
                    category={category}
                    categoryEtc={categoryEtc}
                    tags={tags}
                    tagInput={newTag}
                    activeSection={expandedSection}
                    platformsData={platformsData}
                    modelsData={modelsData}
                    categoriesData={categoriesData}
                    onSatisfactionChange={setSatisfaction}
                    onPlatformChange={handlePlatformChange}
                    onModelChange={handleModelChange}
                    onModelEtcValueChange={setModelEtc}
                    onModelDetailValueChange={setModelDetail}
                    onCategoryChange={handleCategoryChange}
                    onCategoryEtcValueChange={setCategoryEtc}
                    onTagsChange={setTags}
                    onTagInputChange={setNewTag}
                    onActiveSectionChange={setExpandedSection}
                    onUserInteraction={() => {}}
                    showModelEtcInput={showModelEtcInput}
                    showCategoryEtcInput={showCategoryEtcInput}
                  />

                  {/* 본문 내용 수정 */}
                  <EditPostContentSection
                    title={title}
                    prompt={prompt}
                    aiResponse={aiResponse}
                    additionalOpinion={additionalOpinion}
                    onTitleChange={setTitle}
                    onPromptChange={setPrompt}
                    onAiResponseChange={setAiResponse}
                    onAdditionalOpinionChange={setAdditionalOpinion}
                  />

                  {/* 액션 버튼 */}
                  <div className="flex items-center justify-end border-t border-gray-100 pt-6">
                    <div className="flex gap-3">
                      <CustomButton
                        color="flat"
                        border="none"
                        shape="rounded"
                        size="responsive"
                        icon={<X className="h-4 w-4" />}
                        onClick={handleCancel}
                        className="text-gray-600">
                        취소
                      </CustomButton>
                      <CustomButton
                        color="gradient"
                        border="none"
                        shape="rounded"
                        size="responsive"
                        icon={<Save className="h-4 w-4" />}
                        onClick={handleSave}
                        disabled={isSaving}>
                        {isSaving ? '저장 중...' : '저장'}
                      </CustomButton>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
