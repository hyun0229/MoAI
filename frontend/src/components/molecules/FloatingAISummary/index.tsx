import React, { useState, useRef, useEffect } from 'react'
import type { FloatingAISummaryProps } from './types'

const FloatingAISummary: React.FC<FloatingAISummaryProps> = ({
  title,
  description,
  selectedModel,
  prompt,
  isSelectAll,
  isVisible,
  selectedContents,
  onTitleChange,
  onDescriptionChange,
  onModelChange,
  onPromptChange,
  onSelectAllChange,
  onContentRemove,
  onSubmit,
  onClose,
  onSuccess, // 새로운 prop 추가
}) => {
  const [isDragging, setIsDragging] = useState(false)
  const [position, setPosition] = useState({ x: 0, y: 0 })
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 })
  const [isLoading, setIsLoading] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  // 화면 중앙에 위치하도록 초기 위치 설정
  useEffect(() => {
    const centerX = (window.innerWidth - 800) / 2
    const centerY = (window.innerHeight + 100) / 2
    setPosition({ x: centerX, y: centerY })
  }, [])

  const handleMouseDown = (e: React.MouseEvent) => {
    if (containerRef.current) {
      const rect = containerRef.current.getBoundingClientRect()
      setDragOffset({
        x: e.clientX - rect.left,
        y: e.clientY - rect.top
      })
      setIsDragging(true)
    }
  }

  const handleMouseMove = (e: MouseEvent) => {
    if (isDragging) {
      setPosition({
        x: e.clientX - dragOffset.x,
        y: e.clientY - dragOffset.y
      })
    }
  }

  const handleMouseUp = () => {
    setIsDragging(false)
  }

  useEffect(() => {
    if (isDragging) {
      document.addEventListener('mousemove', handleMouseMove)
      document.addEventListener('mouseup', handleMouseUp)

      return () => {
        document.removeEventListener('mousemove', handleMouseMove)
        document.removeEventListener('mouseup', handleMouseUp)
      }
    }
  }, [isDragging, dragOffset])

  const handleSubmit = async () => {
    if (!title.trim() || selectedContents.length === 0) {
      alert('제목을 입력하고 자료를 선택해주세요.')
      return
    }

    setIsLoading(true)
    try {
      const summaryData = {
        fileId: selectedContents.map(content => parseInt(content.id)), // ContentItem.id를 number로 변환
        title: title,
        description: description,
        modelType: selectedModel,
        promptType: prompt || 'study-summary.v1'
      }

      await onSubmit(summaryData)

      // 성공 시 onSuccess 콜백 호출
      if (onSuccess) {
        onSuccess()
      }
    } finally {
      setIsLoading(false)
    }
  }

  if (!isVisible) return null

  return (
    <div
      ref={containerRef}
      className="fixed w-[950px] max-h-[450px] bg-white rounded-lg shadow-2xl border border-purple-200 z-50 cursor-move font-['Noto_Sans_KR']"
      style={{
        left: position.x,
        top: position.y,
        transform: 'none'
      }}
    >
      {/* Header */}
      <div
        className="bg-[#F6EEFF] text-gray-800 p-1 rounded-t-lg flex items-center justify-between cursor-move"
        onMouseDown={handleMouseDown}
      >
        <div className="flex items-center space-x-2 pl-4">
          <h2 className="text-base font-semibold">AI 요약본 만들기</h2>
          <span className="text-sm text-gray-600">({selectedContents.length}개 선택됨)</span>
        </div>
        <div className="flex items-center space-x-2 pr-4">
          <label className="flex items-center space-x-2 text-sm cursor-pointer">
            <input
              type="checkbox"
              checked={isSelectAll}
              onChange={(e) => onSelectAllChange(e.target.checked)}
              className="w-4 h-4"
            />
            <span>전체선택</span>
          </label>
          <button
            onClick={onClose}
            className="ml-2 p-1 text-gray-500 hover:text-gray-700 transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="p-4 space-y-4">
        {/* Title Input */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            제목
          </label>
          <input
            type="text"
            value={title}
            onChange={(e) => onTitleChange(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 text-sm"
            placeholder="요약본 제목을 입력하세요"
          />
        </div>

        {/* Selected Contents */}
        {selectedContents.length > 0 && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              선택된 자료 ({selectedContents.length}개)
            </label>
            <div className="max-h-24 overflow-y-auto border border-gray-200 rounded-lg p-2 bg-gray-50">
              <div className="flex flex-wrap gap-2">
                {selectedContents.map((content) => (
                  <div
                    key={content.id}
                    className="inline-flex items-center bg-purple-100 text-purple-800 px-3 py-1 rounded-full text-sm"
                  >
                    <span className="max-w-32 truncate">{content.title}</span>
                    <button
                      onClick={() => onContentRemove(content.id)}
                      className="ml-2 text-purple-600 hover:text-purple-800 transition-colors"
                    >
                      <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            </div>
            {selectedContents.length > 10 && (
              <p className="text-xs text-gray-500 mt-1">
                💡 많은 자료가 선택되었습니다. 스크롤하여 모든 항목을 확인하세요.
              </p>
            )}
          </div>
        )}

        {/* Description Input */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            설명
          </label>
          <textarea
            value={description}
            onChange={(e) => onDescriptionChange(e.target.value)}
            rows={1}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 text-sm resize-none"
            placeholder="요약본 설명을 입력하세요"
          />
        </div>

        {/* AI Summarization Section */}
        <div className="relative">
          {/* Model Selection at Top */}
          <div className="absolute top-1 left-1 z-10">
            <select
              value={selectedModel}
              onChange={(e) => onModelChange(e.target.value)}
              className="px-2 py-1 border-transparent rounded text-xs bg-white focus:outline-none focus:ring-1 focus:ring-purple-500"
            >
              <option value="gpt-4o">GPT-4</option>
              <option value="gpt-4o-mini">GPT-4o Mini</option>
              <option value="gemini-2.0-flash">gemini-2.0-flash</option>
              <option value="gemini-2.0-flash-lite">gemini-2.0-flash-lite</option>
              <option value="gemini-3.5-flash-lite">gemini-3.5-flash-lite</option>
              <option value="gemini-3.6-flash">gemini-3.6-flash</option>
            </select>
          </div>

          <textarea
            value={prompt}
            onChange={(e) => onPromptChange(e.target.value)}
            rows={2}
            className="w-full px-2 py-1 pt-8 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 text-sm resize-none pr-8"
            placeholder="원하는 요약 방식을 적어주세요."
          />

          {/* Bottom Section with Submit Button */}
          <div className="absolute top-1/2 right-2 transform -translate-y-1/2 flex items-center justify-end">
            <button
              onClick={handleSubmit}
              disabled={isLoading || !title.trim() || selectedContents.length === 0}
              className="p-1 text-gray-500 hover:text-gray-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? (
                <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
              ) : (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                </svg>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default FloatingAISummary
