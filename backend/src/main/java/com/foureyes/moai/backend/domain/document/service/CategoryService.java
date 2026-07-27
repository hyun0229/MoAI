package com.foureyes.moai.backend.domain.document.service;

import com.foureyes.moai.backend.domain.document.dto.CategoryItemDto;
import com.foureyes.moai.backend.domain.document.dto.request.CreateCategoryRequest;
import com.foureyes.moai.backend.domain.document.dto.request.EditCategoryRequest;

import java.util.List;

public interface CategoryService {
    /**
     * 입력: int userId, CreateCategoryRequest req
     * 출력: void
     * 기능: 스터디 관리자가 커스텀 카테고리를 생성합니다.
     */
    void createCategory(int userId, CreateCategoryRequest req);

    /**
     * 입력: int userId, int categoryId, EditCategoryRequest req
     * 출력: void
     * 기능: 스터디 관리자가 카테고리 정보를 수정합니다.
     */
    void editCategory(int userId, int categoryId, EditCategoryRequest req);

    /**
     * 입력: int userId, int categoryId
     * 출력: void
     * 기능: 스터디 관리자가 카테고리를 삭제합니다.
     */
    void deleteCategory(int userId, int categoryId);

    /**
     * 입력: int userId, int studyId
     * 출력: List<CategoryItemDto>
     * 기능: 해당 스터디의 커스텀 카테고리 목록을 조회합니다.
     */
    List<CategoryItemDto> getCategories(int userId, int studyId);
}
