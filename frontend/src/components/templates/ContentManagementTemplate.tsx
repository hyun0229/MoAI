import React, { useState, useEffect, useMemo, useRef } from 'react'
import CategoryTab from '../atoms/CategoryTab'
import SearchBar from '../molecules/SearchBar'
import ContentList from '../organisms/ContentList'
import FloatingAISummary from '@/components/molecules/FloatingAISummary'
import { useCreateAISummary } from '@/hooks/useAisummaries'
import { getAvailableModels } from '@/services/aiSummaryService'
import type { Category, ContentItem } from '@/types/content'

interface ContentManagementTemplateProps {
  categories: Category[]
  selectedCategories: number[]
  contents: ContentItem[]
  searchTerm: string
  sortOrder: 'newest' | 'oldest'
  onCategoryToggle: (categoryId: number) => void
  onAddCategory: () => void
  onSearchChange: (term: string) => void
  onSearch: () => void
  onSortChange: (order: 'newest' | 'oldest') => void
  onContentSelect: (contentId: string) => void
  onContentPreview: (contentId: string) => void
  onContentEdit: (contentId: string) => void
  onContentDelete: (contentId: string) => void
  onContentDownload: (contentId: string) => void
  onUploadData: () => void
  currentUserRole?: string
  onAISummarySuccess?: () => void
}

