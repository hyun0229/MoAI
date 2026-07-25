import api from './api'
import type {
  AiSummaryCreateReq,
  AiSummaryCreateRes,
  AiSummaryListRes,
  AiSummarySidebarListRes,
  AiSummaryDashboardRes,
  AiSummaryEditReq,
  AiSummaryEditRes,
  AiSummaryDeleteRes
} from '@/types/aiSummary'

// snake_case를 camelCase로 변환하는 유틸리티 함수
const toCamelCase = (obj: any): any => {
  if (obj === null || typeof obj !== 'object') return obj
  if (Array.isArray(obj)) return obj.map(toCamelCase)

  return Object.keys(obj).reduce((acc, key) => {
    const camelKey = key.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase())
    acc[camelKey] = toCamelCase(obj[key])
    return acc
  }, {} as any)
}

// AI 요약 생성
export const createAiSummary = async (data: AiSummaryCreateReq): Promise<AiSummaryCreateRes> => {
  try {
    const response = await api.post('/ai/create', data)
    return toCamelCase(response.data)
  } catch (error) {
    throw error
  }
}

// 내 AI 요약 목록 조회
export const getMySummaries = async (): Promise<AiSummaryListRes> => {
  try {
    const response = await api.get('/ai/list')
    return toCamelCase(response.data)
  } catch (error: any) {
    // 404 에러 시 빈 배열 반환
    if (error.response?.status === 404) {
      return { summaries: [] }
    }
    throw error
  }
}

// 사이드바용 AI 요약 목록 조회
export const getSidebarSummaries = async (): Promise<AiSummarySidebarListRes> => {
  try {
    const response = await api.get('/ai/sidebar-list')
    return toCamelCase(response.data)
  } catch (error: any) {
    // 404 에러 시 빈 배열 반환
    if (error.response?.status === 404) {
      return { summaries: [] }
    }
    throw error
  }
}

// AI 요약 수정
export const editAiSummary = async (id: number, data: AiSummaryEditReq): Promise<AiSummaryEditRes> => {
  try {
    const response = await api.patch(`/ai/edit/${id}`, data)
    return toCamelCase(response.data)
  } catch (error) {
    throw error
  }
}

// AI 요약 삭제
export const deleteAiSummary = async (id: number): Promise<AiSummaryDeleteRes> => {
  try {
    const response = await api.delete(`/ai/delete/${id}`)
    return toCamelCase(response.data)
  } catch (error) {
    throw error
  }
}

// 대시보드용 AI 요약 목록 조회
export const getDashboardSummaries = async (): Promise<AiSummaryDashboardRes> => {
  try {
    const response = await api.get('/ai/dashboard')
    return toCamelCase(response.data)
  } catch (error: any) {
    // 404 에러 시 빈 배열 반환
    if (error.response?.status === 404) {
      return { summaries: [] }
    }
    throw error
  }
}

export const createAISummary = async (summaryData: {
  fileId: number[]
  title: string
  description: string
  modelType: string
  promptType: string
}) => {
  // 요청 데이터 로깅
  console.log('🚀 AI 요약본 생성 요청:', {
    endpoint: 'POST /ai/create',
    requestData: summaryData,
    timestamp: new Date().toISOString()
  })

  const response = await api.post('/ai/create', summaryData)

  // 응답 데이터 로깅
  console.log('✅ AI 요약본 생성 응답:', {
    status: response.status,
    responseData: response.data,
    timestamp: new Date().toISOString()
  })

  

  return response.data
}

export const getAvailableModels = async (): Promise<string[]> => {
  const response = await api.get('/ai/models')
  return response.data
}