package com.foureyes.moai.backend.domain.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foureyes.moai.backend.domain.ai.dto.request.CreateAiSummaryRequest;
import com.foureyes.moai.backend.domain.ai.entity.AiSummary;
import com.foureyes.moai.backend.domain.ai.entity.AiSummaryDocument;
import com.foureyes.moai.backend.domain.ai.repository.AiSummaryDocumentRepository;
import com.foureyes.moai.backend.domain.ai.repository.AiSummaryRepository;
import com.foureyes.moai.backend.domain.document.entity.Document;
import com.foureyes.moai.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * AI 호출(오래 걸리고 트랜잭션이 필요 없는 부분)과
 * DB 저장(짧고 트랜잭션이 필요한 부분)의 경계를 분리하기 위해 별도 Bean으로 뺐다.
 * 이렇게 하면 AiServiceImpl.createSummary()가 AI 응답을 기다리는 동안
 * DB 커넥션을 붙잡고 있지 않게 된다.
 */
@Service
@RequiredArgsConstructor
public class AiSummaryPersistenceService {

    private final AiSummaryRepository aiSummaryRepository;
    private final AiSummaryDocumentRepository aiSummaryDocumentRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiSummary save(User owner, CreateAiSummaryRequest req, String summaryJson, List<Document> docs) throws Exception {
        AiSummary summary = AiSummary.builder()
            .owner(owner)
            .title(Optional.ofNullable(req.getTitle()).orElse("").trim())
            .description(Optional.ofNullable(req.getDescription()).orElse("").trim())
            .modelType(req.getModelType())
            .promptType(Optional.ofNullable(req.getPromptType()).orElse("").trim())
            .summaryJson(objectMapper.readTree(summaryJson))
            .build();
        aiSummaryRepository.save(summary);

        for (Document d : docs) {
            if (!aiSummaryDocumentRepository.existsBySummary_IdAndDocument_Id(summary.getId(), d.getId())) {
                aiSummaryDocumentRepository.save(
                    AiSummaryDocument.builder().summary(summary).document(d).build()
                );
            }
        }
        return summary;
    }
}