package com.foureyes.moai.backend.domain.document.service;

import com.foureyes.moai.backend.commons.exception.CustomException;
import com.foureyes.moai.backend.commons.exception.ErrorCode;
import com.foureyes.moai.backend.domain.document.dto.CategoryItemDto;
import com.foureyes.moai.backend.domain.document.dto.request.CreateCategoryRequest;
import com.foureyes.moai.backend.domain.document.dto.request.EditCategoryRequest;
import com.foureyes.moai.backend.domain.document.entity.Category;
import com.foureyes.moai.backend.domain.document.repository.CategoryRepository;
import com.foureyes.moai.backend.domain.document.repository.DocumentCategoryRepository;
import com.foureyes.moai.backend.domain.study.entity.StudyGroup;
import com.foureyes.moai.backend.domain.study.entity.StudyMembership;
import com.foureyes.moai.backend.domain.study.repository.StudyGroupRepository;
import com.foureyes.moai.backend.domain.study.repository.StudyMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final StudyMembershipRepository studyMembershipRepository;
    private final DocumentCategoryRepository documentCategoryRepository;

    /** 스터디 관리자/대리자 권한을 확인하는 공통 메서드 */
    private void assertLeader(int userId, int studyId) {
        var leaderRoles = List.of(StudyMembership.Role.ADMIN, StudyMembership.Role.DELEGATE);
        boolean ok = studyMembershipRepository
            .existsByUserIdAndStudyGroup_IdAndRoleInAndStatus(
                userId, studyId, leaderRoles, StudyMembership.Status.APPROVED
            );
        if (!ok) throw new CustomException(ErrorCode.FORBIDDEN);
    }

    /**
     * 입력: int userId, CreateCategoryRequest req
     * 출력: void
     * 기능: 스터디 관리자가 커스텀 카테고리를 생성합니다.
     */
    @Override
    public void createCategory(int userId, CreateCategoryRequest req) {
        if (req.getStudyId() == null || req.getCategoryName() == null || req.getCategoryName().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        int studyId = req.getStudyId();
        assertLeader(userId, studyId);

        if (categoryRepository.existsByStudyGroup_IdAndName(studyId, req.getCategoryName().trim())) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
        }

        StudyGroup sg = studyGroupRepository.findById(studyId)
            .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));

        Category c = Category.builder()
            .studyGroup(sg)
            .name(req.getCategoryName().trim())
            .build();

        categoryRepository.save(c); // 반환값 필요 없으므로 그대로 종료
    }

    /**
     * 입력: int userId, int categoryId, EditCategoryRequest req
     * 출력: void
     * 기능: 스터디 관리자가 카테고리 정보를 수정합니다.
     */
    @Override
    public void editCategory(int userId, int categoryId, EditCategoryRequest req) {
        Category c = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        assertLeader(userId, c.getStudyGroup().getId());

        String newName = req.getCategoryName();
        if (newName == null || newName.isBlank()) throw new CustomException(ErrorCode.INVALID_REQUEST);

        if (!newName.trim().equals(c.getName()) &&
            categoryRepository.existsByStudyGroup_IdAndName(c.getStudyGroup().getId(), newName.trim())) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
        }

        c.setName(newName.trim());
        categoryRepository.save(c);
    }

    /**
     * 입력: int userId, int categoryId
     * 출력: void
     * 기능: 스터디 관리자가 카테고리를 삭제합니다.
     */
    @Override
    public void deleteCategory(int userId, int categoryId) {
        Category c = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        assertLeader(userId, c.getStudyGroup().getId());

        documentCategoryRepository.deleteByCategory_Id(categoryId); // FK가 ON DELETE CASCADE면 생략 가능
        categoryRepository.delete(c);
    }

    /**
     * 입력: int userId, int studyId
     * 출력: List<CategoryItemDto>
     * 기능: 해당 스터디의 커스텀 카테고리 목록을 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryItemDto> getCategories(int userId, int studyId) {
        boolean member = studyMembershipRepository
            .existsByUserIdAndStudyGroup_IdAndStatus(userId, studyId, StudyMembership.Status.APPROVED);
        if (!member) throw new CustomException(ErrorCode.FORBIDDEN);

        return categoryRepository.findByStudyGroup_IdOrderByIdDesc(studyId).stream()
            .map(c -> new CategoryItemDto(c.getId(), c.getName(), c.getCreatedAt(), c.getUpdatedAt()))
            .toList();
    }
}
