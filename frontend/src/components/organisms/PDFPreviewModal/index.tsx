import React, { useState, useEffect } from 'react'
import { Document, Page, pdfjs } from 'react-pdf'
import { refService } from '../../../services/refService'

// PDF.js worker 설정 - Vite 번들 사용 (CORS 문제 해결)
// public 폴더의 워커 파일 사용
pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

const PDF_OPTIONS = {
  cMapUrl: `//unpkg.com/pdfjs-dist@${pdfjs.version}/cmaps/`,
  cMapPacked: true,
}


interface PDFPreviewModalProps {
  isOpen: boolean
  onClose: () => void
  fileId: number
  fileName: string
}

const PDFPreviewModal: React.FC<PDFPreviewModalProps> = ({
  isOpen,
  onClose,
  fileId,
  fileName,
}) => {
  const [pdfUrl, setPdfUrl] = useState<string>('')
  const [numPages, setNumPages] = useState<number>(0)
  const [pageNumber, setPageNumber] = useState<number>(1)
  const [loading, setLoading] = useState<boolean>(false)
  const [error, setError] = useState<string>('')

  // PDF View URL 가져오기
  useEffect(() => {
    if (isOpen && fileId) {
      fetchPDFUrl()
    }
  }, [isOpen, fileId])

  const fetchPDFUrl = async () => {
    try {
      setLoading(true)
      setError('')

      const response = await refService.getViewUrl(fileId)
      setPdfUrl(response.presignedUrl)
    } catch (err) {
      setError('PDF를 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const onDocumentLoadSuccess = ({ numPages }: { numPages: number }) => {
    setNumPages(numPages)
    setPageNumber(1)
  }

  const onDocumentLoadError = (_error: Error) => {
    setError('PDF를 표시할 수 없습니다. 새 창에서 열기를 시도해보세요.')
  }

  const changePage = (offset: number) => {
    setPageNumber(prevPageNumber => prevPageNumber + offset)
  }

  const previousPage = () => {
    changePage(-1)
  }

  const nextPage = () => {
    changePage(1)
  }

  const openInNewWindow = () => {
    if (pdfUrl) {
      try {
        window.open(pdfUrl, '_blank')
      } catch (error) {
        // 새 창 열기 실패 처리
      }
    }
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-[90vw] h-[90vh] max-w-6xl max-h-[90vh] flex flex-col">
        {/* 헤더 */}
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold">{fileName}</h2>
          <div className="flex items-center gap-2">
            <button
              onClick={openInNewWindow}
              className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors"
            >
              새 창에서 열기
            </button>
            <button
              onClick={onClose}
              className="px-4 py-2 bg-gray-500 text-white rounded hover:bg-gray-600 transition-colors"
            >
              닫기
            </button>
          </div>
        </div>

        {/* PDF 뷰어 */}
        <div className="flex-1 bg-gray-100 rounded-lg p-4 overflow-auto">
          {loading && (
            <div className="flex items-center justify-center h-full">
              <div className="text-lg">PDF를 불러오는 중...</div>
            </div>
          )}

          {error && (
            <div className="flex flex-col items-center justify-center h-full gap-4">
              <div className="text-lg text-red-500">{error}</div>
              {pdfUrl && (
                <button
                  onClick={openInNewWindow}
                  className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors"
                >
                  새 창에서 열기
                </button>
              )}
            </div>
          )}

          {pdfUrl && !loading && !error && (
            <div className="flex flex-col items-center">
              <Document
                file={pdfUrl}
                options={PDF_OPTIONS}
                onLoadSuccess={onDocumentLoadSuccess}
                onLoadError={onDocumentLoadError}
              >
                <Page
                  pageNumber={pageNumber}
                  width={Math.min(800, window.innerWidth * 0.8)}
                  renderTextLayer={false}
                  renderAnnotationLayer={false}
                />
              </Document>

              {/* 페이지 네비게이션 */}
              {numPages > 1 && (
                <div className="flex items-center gap-4 mt-4">
                  <button
                    onClick={previousPage}
                    disabled={pageNumber <= 1}
                    className="px-3 py-1 bg-gray-300 text-gray-700 rounded disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-400"
                  >
                    이전
                  </button>
                  <span className="text-sm">
                    {pageNumber} / {numPages}
                  </span>
                  <button
                    onClick={nextPage}
                    disabled={pageNumber >= numPages}
                    className="px-3 py-1 bg-gray-300 text-gray-700 rounded disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-400"
                  >
                    다음
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default PDFPreviewModal
