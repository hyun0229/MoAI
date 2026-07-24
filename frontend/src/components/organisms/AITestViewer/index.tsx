import React, { useEffect, useRef, useState } from 'react'
import { Document, Page, pdfjs } from 'react-pdf'
import 'react-pdf/dist/Page/AnnotationLayer.css'
import 'react-pdf/dist/Page/TextLayer.css'
import SplitResizer from '../../atoms/SplitResizer'
import fileImage from '../../../assets/MoAI/file.png'

// 타입 정의
interface PDFItem {
  id: string
  url: string
  title?: string
}

interface SummaryItem {
  docsId: number
  pageNumber: number
  originalQuote: string
  summarySentence: string
}

interface AITestViewerProps {
  pdfList: PDFItem[]
  summaryList: SummaryItem[]
  selectedPdf?: PDFItem | null
  onPdfChange?: (pdf: PDFItem) => void
  onSummaryClick?: (item: SummaryItem) => void
}

// PDF.js worker 설정
pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`

// ────────────────────────────────
// 하이라이트 유틸리티 함수들
const norm = (s: string) => (s || '').replace(/\s+/g, ' ').trim()

const PDF_OPTIONS = {
  cMapUrl: `//unpkg.com/pdfjs-dist@${pdfjs.version}/cmaps/`,
  cMapPacked: true,
}

const trigrams = (s: string) => {
  const t = norm(s)
  if (t.length < 3) return new Set([t])
  const set = new Set<string>()
  for (let i = 0; i <= t.length - 3; i++) set.add(t.slice(i, i + 3))
  return set
}

const dice = (aSet: Set<string>, bSet: Set<string>) => {
  if (!aSet.size || !bSet.size) return 0
  let inter = 0
  const small = aSet.size < bSet.size ? aSet : bSet
  const large = aSet.size < bSet.size ? bSet : aSet
  for (const x of small) if (large.has(x)) inter++
  return (2 * inter) / (aSet.size + bSet.size)
}

// 정렬 기반 하이라이트 함수
function highlightQuoteByAlignment(pageRootEl: HTMLElement, quote: string): boolean {
  // 기존 오버레이 제거
  pageRootEl.querySelectorAll('.pdf-quote-hit').forEach(n => n.remove())
  const q = norm(quote)
  if (!q) return false

  const textLayer = pageRootEl.querySelector('.react-pdf__Page__textContent') as HTMLElement
  if (!textLayer) return false

  const spans = Array.from(textLayer.querySelectorAll('span'))
  if (!spans.length) return false

  // 좌표계
  const layerRect = textLayer.getBoundingClientRect()
  const info = spans.map(sp => {
    const r = sp.getBoundingClientRect()
    return {
      text: sp.textContent || '',
      top: r.top - layerRect.top,
      left: r.left - layerRect.left,
      right: r.right - layerRect.left,
      bottom: r.bottom - layerRect.top
    }
  })

  // 누적 길이
  const lens = info.map(s => s.text.length)
  const pref = [0]
  for (let i = 0; i < lens.length; i++) pref.push(pref[pref.length - 1] + lens[i])

  const qLen = q.length
  const qTri = trigrams(q)
  const minChars = Math.max(12, Math.floor(qLen * 0.8))
  const maxChars = Math.max(minChars, Math.floor(qLen * 1.2))

  const findEnd = (start: number, chars: number) => {
    let lo = start + 1, hi = spans.length
    const want = pref[start] + chars
    while (lo < hi) {
      const mid = (lo + hi) >> 1
      if (pref[mid] >= want) hi = mid; else lo = mid + 1
    }
    return lo
  }

  let best = { score: 0, s: 0, e: 0 }
  for (let sIdx = 0; sIdx < spans.length; sIdx++) {
    for (const tgt of [minChars, qLen, maxChars]) {
      const eIdx = Math.min(findEnd(sIdx, tgt), spans.length)
      if (eIdx <= sIdx + 1) continue
      const wText = norm(info.slice(sIdx, eIdx).map(x => x.text).join(' '))
      if (!wText) continue
      const sc = dice(trigrams(wText), qTri)
      if (sc > best.score) best = { score: sc, s: sIdx, e: eIdx }
    }
  }

  if (best.score < 0.35) return false

  const picked = info.slice(best.s, best.e)
  const left = Math.min(...picked.map(s => s.left))
  const right = Math.max(...picked.map(s => s.right))
  const top = Math.min(...picked.map(s => s.top))
  const bottom = Math.max(...picked.map(s => s.bottom))

  const box = document.createElement('div')
  box.className = 'pdf-quote-hit'
  box.style.left = `${left - 3}px`
  box.style.top = `${top - 2}px`
  box.style.width = `${(right - left) + 6}px`
  box.style.height = `${(bottom - top) + 4}px`
  textLayer.appendChild(box)
  return true
}