const ContentManagementTemplate: React.FC<ContentManagementTemplateProps> = ({
  categories,
  selectedCategories,
  contents,
  searchTerm,
  sortOrder,
  onCategoryToggle,
  onAddCategory,
  onSearchChange,
  onSearch,
  onSortChange,
  onContentSelect,
  onContentPreview,
  onContentEdit,
  onContentDelete,
  onContentDownload,
  onUploadData,
  currentUserRole,
  onAISummarySuccess,
}) => {
  // AI Summary Modal 상태 관리
  const [isModalVisible, setIsModalVisible] = useState(false)
  const [modalTitle, setModalTitle] = useState('')
  const [modalDescription, setModalDescription] = useState('')
  const [selectedModel, setSelectedModel] = useState('gemini-3.5-flash-lite')
  const [prompt, setPrompt] = useState('')
  const [isSelectAll, setIsSelectAll] = useState(false)
  const [selectionOrder, setSelectionOrder] = useState<string[]>([])

  // ↓↓↓ 새로 추가: 백엔드에서 사용 가능한 AI 모델 목록을 불러와서 저장
  const [availableModels, setAvailableModels] = useState<string[]>([])

  // ↓↓↓ 새로 추가: 컴포넌트가 처음 화면에 뜰 때 딱 한 번 모델 목록 조회
  useEffect(() => {
    getAvailableModels()
      .then((models) => {
        setAvailableModels(models)
        // 지금 선택된 모델이 새로 받아온 목록에 없으면, 목록의 첫 번째 모델로 바꿔줌
        if (models.length > 0 && !models.includes(selectedModel)) {
          setSelectedModel(models[0])
        }
      })
      .catch((err) => console.error('모델 목록 조회 실패:', err))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 이전 selectedCount를 추적하기 위한 ref
  const prevSelectedCountRef = useRef<number>(0)

  // 선택된 컨텐츠들을 useMemo로 계산
  const selectedContents = useMemo(() =>
    contents
      .filter(content => content.isSelected)
      .map(content => ({
        id: content.id,
        title: content.title,
        description: content.description,
        tags: content.tags,
        author: content.author,
        date: content.date,
        isSelected: content.isSelected
      })), [contents]
  )

  // 선택된 컨텐츠 개수 계산
  const selectedCount = useMemo(() =>
    contents.filter(content => content.isSelected).length,
    [contents]
  )

  // 선택 순서 업데이트 및 모달 자동 열기 - 수정된 버전
  useEffect(() => {
    const currentSelected = contents.filter(content => content.isSelected).map(content => content.id)

    const currentSelectedSet = new Set(currentSelected)
    const prevSelectedSet = new Set(selectionOrder.filter(id =>
      contents.find(content => content.id === id)?.isSelected
    ))

    if (currentSelectedSet.size !== prevSelectedSet.size ||
      ![...currentSelectedSet].every(id => prevSelectedSet.has(id))) {
      const newOrder = selectionOrder.filter(id => currentSelected.includes(id))
      const newSelections = currentSelected.filter(id => !selectionOrder.includes(id))
      setSelectionOrder([...newOrder, ...newSelections])
    }

    if (currentSelected.length > 0 && prevSelectedCountRef.current === 0 && !isModalVisible) {
      setIsModalVisible(true)
      setModalTitle('')
      setModalDescription('')
    }

    prevSelectedCountRef.current = selectedCount
  }, [selectedCount, contents, isModalVisible])

  const handleOpenAIModal = () => {
    setIsModalVisible(true)
    contents.forEach(content => {
      if (content.isSelected) {
        onContentSelect(content.id)
      }
    })
    setModalTitle('')
    setModalDescription('')
    setPrompt('')
    setIsSelectAll(false)
    setSelectionOrder([])
  }

  useEffect(() => {
    if (isSelectAll) {
      const unselectedContents = contents.filter(content => !content.isSelected)
      unselectedContents.forEach(content => {
        onContentSelect(content.id)
      })
      setIsSelectAll(false)
    }
  }, [isSelectAll])

  const handleModalClose = () => {
    setIsModalVisible(false)
    contents.forEach(content => {
      if (content.isSelected) {
        onContentSelect(content.id)
      }
    })
    setModalTitle('')
    setModalDescription('')
    setPrompt('')
    setIsSelectAll(false)
    setSelectionOrder([])
    prevSelectedCountRef.current = 0
  }

  const handleContentRemove = (contentId: string) => {
    onContentSelect(contentId)
    setSelectionOrder(prev => prev.filter(id => id !== contentId))
  }

  const createAISummaryMutation = useCreateAISummary()

  const handleModalSubmit = async (summaryData: {
    fileId: number[]
    title: string
    description: string
    modelType: string
    promptType: string
  }): Promise<void> => {
    try {
      await createAISummaryMutation.mutateAsync(summaryData)

      if (onAISummarySuccess) {
        onAISummarySuccess()
      }

      handleModalClose()
    } catch (error) {
      alert('AI 요약본 생성에 실패했습니다. 다시 시도해주세요.')
    }
  }

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm">
      <div className="flex items-center mb-6">
        <div className="w-2 h-8 rounded-full mr-3" style={{ backgroundColor: '#477866' }}></div>
        <h2 className="text-2xl font-bold text-gray-900">공부 자료</h2>
      </div>

      <div className="px-0 pt-0 pb-0">
        <CategoryTab
          categories={categories}
          selectedCategories={selectedCategories}
          onCategoryToggle={onCategoryToggle}
          onAddClick={onAddCategory}
          currentUserRole={currentUserRole}
        />
      </div>

      <div className="flex-1 bg-gray-50 rounded-lg flex flex-col min-h-0 relative mt-4">
        <div className="absolute top-4 right-4 z-10 flex gap-3">
          <button
            onClick={handleOpenAIModal}
            className="px-6 py-2 bg-[#AA64FF] text-white rounded-lg hover:bg-[#9955EE] transition-colors"
          >
            AI 요약
          </button>
          <button
            onClick={onUploadData}
            className="px-6 py-2 bg-[#AA64FF] text-white rounded-lg hover:bg-[#9955EE] transition-colors"
          >
            자료 올리기
          </button>
        </div>

        <div className="p-4 pb-2 flex-shrink-0 pr-72">
          <SearchBar
            searchTerm={searchTerm}
            onSearchChange={onSearchChange}
            onSearch={onSearch}
            sortOrder={sortOrder}
            onSortChange={onSortChange}
          />
        </div>

        <div className="flex-1 overflow-y-auto px-4 pb-4">
          <ContentList
            contents={contents}
            onContentSelect={onContentSelect}
            onContentPreview={onContentPreview}
            onContentEdit={onContentEdit}
            onContentDelete={onContentDelete}
            onContentDownload={onContentDownload}
          />
        </div>
      </div>

      {/* AI Summary Modal */}
      <FloatingAISummary
        title={modalTitle}
        description={modalDescription}
        selectedModel={selectedModel}
        availableModels={availableModels}
        prompt={prompt}
        isSelectAll={isSelectAll}
        isVisible={isModalVisible}
        selectedContents={selectedContents}
        onTitleChange={setModalTitle}
        onDescriptionChange={setModalDescription}
        onModelChange={setSelectedModel}
        onPromptChange={setPrompt}
        onSelectAllChange={setIsSelectAll}
        onContentRemove={handleContentRemove}
        onSubmit={handleModalSubmit}
        onClose={handleModalClose}
        onSuccess={onAISummarySuccess}
      />
    </div>
  )
}

export default ContentManagementTemplate