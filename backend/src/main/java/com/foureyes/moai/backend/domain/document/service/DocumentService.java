package com.foureyes.moai.backend.domain.document.service;

import com.foureyes.moai.backend.domain.document.dto.request.CreateDocumentRequest;
import com.foureyes.moai.backend.domain.document.dto.request.EditDocumentRequest;
import com.foureyes.moai.backend.domain.document.dto.response.DocumentListItemDto;
import com.foureyes.moai.backend.domain.document.dto.response.DocumentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

public interface DocumentService {
    /**
     * 입력: int uploaderId, CreateDocumentRequest req
     * 출력: DocumentResponseDto
     * 기능: PDF를 비공개 버킷에 업로드하고 문서 정보를 저장합니다.
     */
    DocumentResponseDto uploadDocument(int uploaderId, CreateDocumentRequest req) throws IOException;

    /**
     * 입력: int userId, int documentId
     * 출력: String(파일 키)
     * 기능: 문서 접근 권한을 확인한 후 파일 키를 반환합니다.
     */
    String getDocumentKeyIfAllowed(int userId, int documentId);

    /**
     * 입력: int userId, int documentId, EditDocumentRequest req
     * 출력: void
     * 기능: 문서 접근 권한 확인 후 제목/설명/카테고리를 수정합니다.
     */
    void updateDocument(int userId, int documentId, EditDocumentRequest req);

    /**
     * 입력: int userId, int studyId
     * 출력: List<DocumentListItemDto>
     * 기능: 스터디별 문서 목록을 조회합니다.
     */
    List<DocumentListItemDto> getDocuments(int userId, int studyId);

    /**
     * 입력: int userId, int documentId
     * 출력: void
     * 기능: 문서를 삭제합니다.
     */
    void deleteDocument(int userId, int documentId);

}
