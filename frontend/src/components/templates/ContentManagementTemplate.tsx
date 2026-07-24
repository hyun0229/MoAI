import React, { useState, useEffect, useMemo, useRef } from 'react'
import CategoryTab from '../atoms/CategoryTab'
import SearchBar from '../molecules/SearchBar'
import ContentList from '../organisms/ContentList'
import FloatingAISummary from '@/components/molecules/FloatingAISummary'
import { useCreateAISummary } from '@/hooks/useAisummaries'
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

    // 선택 순서 업데이트 (선택된 항목이 실제로 변경된 경우에만)
    const currentSelectedSet = new Set(currentSelected)
    const prevSelectedSet = new Set(selectionOrder.filter(id =>
      contents.find(content => content.id === id)?.isSelected
    ))

    // 선택된 항목이 실제로 변경된 경우에만 selectionOrder 업데이트
    if (currentSelectedSet.size !== prevSelectedSet.size ||
      ![...currentSelectedSet].every(id => prevSelectedSet.has(id))) {
      const newOrder = selectionOrder.filter(id => currentSelected.includes(id))
      const newSelections = currentSelected.filter(id => !selectionOrder.includes(id))
      setSelectionOrder([...newOrder, ...newSelections])
    }

    // 선택된 컨텐츠가 있고 이전에 선택된게 없었을 때만 모달 자동 열기
    if (currentSelected.length > 0 && prevSelectedCountRef.current === 0 && !isModalVisible) {
      setIsModalVisible(true)
      setModalTitle('')
      setModalDescription('')
    }

    // 현재 selectedCount를 ref에 저장
    prevSelectedCountRef.current = selectedCount
  }, [selectedCount, contents, isModalVisible]) // selectionOrder를 의존성에서 제거

  // AI 요약 버튼 클릭 시 모달 열기
  const handleOpenAIModal = () => {
    setIsModalVisible(true)
    // 모달 열 때 모든 컨텐츠 선택 해제
    contents.forEach(content => {
      if (content.isSelected) {
        onContentSelect(content.id)
      }
    })
    // 모달 상태 초기화
    setModalTitle('')
    setModalDescription('')
    setPrompt('')
    setIsSelectAll(false)
    setSelectionOrder([])
  }

  // 전체선택 처리 - 수정된 버전
  useEffect(() => {
    if (isSelectAll) {
      const unselectedContents = contents.filter(content => !content.isSelected)
      unselectedContents.forEach(content => {
        onContentSelect(content.id)
      })
      // 전체선택 후 플래그 리셋
      setIsSelectAll(false)
    }
  }, [isSelectAll]) // contents와 onContentSelect를 의존성에서 제거

  const handleModalClose = () => {
    setIsModalVisible(false)
    // 모든 선택 해제
    contents.forEach(content => {
      if (content.isSelected) {
        onContentSelect(content.id)
      }
    })
    // 모달 상태 초기화
    setModalTitle('')
    setModalDescription('')
    setPrompt('')
    setIsSelectAll(false)
    setSelectionOrder([])
    // ref도 초기화
    prevSelectedCountRef.current = 0
  }

  const handleContentRemove = (contentId: string) => {
    onContentSelect(contentId) // 선택 해제
    setSelectionOrder(prev => prev.filter(id => id !== contentId))
  }

  // AI Summary 생성 훅
  const createAISummaryMutation = useCreateAISummary()

  const handleModalSubmit = async (summaryData: {
    fileId: number[]
    title: string
    description: string
    modelType: string
    promptType: string
  }): Promise<void> => {
    try {
      // API 호출
      await createAISummaryMutation.mutateAsync(summaryData)

      // 성공 후 onAISummarySuccess 콜백 호출
      if (onAISummarySuccess) {
        onAISummarySuccess()
      }

      // 성공 후 모달 닫기
      handleModalClose()
    } catch (error) {
      alert('AI 요약본 생성에 실패했습니다. 다시 시도해주세요.')
    }
  }

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm">
      {/* 제목과 세로바 */}
      <div className="flex items-center mb-6">
        <div className="w-2 h-8 rounded-full mr-3" style={{ backgroundColor: '#477866' }}></div>
        <h2 className="text-2xl font-bold text-gray-900">공부 자료</h2>
      </div>

      {/* Category Tabs - attached to the box */}
      <div className="px-0 pt-0 pb-0">
        <CategoryTab
          categories={categories}
          selectedCategories={selectedCategories}
          onCategoryToggle={onCategoryToggle}
          onAddClick={onAddCategory}
          currentUserRole={currentUserRole}
        />
      </div>

      {/* Main Content Box - 스크롤 가능하게 수정 */}
      <div className="flex-1 bg-gray-50 rounded-lg flex flex-col min-h-0 relative mt-4">
        {/* Upload Button - MainContent 박스의 오른쪽 위에 배치 */}
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

        {/* Search Bar */}
        <div className="p-4 pb-2 flex-shrink-0 pr-72">
          <SearchBar
            searchTerm={searchTerm}
            onSearchChange={onSearchChange}
            onSearch={onSearch}
            sortOrder={sortOrder}
            onSortChange={onSortChange}
          />
        </div>

        {/* Content List - 스크롤 가능한 영역 */}
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