// ────────────────────────────────

const AITestViewer: React.FC<AITestViewerProps> = ({
  pdfList = [],
  summaryList = [],
  selectedPdf,
  onPdfChange,
  onSummaryClick
}) => {
  const [numPages, setNumPages] = useState<number | null>(null)
  const [currentPage, setCurrentPage] = useState(1)
  const [highlight, setHighlight] = useState<{ page: number | null; quote: string | null }>({
    page: null,
    quote: null
  })
  const [pageRenderTick, setPageRenderTick] = useState(0)
  const [leftPanelWidth, setLeftPanelWidth] = useState(400)

  const pageRef = useRef<HTMLDivElement>(null)
  const retryTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // 페이지 이동 시 포커스
  useEffect(() => {
    if (pageRef.current) {
      pageRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, [currentPage, selectedPdf])

  // 하이라이트 실행 (렌더 완료 + 리트라이)
  useEffect(() => {
    // 기존 리트라이 타이머 정리
    if (retryTimerRef.current) {
      clearTimeout(retryTimerRef.current)
      retryTimerRef.current = null
    }

    if (!pageRef.current) return

    // 대상 페이지 아니면 제거
    if (!highlight.quote || highlight.page !== currentPage) {
      pageRef.current.querySelectorAll('.pdf-quote-hit').forEach(n => n.remove())
      return
    }

    let attempts = 0
    const MAX_ATTEMPTS = 8
    const DELAY_MS = 60

    const tryHighlight = () => {
      attempts++
      requestAnimationFrame(() => {
        if (pageRef.current) {
          const ok = highlightQuoteByAlignment(pageRef.current, highlight.quote!)
          if (!ok && attempts < MAX_ATTEMPTS) {
            retryTimerRef.current = setTimeout(tryHighlight, DELAY_MS)
          }
        }
      })
    }

    tryHighlight()

    return () => {
      if (retryTimerRef.current) {
        clearTimeout(retryTimerRef.current)
        retryTimerRef.current = null
      }
    }
  }, [currentPage, selectedPdf, highlight.page, highlight.quote, pageRenderTick])

  const handleSummaryClick = (item: SummaryItem) => {
    const pdf = pdfList.find(p => p.id === `doc${item.docsId}`)
    if (pdf && onPdfChange) {
      onPdfChange(pdf)
    }
    setHighlight({ page: item.pageNumber, quote: item.originalQuote })
    setCurrentPage(item.pageNumber)
    if (onSummaryClick) {
      onSummaryClick(item)
    }
  }

  const handleResize = (width: number) => {
    setLeftPanelWidth(width)
  }

  const clearHighlight = () => {
    setHighlight({ page: null, quote: null })
    if (retryTimerRef.current) {
      clearTimeout(retryTimerRef.current)
      retryTimerRef.current = null
    }
    pageRef.current?.querySelectorAll('.pdf-quote-hit').forEach(n => n.remove())
  }

  if (!selectedPdf) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <div className="mb-6">
            <img
              src={fileImage}
              alt="File Icon"
              className="w-72 h-72 mx-auto mb-4"
            />
            <p className="text-gray-600 text-xl">현재 선택된 파일이 없어요...</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-full">
      {/* PDF 하이라이트 스타일 */}
      <style>{`
        .pdf-quote-hit {
          position: absolute;
          background: rgba(255, 230, 140, 0.32);
          box-shadow: 0 0 0 2px rgba(255, 196, 0, 0.55) inset;
          border-radius: 4px;
          pointer-events: none;
          transition: opacity .2s ease;
        }
      `}</style>

      {/* 왼쪽: 요약 목록 */}
      <div
        className="bg-white border-r border-gray-200 p-4 overflow-y-auto"
        style={{ width: `${leftPanelWidth}px` }}
      >
        <h3 className="text-lg font-semibold mb-4">인용문 목록 ({summaryList.length}개)</h3>
        <div className="space-y-3 max-h-[calc(100vh-200px)] overflow-y-auto">
          {summaryList.map((item: SummaryItem, idx: number) => (
            <button
              key={idx}
              className={`w-full p-3 text-left rounded border transition-colors ${
                highlight.page === item.pageNumber && highlight.quote === item.originalQuote
                  ? 'bg-blue-100 border-blue-300'
                  : 'bg-gray-50 border-gray-200 hover:bg-gray-100'
              }`}
              onClick={() => handleSummaryClick(item)}
            >
              <div className="text-sm text-gray-600 mb-1">
                <span className="font-semibold">문서 ID:</span> {item.docsId} |
                <span className="font-semibold"> 페이지:</span> {item.pageNumber}
              </div>
              <div className="text-sm text-blue-600">
                <span className="font-semibold"></span> {item.summarySentence}
              </div>
            </button>
          ))}
          {summaryList.length === 0 && (
            <div className="text-center text-gray-500 py-4">
              인용문이 없습니다.
            </div>
          )}
        </div>
      </div>

      {/* 리사이저 */}
      <SplitResizer
        onResize={handleResize}
        minLeftWidth={300}
        maxLeftWidth={800}
      />

      {/* 오른쪽: PDF 뷰어 */}
      <div className="flex-1 bg-gray-100 p-4">
        {/* PDF 컨트롤 바 */}
        <div className="flex justify-between items-center mb-4 bg-white p-3 rounded-lg shadow">
          <div>
            <span className="mr-2">PDF:</span>
            <select
              value={selectedPdf.id}
              onChange={e => {
                const found = pdfList.find((p: PDFItem) => p.id === e.target.value)
                if (found && onPdfChange) {
                  onPdfChange(found)
                  setNumPages(null)
                  setCurrentPage(1)
                  clearHighlight()
                }
              }}
              className="px-3 py-1 border rounded"
            >
              {pdfList.map((pdf: PDFItem) => (
                <option key={pdf.id} value={pdf.id}>
                  {pdf.title || pdf.id}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-2">
            <button
              disabled={!numPages || currentPage <= 1}
              onClick={() => setCurrentPage((p: number) => Math.max(1, p - 1))}
              className="px-3 py-1 bg-blue-500 text-white rounded disabled:bg-gray-300"
            >
              ← 이전
            </button>
            <span className="px-3">
              페이지 {numPages ? currentPage : '-'} {numPages ? `/ ${numPages}` : ''}
            </span>
            <button
              disabled={!numPages || currentPage >= numPages}
              onClick={() => setCurrentPage((p: number) => Math.min(numPages!, p + 1))}
              className="px-3 py-1 bg-blue-500 text-white rounded disabled:bg-gray-300"
            >
              다음 →
            </button>
            <button
              onClick={clearHighlight}
              className="px-3 py-1 bg-red-500 text-white rounded"
              title="하이라이트 해제"
            >
              하이라이트 해제
            </button>
          </div>
        </div>

        {/* PDF 문서 */}
        <div className="bg-white rounded-lg shadow overflow-auto" style={{ height: 'calc(100% - 80px)' }}>
          <Document
            file={selectedPdf.url}
            options={PDF_OPTIONS}
            onLoadSuccess={({ numPages }) => {
              setNumPages(numPages)
              setCurrentPage(p => Math.min(Math.max(1, p), numPages))
            }}
            loading={<div className="text-center p-8">PDF 문서를 불러오는 중...</div>}
            onLoadError={() => {
              // 에러 처리
            }}
          >
            <div
              ref={pageRef}
              className={highlight.page === currentPage ? 'bg-blue-50' : ''}
            >
              <Page
                pageNumber={currentPage}
                width={Math.min(800, window.innerWidth * 0.4)}
                onRenderSuccess={() => setPageRenderTick(t => t + 1)}
              />
            </div>
          </Document>
        </div>
      </div>
    </div>
  )
}

export default AITestViewer
