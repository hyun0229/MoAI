package com.foureyes.moai.backend.domain.ai.controller;

import com.foureyes.moai.backend.domain.ai.dto.request.CreateAiSummaryRequest;
import com.foureyes.moai.backend.domain.ai.dto.request.EditAiSummaryRequest;
import com.foureyes.moai.backend.domain.ai.dto.response.AiSummaryResponseDto;
import com.foureyes.moai.backend.domain.ai.dto.response.CreateAiSummaryResponse;
import com.foureyes.moai.backend.domain.ai.dto.response.DashboardSummariesResponse;
import com.foureyes.moai.backend.domain.ai.dto.response.SidebarSummariesResponse;
import com.foureyes.moai.backend.domain.ai.service.AiService;
import com.foureyes.moai.backend.domain.user.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;

    @Operation(
        summary = "AI 요약본 생성",
        description = """
            여러 문서 ID를 받아 요약본 레코드를 생성하고 문서와 연결합니다.
            """
    )
    @PostMapping("/create")
    public ResponseEntity<CreateAiSummaryResponse> create(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestBody CreateAiSummaryRequest req
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        var resp = aiService.createSummary(user.getId(), req);
        return ResponseEntity.status(201).body(resp);
    }

    @Operation(summary = "내 요약본 목록(대시보드)")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummariesResponse> dashboard(
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(aiService.getDashboardList(user.getId()));
    }

    @Operation(summary = "내 요약본 목록(사이드바)")
    @GetMapping("/sidebar")
    public ResponseEntity<SidebarSummariesResponse> sidebar(
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(aiService.getSidebarList(user.getId()));
    }

    @Operation(
        summary = "AI 요약본 삭제",
        description = "요약본의 소유자만 삭제할 수 있습니다."
    )
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteSummary(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable int id
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        aiService.deleteSummary(user.getId(), id);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "AI 요약본 수정",
        description = "제목과 설명을 수정합니다."
    )
    @PatchMapping("/edit/{id}")
    public ResponseEntity<Void> editAiSummary(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable int id,
        @Valid @RequestBody EditAiSummaryRequest request
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        aiService.editSummary(user.getId(), id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "AI 요약본 상세 조회",
        description = "요약본 JSON과 연결된 문서들의 프리사인드 뷰 URL을 반환합니다."
    )
    @GetMapping("/detail/{id}")
    public ResponseEntity<AiSummaryResponseDto> getDetail(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable("id") int id
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(aiService.getSummaryDetail(user.getId(), id));
    }
}