package com.foureyes.moai.backend.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.foureyes.moai.backend.domain.ai.dto.SummaryDto;
import com.foureyes.moai.backend.domain.ai.dto.request.CreateAiSummaryRequest;
import com.foureyes.moai.backend.domain.ai.dto.request.EditAiSummaryRequest;
import com.foureyes.moai.backend.domain.ai.dto.response.AiSummaryResponseDto;
import com.foureyes.moai.backend.domain.ai.dto.response.CreateAiSummaryResponse;
import com.foureyes.moai.backend.domain.ai.dto.response.DashboardSummariesResponse;
import com.foureyes.moai.backend.domain.ai.dto.response.SidebarSummariesResponse;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public interface AiService {

    /**
     * 입력: int ownerId, CreateAiSummaryRequest req
     * 출력: CreateAiSummaryResponse
     * 기능: 여러 문서 ID를 받아 요약본 레코드를 생성하고 문서와 연결합니다.
     */
    CreateAiSummaryResponse createSummary(int ownerId, CreateAiSummaryRequest req);

    /**
     * 입력: int ownerId
     * 출력: DashboardSummariesResponse
     * 기능: 소유자의 요약본 목록(대시보드)을 조회합니다.
     */
    DashboardSummariesResponse getDashboardList(int ownerId);

    /**
     * 입력: int ownerId
     * 출력: SidebarSummariesResponse
     * 기능: 소유자의 요약본 목록(사이드바)을 스터디별로 그룹핑하여 조회합니다.
     */
    SidebarSummariesResponse getSidebarList(int ownerId);

    /**
     * 입력: int ownerId, int summaryId
     * 출력: void
     * 기능: 요약본을 삭제합니다. 소유자만 삭제할 수 있습니다.
     */
    void deleteSummary(int ownerId, int summaryId);

    /**
     * 입력: int userId, int summaryId, EditAiSummaryRequest request
     * 출력: void
     * 기능: 요약본의 제목과 설명을 수정합니다.
     */
    void editSummary(int userId, int summaryId, EditAiSummaryRequest request);

    /**
     * 입력: int userId, int summaryId
     * 출력: AiSummaryResponseDto
     * 기능: 요약본 JSON과 연결된 문서들의 프리사인드 뷰 URL을 조회합니다.
     */
    AiSummaryResponseDto getSummaryDetail(int userId, int summaryId);
}
