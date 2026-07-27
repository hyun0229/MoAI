package com.foureyes.moai.backend.domain.document.controller;

import com.foureyes.moai.backend.commons.util.StorageService;
import com.foureyes.moai.backend.domain.document.dto.CategoryItemDto;
import com.foureyes.moai.backend.domain.document.dto.request.CreateCategoryRequest;
import com.foureyes.moai.backend.domain.document.dto.request.CreateDocumentRequest;
import com.foureyes.moai.backend.domain.document.dto.request.EditCategoryRequest;
import com.foureyes.moai.backend.domain.document.dto.request.EditDocumentRequest;
import com.foureyes.moai.backend.domain.document.dto.response.DocumentListItemDto;
import com.foureyes.moai.backend.domain.document.dto.response.DocumentResponseDto;
import com.foureyes.moai.backend.domain.document.dto.response.PresignedUrlResponse;
import com.foureyes.moai.backend.domain.document.service.CategoryService;
import com.foureyes.moai.backend.domain.document.service.DocumentService;
import com.foureyes.moai.backend.domain.user.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Tag(name = "docs API", description = "공부 자료 관리 기능")
@RestController
@RequestMapping("/ref")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    private final StorageService storageService;
    private final CategoryService categoryService;

    @Operation(
        summary = "공부 자료 업로드",
        description = "PDF를 비공개 버킷에 업로드하고 key를 저장합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    /**
     * 입력: CustomUserDetails user, CreateDocumentRequest request
     * 출력: DocumentResponseDto
     * 기능: PDF를 비공개 버킷에 업로드하고 문서 정보를 저장합니다.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponseDto> upload(
        @AuthenticationPrincipal CustomUserDetails user,
        @ModelAttribute @Valid CreateDocumentRequest request
    ) throws IOException {
        if (user == null) return ResponseEntity.status(401).build();
        DocumentResponseDto dto = documentService.uploadDocument(user.getId(), request);
        return ResponseEntity.status(201).body(dto);
    }

    @Operation(
        summary = "단일 문서 다운로드 URL 발급",
        description = "문서 접근 권한 확인 후 Pre-signed URL을 발급합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    /**
     * 입력: CustomUserDetails user, int id
     * 출력: PresignedUrlResponse
     * 기능: 문서 접근 권한 확인 후 다운로드용 Pre-signed URL을 발급합니다.
     */
    @GetMapping("/download-url/{id}")
    public ResponseEntity<PresignedUrlResponse> getDownloadUrl(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable int id
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        String key = documentService.getDocumentKeyIfAllowed(user.getId(), id);
        String url = storageService.presignDocumentDownloadUrl(key, Duration.ofMinutes(10));
        return ResponseEntity.ok(new PresignedUrlResponse(url));
    }

    @Operation(
        summary = "단일 문서 View URL 발급",
        description = "문서 접근 권한 확인 후 Pre-signed URL을 발급합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    /**
     * 입력: CustomUserDetails user, int id
     * 출력: PresignedUrlResponse
     * 기능: 문서 접근 권한 확인 후 조회용 Pre-signed URL을 발급합니다.
     */
    @GetMapping("/view-url/{id}")
    public ResponseEntity<PresignedUrlResponse> getViewUrl(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable int id
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        String key = documentService.getDocumentKeyIfAllowed(user.getId(), id);
        String url = storageService.presignDocumentViewUrl(key, Duration.ofMinutes(40));
        return ResponseEntity.ok(new PresignedUrlResponse(url));
    }

    @Operation(
        summary = "공부 자료 수정",
        description = "문서 접근 권한 확인 후 제목/설명/카테고리를 수정합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    /**
     * 입력: CustomUserDetails user, int id, EditDocumentRequest request
     * 출력: void
     * 기능: 문서 접근 권한 확인 후 제목/설명/카테고리를 수정합니다.
     */
    @PatchMapping("/edit/{id}")
    public ResponseEntity<Void> editDocument(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable int id,
        @RequestBody EditDocumentRequest request
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        documentService.updateDocument(user.getId(), id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "공부 자료 목록 조회",
        description = "스터디별 문서 목록을 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    /**
     * 입력: CustomUserDetails user, int studyId
     * 출력: List<DocumentListItemDto>
     * 기능: 스터디별 문서 목록을 조회합니다.
     */
    @GetMapping("/list")
    public ResponseEntity<List<DocumentListItemDto>> listDocuments(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestParam int studyId
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        List<DocumentListItemDto> result = documentService.getDocuments(user.getId(), studyId);
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "공부 자료 삭제",
        description = "파일 ID에 해당하는 공부 자료를 삭제합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    /**
     * 입력: CustomUserDetails user, int id
     * 출력: void
     * 기능: 파일 ID에 해당하는 공부 자료를 삭제합니다.
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteDocument(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable int id
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        documentService.deleteDocument(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "커스텀 카테고리 생성",
        description = "스터디 관리자만 커스텀 카테고리를 생성할 수 있습니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    /**
     * 입력: CustomUserDetails user, CreateCategoryRequest req
     * 출력: void
     * 기능: 스터디 관리자가 커스텀 카테고리를 생성합니다.
     */
    @PostMapping("/categories/create")
    public ResponseEntity<Void> create(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestBody CreateCategoryRequest req
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        categoryService.createCategory(user.getId(), req);
        return ResponseEntity.status(201).build();
    }

    @Operation(
        summary = "커스텀 카테고리 수정",
        description = "스터디 관리자만 카테고리 정보를 수정할 수 있습니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    /**
     * 입력: CustomUserDetails user, int id, EditCategoryRequest req
     * 출력: void
     * 기능: 스터디 관리자가 카테고리 정보를 수정합니다.
     */
    @PatchMapping("/categories/edit/{id}")
    public ResponseEntity<Void> edit(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable int id,
        @RequestBody EditCategoryRequest req
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        categoryService.editCategory(user.getId(), id, req);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "커스텀 카테고리 삭제",
        description = "스터디 관리자만 카테고리를 삭제할 수 있습니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    /**
     * 입력: CustomUserDetails user, int id
     * 출력: void
     * 기능: 스터디 관리자가 커스텀 카테고리를 삭제합니다.
     */
    @DeleteMapping("/categories/delete/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable int id
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        categoryService.deleteCategory(user.getId(), id);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "커스텀 카테고리 조회",
        description = "해당 스터디의 커스텀 카테고리 목록을 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    /**
     * 입력: CustomUserDetails user, int studyId
     * 출력: List<CategoryItemDto>
     * 기능: 해당 스터디의 커스텀 카테고리 목록을 조회합니다.
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryItemDto>> list(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestParam int studyId
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(categoryService.getCategories(user.getId(), studyId));
    }
}