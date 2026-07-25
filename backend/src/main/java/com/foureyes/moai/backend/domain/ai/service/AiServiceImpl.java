package com.foureyes.moai.backend.domain.ai.service;

import com.foureyes.moai.backend.commons.exception.CustomException;
import com.foureyes.moai.backend.commons.exception.ErrorCode;
import com.foureyes.moai.backend.commons.util.StorageService;
import com.foureyes.moai.backend.domain.ai.dto.DocsItem;
import com.foureyes.moai.backend.domain.ai.dto.SummaryDto;
import com.foureyes.moai.backend.domain.ai.dto.request.CreateAiSummaryRequest;
import com.foureyes.moai.backend.domain.ai.dto.request.EditAiSummaryRequest;
import com.foureyes.moai.backend.domain.ai.dto.response.AiSummaryResponseDto;
import com.foureyes.moai.backend.domain.ai.dto.response.CreateAiSummaryResponse;
import com.foureyes.moai.backend.domain.ai.dto.response.DashboardSummariesResponse;
import com.foureyes.moai.backend.domain.ai.dto.response.SidebarSummariesResponse;
import com.foureyes.moai.backend.domain.ai.entity.AiSummary;
import com.foureyes.moai.backend.domain.ai.entity.AiSummaryDocument;
import com.foureyes.moai.backend.domain.ai.internal.*;
import com.foureyes.moai.backend.domain.ai.repository.AiSummaryDocumentRepository;
import com.foureyes.moai.backend.domain.ai.repository.AiSummaryRepository;
import com.foureyes.moai.backend.domain.document.entity.Document;
import com.foureyes.moai.backend.domain.document.repository.DocumentRepository;
import com.foureyes.moai.backend.domain.document.service.DocumentService;
import com.foureyes.moai.backend.domain.user.entity.User;
import com.foureyes.moai.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final AiSummaryRepository aiSummaryRepository;
    private final AiSummaryDocumentRepository aiSummaryDocumentRepository;

    private final DocumentService documentService;
    private final StorageService storageService;

    // 역할별 컴포넌트
    private final PdfTextExtractor pdfTextExtractor;
    private final PromptBuilder promptBuilder;
    private final SummaryParser summaryParser;

    private final AiClientRouter aiClientRouter;
    private final AiSummaryPersistenceService aiSummaryPersistenceService;

    /**
     * 클래스 레벨 @Transactional을 이 메서드만 무효화한다.
     * PDF 추출 + AI 호출(수 초 이상 걸릴 수 있음) 동안 DB 커넥션을 붙잡고 있지 않기 위함.
     * 실제 DB 저장은 aiSummaryPersistenceService.save()에서 짧게 트랜잭션으로 묶는다.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CreateAiSummaryResponse createSummary(int ownerId, CreateAiSummaryRequest req) {
        if (req.getFileId() == null || req.getFileId().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 문서 검증
        List<Integer> docIds = req.getFileId().stream()
            .filter(Objects::nonNull).distinct().toList();
        List<Document> docs = documentRepository.findAllById(docIds);
        if (docs.size() != docIds.size()) throw new CustomException(ErrorCode.DOCUMENT_NOT_FOUND);

        try {
            List<String> docBlocks = new ArrayList<>(docs.size());
            for (Document d : docs) {
                String key = documentService.getDocumentKeyIfAllowed(ownerId, d.getId());
                try (InputStream in = storageService.openDocumentStream(key)) {
                    String text = pdfTextExtractor.extractTextWithPages(in, Optional.ofNullable(d.getTitle()).orElse("document-" + d.getId()));
                    String clipped = promptBuilder.clip(text, 12000);
                    docBlocks.add(promptBuilder.formatDocBlock(d.getId(), d.getTitle(), clipped));
                }
            }
            String joinedBlocks = String.join("\n\n", docBlocks);

            String prompt = promptBuilder.buildMultiDocPrompt(joinedBlocks, req.getPromptType());

            // --- 여기까지 DB 트랜잭션이 전혀 열려있지 않은 상태로 진행됨 ---
            String summaryJson = aiClientRouter
                    .generateJsonArray(req.getModelType(), prompt)
                    .block();

            List<SummaryDto> parsed;
            try {
                parsed = summaryParser.parse(summaryJson);
            } catch (RuntimeException parseError) {
                log.warn("AI 응답 파싱 완전 실패, AI 1회 재호출 시도");
                summaryJson = aiClientRouter.generateJsonArray(req.getModelType(), prompt).block();
                parsed = summaryParser.parse(summaryJson);
            }
            log.info("AI 요약 파싱 결과: {} items", parsed.size());

            // --- DB 저장만 별도 Bean에서 짧게 트랜잭션으로 처리 ---
            AiSummary summary = aiSummaryPersistenceService.save(owner, req, summaryJson, docs);

            return CreateAiSummaryResponse.builder()
                .summary_id(summary.getId())
                .title(summary.getTitle())
                .description(summary.getDescription())
                .model_type(summary.getModelType())
                .prompt_type(summary.getPromptType())
                .build();

        } catch (CustomException e) {
            throw e;
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
            log.warn("AI API 호출 한도 초과 (429): {}", e.getMessage());
            throw new RuntimeException("AI 서비스 호출 한도를 초과했습니다. 잠시 후 다시 시도해주세요.", e);
        } catch (Exception e) {
            log.error("AI 요약 생성 실패", e);
            throw new RuntimeException("AI 요약 생성에 실패했습니다.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummariesResponse getDashboardList(int ownerId) {
        var list = aiSummaryRepository.findByOwner_IdOrderByCreatedAtDesc(ownerId);
        var items = list.stream()
            .map(s -> DashboardSummariesResponse.Item.builder()
                .summaryId(s.getId())
                .title(s.getTitle())
                .description(s.getDescription())
                .createdAt(s.getCreatedAt())
                .build())
            .toList();

        return DashboardSummariesResponse.builder()
            .summaries(items)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SidebarSummariesResponse getSidebarList(int ownerId) {
        var rows = aiSummaryDocumentRepository.findSidebarRows(ownerId);

        // studyId 기준 그룹핑 + 같은 summaryId 중복 제거
        Map<Integer, SidebarSummariesResponse.StudyBlock> studies = new LinkedHashMap<>();
        for (var r : rows) {
            var block = studies.computeIfAbsent(
                r.getStudyId(),
                id -> SidebarSummariesResponse.StudyBlock.builder()
                    .studyId(r.getStudyId())
                    .name(r.getStudyName())
                    .studyImg(r.getStudyImg())
                    .summaries(new ArrayList<>())
                    .build()
            );

            boolean exists = block.getSummaries().stream()
                .anyMatch(x -> x.getSummaryId() == r.getSummaryId());
            if (!exists) {
                block.getSummaries().add(
                    SidebarSummariesResponse.SummaryItem.builder()
                        .summaryId(r.getSummaryId())
                        .title(r.getTitle())
                        .description(r.getDescription())
                        .modelType(r.getModelType())
                        .promptType(r.getPromptType())
                        .createdAt(r.getCreatedAt())
                        .build()
                );
            }
        }

        return SidebarSummariesResponse.builder()
            .studies(new ArrayList<>(studies.values()))
            .build();
    }

    @Override
    public void deleteSummary(int ownerId, int summaryId) {
        var summary = aiSummaryRepository.findById(summaryId)
            .orElseThrow(() -> new CustomException(ErrorCode.SUMMARY_NOT_FOUND));

        if (summary.getOwner() == null || summary.getOwner().getId() != ownerId) {
            throw new CustomException(ErrorCode.FORBIDDEN_SUMMARY_ACCESS);
        }

        aiSummaryDocumentRepository.deleteBySummary_Id(summaryId);

        aiSummaryRepository.delete(summary);
    }

    @Override
    @Transactional
    public void editSummary(int userId, int summaryId, EditAiSummaryRequest request) {
        AiSummary summary = aiSummaryRepository.findById(summaryId)
            .orElseThrow(() -> new CustomException(ErrorCode.SUMMARY_NOT_FOUND));

        if (summary.getOwner().getId() != userId) {
            throw new CustomException(ErrorCode.FORBIDDEN_SUMMARY_ACCESS);
        }

        summary.setTitle(request.getTitle().trim());
        summary.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        aiSummaryRepository.save(summary);
    }

    @Override
    public AiSummaryResponseDto getSummaryDetail(int userId, int summaryId) {
        AiSummary summary = aiSummaryRepository.findById(summaryId)
            .orElseThrow(() -> new CustomException(ErrorCode.SUMMARY_NOT_FOUND));

        if (summary.getOwner() == null || summary.getOwner().getId() != userId) {
            throw new CustomException(ErrorCode.FORBIDDEN_SUMMARY_ACCESS);
        }

        List<AiSummaryDocument> links = aiSummaryDocumentRepository.findBySummary_Id(summaryId);

        List<DocsItem> docsItems = links.stream().map(link -> {
            int docId = link.getDocument().getId();
            String key = documentService.getDocumentKeyIfAllowed(userId, docId);
            String url = storageService.presignDocumentViewUrl(key, Duration.ofMinutes(40));
            return DocsItem.builder()
                .docsId(docId)
                .url(url)
                .build();
        }).toList();

        return AiSummaryResponseDto.builder()
            .summaryJson(summary.getSummaryJson())
            .docses(docsItems)
            .build();
    }
}