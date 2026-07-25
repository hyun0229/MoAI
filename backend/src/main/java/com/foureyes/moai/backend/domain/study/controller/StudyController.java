package com.foureyes.moai.backend.domain.study.controller;

import com.foureyes.moai.backend.domain.study.dto.request.*;
import com.foureyes.moai.backend.domain.study.dto.response.*;
import com.foureyes.moai.backend.domain.study.service.StudyService;
import com.foureyes.moai.backend.domain.user.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Study API", description = "스터디 관리 기능")
@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyController {

    private static final Logger log = LoggerFactory.getLogger(StudyController.class);
    private final StudyService studyService;

    @Operation(
        summary = "스터디 생성",
        description = "스터디 이름, 설명, 대표 이미지를 등록하여 새 스터디를 생성합니다",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyResponseDto> createStudy(
        @AuthenticationPrincipal CustomUserDetails user,
        @Valid @ModelAttribute CreateStudyRequest request) {
        if (user == null) return ResponseEntity.status(401).build();
        log.info("스터디 생성 API 호출: 스터디명={}", request.getName());
        StudyResponseDto response = studyService.createStudy(user.getId(), request);
        log.info("스터디 생성 완료: studyId={}, userId={}", response.getId(), user.getId());
        return ResponseEntity.status(201).body(response);
    }

    @Operation(
        summary = "스터디 가입 요청 전송",
        description = "사용자가 특정 스터디에 가입 요청을 전송합니다",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/join")
    public ResponseEntity<String> sendJoinRequest(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestParam("study_id") int studyId
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        log.info("스터디 가입 요청 API 호출: studyId={}", studyId);
        studyService.sendJoinRequest(user.getId(), studyId);
        log.info("스터디 가입 요청 완료: userId={}, studyId={}", user.getId(), studyId);
        return ResponseEntity.ok("OK");
    }

    @Operation(
        summary = "스터디 멤버 목록 조회",
        description = "유저가 참여 중인 스터디의 모든 멤버 이름과 역할을 반환합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{study_id}/members")
    public ResponseEntity<List<StudyMemberListResponseDto>> getMembers(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable("study_id") int studyId
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        log.info("스터디 멤버 목록 조회 API 호출: studyId={}", studyId);
        List<StudyMemberListResponseDto> members = studyService.getStudyMembers(user.getId(), studyId);
        log.info("스터디 멤버 목록 조회 완료: studyId={}, memberCount={}", studyId, members.size());
        return ResponseEntity.ok(members);
    }

    @Operation(
        summary = "참여/승인 대기 중인 스터디 조회",
        description = "유저가 가입 혹은 신청한 스터디 목록을 반환합니다",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/all")
    public ResponseEntity<List<StudyListResponseDto>> listUserStudies(
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        log.info("사용자 스터디 목록 조회 API 호출");
        List<StudyListResponseDto> studies = studyService.getUserStudies(user.getId());
        log.info("사용자 스터디 목록 조회 완료: userId={}, studyCount={}", user.getId(), studies.size());
        return ResponseEntity.ok(studies);
    }

    @Operation(
        summary     = "스터디 탈퇴",
        description = "유저가 스터디에서 탈퇴하여 상태를 LEFT 로 변경합니다.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/leave")
    public ResponseEntity<Void> leaveStudy(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestBody StudyIdRequestDto request
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        studyService.leaveStudy(user.getId(), request.getStudyGroupId());
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary     = "스터디 멤버 삭제(강제탈퇴)",
        description = "관리자가 특정 유저를 스터디에서 강제 탈퇴시킵니다.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/delete")
    public ResponseEntity<Void> deleteMember(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestBody StudyMemberDeleteRequestDto request
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        studyService.deleteMember(user.getId(), request.getStudyId(), request.getUserId());
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary     = "스터디 멤버 권한 변경(관리자 지정)",
        description = "관리자가 특정 유저의 역할을 변경합니다. ADMIN 지정 시 기존 ADMIN은 MEMBER로 변경됩니다.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/designate")
    public ResponseEntity<Void> designateMember(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestBody StudyMemberRoleChangeRequestDto request
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        studyService.changeMemberRole(user.getId(), request.getStudyId(), request.getUserId(), request.getRole());
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary     = "스터디 가입 요청 거절",
        description = "관리자가 가입 요청 중인 유저를 거절하여 DB에서 멤버십을 삭제합니다.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/reject")
    public ResponseEntity<Void> rejectJoin(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestBody StudyMemberRejectRequestDto request
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        studyService.rejectJoinRequest(user.getId(), request.getStudyId(), request.getUserId());
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary     = "가입 요청 승인",
        description = "관리자가 스터디 가입 요청을 승인하고 해당 유저의 상태를 PENDING→APPROVED로 변경합니다",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/accept")
    public ResponseEntity<Void> acceptJoin(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestBody AcceptJoinRequestDto request
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        studyService.acceptJoinRequest(user.getId(), request.getStudyId(), request.getUserId(), request.getRole());
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary     = "가입 요청 목록 조회(관리자용)",
        description = "관리자가 자신의 스터디에 온 모든 가입 요청을 조회합니다.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/list/management")
    public ResponseEntity<List<JoinRequestResponseDto>> getPendingRequests(
        @AuthenticationPrincipal CustomUserDetails user,
        @Parameter(description = "스터디 ID", example = "101")
        @RequestParam("studyId") int studyId
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        List<JoinRequestResponseDto> requests = studyService.getPendingJoinRequests(user.getId(), studyId);
        return ResponseEntity.ok(requests);
    }

    @Operation(
        summary = "참여 중인 스터디 목록 조회",
        description = "현재 로그인한 사용자가 참여 중인 모든 스터디 정보를 반환합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/list")
    public ResponseEntity<List<JoinStudyListResponseDto>> getJoinedStudies(
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        List<JoinStudyListResponseDto> studies = studyService.getJoinedStudies(user.getId());
        return ResponseEntity.ok(studies);
    }

    @Operation(
        summary = "스터디 정보 조회(디테일 페이지, hashId 기반)",
        description = "hashId로 스터디 상세 정보를 조회합니다. 상태/역할은 현재 사용자 기준입니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/detail")
    public ResponseEntity<StudyDetailResponseDto> getStudyDetailByHash(
        @AuthenticationPrincipal CustomUserDetails user,
        @Parameter(description = "라우팅/공유용 해시 ID", example = "jR4kd8Lz", required = true, schema = @Schema(type = "string"))
        @RequestParam("hashId") String hashId
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        StudyDetailResponseDto dto = studyService.getStudyDetailByHashId(user.getId(), hashId);
        return ResponseEntity.ok(dto);
    }

    @Operation(
        summary = "스터디 공지사항 조회",
        description = "studyId로 해당 스터디의 공지사항을 조회합니다. 승인된 멤버만 접근 가능합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/notice")
    public ResponseEntity<StudyNoticeResponseDto> getStudyNotice(
        @AuthenticationPrincipal CustomUserDetails user,
        @Parameter(description = "스터디 ID", example = "101", required = true)
        @RequestParam("studyId") int studyId
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        StudyNoticeResponseDto dto = studyService.getStudyNotice(user.getId(), studyId);
        return ResponseEntity.ok(dto);
    }

    @Operation(
        summary = "스터디 공지사항 수정",
        description = "승인된 관리자만 공지사항을 수정할 수 있습니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/notice")
    public ResponseEntity<String> updateNotice(
        @AuthenticationPrincipal CustomUserDetails user,
        @Valid @RequestBody UpdateStudyNoticeRequestDto request
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        studyService.updateStudyNotice(user.getId(), request.getStudyId(), request.getNotice());
        return ResponseEntity.ok("수정완료");
    }

    @Operation(
            summary = "스터디 수정",
            description = "승인된 관리자만 수정할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping(path = "/{study_id}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateGroup(
            @PathVariable("study_id") int studyId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @ModelAttribute UpdateStudyRequestDto request
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        studyService.updateStudyGroup(user.getId(), studyId, request);
        return ResponseEntity.ok("수정완료");
    }
}