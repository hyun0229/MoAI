package com.foureyes.moai.backend.domain.document.service;

import com.foureyes.moai.backend.commons.exception.CustomException;
import com.foureyes.moai.backend.commons.exception.ErrorCode;
import com.foureyes.moai.backend.commons.util.StorageService;
import com.foureyes.moai.backend.domain.ai.repository.AiSummaryDocumentRepository;
import com.foureyes.moai.backend.domain.document.dto.response.DocumentRow;
import com.foureyes.moai.backend.domain.document.dto.request.CreateDocumentRequest;
import com.foureyes.moai.backend.domain.document.dto.request.EditDocumentRequest;
import com.foureyes.moai.backend.domain.document.dto.response.DocumentListItemDto;
import com.foureyes.moai.backend.domain.document.dto.response.DocumentResponseDto;
import com.foureyes.moai.backend.domain.document.entity.Category;
import com.foureyes.moai.backend.domain.document.entity.Document;
import com.foureyes.moai.backend.domain.document.entity.DocumentCategory;
import com.foureyes.moai.backend.domain.document.repository.CategoryRepository;
import com.foureyes.moai.backend.domain.document.repository.DocumentCategoryRepository;
import com.foureyes.moai.backend.domain.document.repository.DocumentRepository;
import com.foureyes.moai.backend.domain.study.entity.StudyGroup;
import com.foureyes.moai.backend.domain.study.entity.StudyMembership;
import com.foureyes.moai.backend.domain.study.repository.StudyMembershipRepository;
import com.foureyes.moai.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService{
    private final CategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;
    private final DocumentCategoryRepository documentCategoryRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final StudyMembershipRepository studyMembershipRepository;
    private final AiSummaryDocumentRepository aiSummaryDocumentRepository;

    /**
     * 입력: int uploaderId, CreateDocumentRequest req
     * 출력: DocumentResponseDto
     * 기능: PDF를 비공개 버킷에 업로드하고 문서 정보를 저장합니다.
     */
    @Override
    @Transactional
    public DocumentResponseDto uploadDocument(int uploaderId, CreateDocumentRequest req) throws IOException {
        if (req.getFile() == null || req.getFile().isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        if (!org.springframework.util.StringUtils.hasText(req.getTitle())) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        if (req.getCategoryId() == null || req.getCategoryId().isEmpty()) {
            throw new IllegalArgumentException("카테고리는 최소 1개 이상 선택해야 합니다.");
        }

        List<Integer> catIds = req.getCategoryId().stream()
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();

        var categories = categoryRepository.findAllById(catIds);
        if (categories.size() != catIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 카테고리가 포함되어 있습니다.");
        }

        int studyId = categories.get(0).getStudyGroup().getId();
        boolean sameStudy = categories.stream()
            .allMatch(c -> c.getStudyGroup().getId() == studyId);
        if (!sameStudy) {
            throw new IllegalArgumentException("서로 다른 스터디의 카테고리가 섞여 있습니다.");
        }

        String storedKey = storageService.uploadDocument(req.getFile(), studyId);

        Document doc = Document.builder()
            .studyGroup(categories.get(0).getStudyGroup())
            .uploader(userRepository.getReferenceById(uploaderId))
            .title(req.getTitle().trim())
            .description(req.getDescription())
            .fileKey(storedKey)
            .build();
        Document saved = documentRepository.save(doc);

        if (!categories.isEmpty()) {
            List<DocumentCategory> links = categories.stream()
                .map(c -> DocumentCategory.builder()
                    .document(saved)
                    .category(c)
                    .build())
                .toList();
            documentCategoryRepository.saveAll(links);
        }

        return DocumentResponseDto.builder()
            .id(saved.getId())
            .studyId(studyId)
            .title(saved.getTitle())
            .description(saved.getDescription())
            .fileKey(saved.getFileKey())
            .categoryIds(catIds)
            .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : java.time.LocalDateTime.now())
            .build();
    }

    /**
     * 입력: int userId, int documentId
     * 출력: String(파일 키)
     * 기능: 문서 접근 권한을 확인한 후 파일 키를 반환합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public String getDocumentKeyIfAllowed(int userId, int documentId) {
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

        boolean hasAccess = studyMembershipRepository.existsByUserIdAndStudyGroup_IdAndStatus(
            userId, doc.getStudyGroup().getId(), StudyMembership.Status.APPROVED);

        if (!hasAccess) throw new CustomException(ErrorCode.FORBIDDEN_DOCUMENT_ACCESS);
        return doc.getFileKey();
    }

    /**
     * 입력: int userId, int documentId, EditDocumentRequest req
     * 출력: void
     * 기능: 문서 접근 권한 확인 후 제목/설명/카테고리를 수정합니다.
     */
    @Override
    @Transactional
    public void updateDocument(int userId, int documentId, EditDocumentRequest req) {
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

        boolean allowed = studyMembershipRepository.existsByUserIdAndStudyGroup_IdAndStatus(
            userId, doc.getStudyGroup().getId(), StudyMembership.Status.APPROVED);
        if (!allowed) throw new CustomException(ErrorCode.FORBIDDEN_DOCUMENT_ACCESS);

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            doc.setTitle(req.getTitle().trim());
        }
        if (req.getDescription() != null) {
            doc.setDescription(req.getDescription().trim());
        }

        if (req.getCategoryIdList() != null) {

            List<Category> categories = req.getCategoryIdList().isEmpty()
                ? List.of()
                : categoryRepository.findAllById(req.getCategoryIdList());

            if (categories.size() != req.getCategoryIdList().size()) {
                throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
            }
            boolean allSameStudy = categories.stream()
                .allMatch(c -> c.getStudyGroup().getId() == doc.getStudyGroup().getId());
            if (!allSameStudy) throw new CustomException(ErrorCode.INVALID_REQUEST);

            documentCategoryRepository.deleteByDocument_Id(doc.getId());
            documentCategoryRepository.flush();

            if (!categories.isEmpty()) {
                List<DocumentCategory> links = new ArrayList<>(categories.size());
                for (Category c : categories) {
                    links.add(DocumentCategory.builder()
                        .document(doc)
                        .category(c)
                        .build());
                }
                documentCategoryRepository.saveAll(links);
            }
        }
    }

    /**
     * 입력: int userId, int studyId
     * 출력: List<DocumentListItemDto>
     * 기능: 스터디별 문서 목록을 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DocumentListItemDto> getDocuments(int userId, int studyId) {
        boolean allowed = studyMembershipRepository.existsByUserIdAndStudyGroup_IdAndStatus(
            userId, studyId, StudyMembership.Status.APPROVED);
        if (!allowed) throw new CustomException(ErrorCode.FORBIDDEN_DOCUMENT_ACCESS);

        List<DocumentRow> rows = documentRepository.findListByStudyId(studyId);
        if (rows.isEmpty()) return List.of();

        List<Integer> docIds = rows.stream().map(DocumentRow::getId).toList();
        Map<Integer, List<String>> catMap = new HashMap<>();
        for (Object[] r : documentCategoryRepository.findNamesByDocumentIds(docIds)) {
            Integer docId = (Integer) r[0];
            String name = (String) r[1];
            catMap.computeIfAbsent(docId, k -> new ArrayList<>()).add(name);
        }

        return rows.stream()
            .map(r -> new DocumentListItemDto(
                r.getId(),
                r.getTitle(),
                r.getDescription(),
                catMap.getOrDefault(r.getId(), List.of()),
                r.getProfileImageUrl(),
                r.getName(),
                r.getUpdatedAt(),
                r.getCreatedAt()
            ))
            .toList();
    }

    /**
     * 입력: int userId, int documentId
     * 출력: void
     * 기능: 업로더 본인 또는 스터디 관리자가 문서를 삭제합니다.
     */
    @Override
    @Transactional
    public void deleteDocument(int userId, int documentId) {
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

        boolean isUploader = doc.getUploader().getId() == userId;
        boolean isAdmin = studyMembershipRepository.existsByUserIdAndStudyGroup_IdAndRole(
            userId, doc.getStudyGroup().getId(), StudyMembership.Role.ADMIN
        );
        if (!isUploader && !isAdmin) {
            throw new CustomException(ErrorCode.FORBIDDEN_DOCUMENT_ACCESS);
        }

        documentCategoryRepository.deleteByDocument_Id(doc.getId());
        aiSummaryDocumentRepository.deleteByDocument_Id(doc.getId());

        storageService.deleteDocumentObject(doc.getFileKey());

        documentRepository.delete(doc);
    }

}