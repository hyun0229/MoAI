import type { ContentItem } from '../../../types/content'

export interface FloatingAISummaryProps {
  title: string
  description: string
  selectedModel: string
  availableModels: string[]
  prompt: string
  isSelectAll: boolean
  isVisible: boolean
  selectedContents: ContentItem[]
  onTitleChange: (title: string) => void
  onDescriptionChange: (description: string) => void
  onModelChange: (model: string) => void
  onPromptChange: (prompt: string) => void
  onSelectAllChange: (isSelectAll: boolean) => void
  onContentRemove: (contentId: string) => void
  onSubmit: (data: {
    fileId: number[]
    title: string
    description: string
    modelType: string
    promptType: string
  }) => Promise<void>
  onClose: () => void
  onSuccess?: () => void
}