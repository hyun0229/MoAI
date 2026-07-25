package com.foureyes.moai.backend.domain.ai.controller;

import com.foureyes.moai.backend.auth.jwt.JwtTokenProvider;
import com.foureyes.moai.backend.commons.util.StorageService;
import com.foureyes.moai.backend.domain.ai.dto.request.CreateAiSummaryRequest;
import com.foureyes.moai.backend.domain.ai.dto.request.EditAiSummaryRequest;
import com.foureyes.moai.backend.domain.ai.dto.response.AiSummaryResponseDto;
import com.foureyes.moai.backend.domain.ai.dto.response.CreateAiSummaryResponse;
import com.foureyes.moai.backend.domain.ai.dto.response.DashboardSummariesResponse;
import com.foureyes.moai.backend.domain.ai.dto.response.SidebarSummariesResponse;
import com.foureyes.moai.backend.domain.ai.service.AiService;
import com.foureyes.moai.backend.domain.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;
    private final JwtTokenProvider jwtTokenProvider;
    private int extractUserIdFromToken(String bearerToken) {
        String token = bearerToken.replaceFirst("^Bearer ", "").trim();
        return jwtTokenProvider.getUserId(token);
    }

    @Operation(
        summary = "AI 요약본 생성",
        description = """
            여러 문서 ID를 받아 요약본 레코드를 생성하고 문서와 연결합니다.
            모델/프롬프트 값은 그대로 저장되며, 실제 모델 호출은 추후 연결합니다.
            """
    )
    @PostMapping("/create")
    public ResponseEntity<CreateAiSummaryResponse> create(
        @Parameter(hidden = true) @RequestHeader("Authorization") String bearerToken,
        @RequestBody CreateAiSummaryRequest req
    ) {
        int ownerId = extractUserIdFromToken(bearerToken);
        var resp = aiService.createSummary(ownerId, req);
        return ResponseEntity.status(201).body(resp);
    }

    @Operation(summary = "내 요약본 목록(대시보드)")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummariesResponse> dashboard(
        @Parameter(hidden = true) @RequestHeader("Authorization") String bearerToken
    ) {
        int ownerId = extractUserIdFromToken(bearerToken);
        return ResponseEntity.ok(aiService.getDashboardList(ownerId));
    }

    @Operation(summary = "내 요약본 목록(사이드바)")
    @GetMapping("/sidebar")
    public ResponseEntity<SidebarSummariesResponse> sidebar(
        @Parameter(hidden = true) @RequestHeader("Authorization") String bearerToken
    ) {
        int ownerId = extractUserIdFromToken(bearerToken);
        return ResponseEntity.ok(aiService.getSidebarList(ownerId));
    }

    @Operation(
        summary = "AI 요약본 삭제",
        description = "요약본의 소유자만 삭제할 수 있습니다."
    )
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteSummary(
        @Parameter(hidden = true) @RequestHeader("Authorization") String bearerToken,
        @Parameter(description = "요약본 ID", example = "123") @PathVariable int id
    ) {
        int ownerId = extractUserIdFromToken(bearerToken);
        aiService.deleteSummary(ownerId, id);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "AI 요약본 수정",
        description = "제목과 설명을 수정합니다."
    )
    @PatchMapping("/edit/{id}")
    public ResponseEntity<Void> editAiSummary(
        @Parameter(hidden = true)
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable int id,
        @Valid @RequestBody EditAiSummaryRequest request
    ) {
        int userId = extractUserIdFromToken(bearerToken);
        aiService.editSummary(userId, id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "AI 요약본 상세 조회",
        description = "요약본 JSON과 연결된 문서들의 프리사인드 뷰 URL을 반환합니다."
    )
    @GetMapping("/detail/{id}")
    public ResponseEntity<AiSummaryResponseDto> getDetail(
        @Parameter(hidden = true) @RequestHeader("Authorization") String bearerToken,
        @PathVariable("id") int id
    ) {
        int userId = extractUserIdFromToken(bearerToken);
        return ResponseEntity.ok(aiService.getSummaryDetail(userId, id));
    }
}
