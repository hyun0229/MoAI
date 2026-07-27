package com.foureyes.moai.backend.domain.study.service;


import com.foureyes.moai.backend.domain.study.dto.request.CreateStudyRequest;
import com.foureyes.moai.backend.domain.study.dto.request.UpdateStudyRequestDto;
import com.foureyes.moai.backend.domain.study.dto.response.*;

import java.util.List;

public interface StudyService {
    /**
     * 입력: int userId, CreateStudyRequest request
     * 출력: StudyResponseDto
     * 기능: 새로운 스터디 그룹을 생성하고 요청자를 관리자로 등록합니다.
     */
    StudyResponseDto createStudy(int userId, CreateStudyRequest request);

    /**
     * 입력: int userId, int studyGroupId
     * 출력: void
     * 기능: 사용자가 특정 스터디에 가입 요청을 전송합니다.
     */
    void sendJoinRequest(int userId, int studyGroupId);

    /**
     * 입력: int userId, int studyId
     * 출력: List<StudyMemberListResponseDto>
     * 기능: 스터디에 승인된 모든 멤버의 정보를 조회합니다.
     */
    List<StudyMemberListResponseDto> getStudyMembers(int userId, int studyId);

    /**
     * 입력: int userId
     * 출력: List<StudyListResponseDto>
     * 기능: 사용자가 가입 신청했거나 승인된 모든 스터디 목록을 조회합니다.
     */
    List<StudyListResponseDto> getUserStudies(int userId);

    /**
     * 입력: int userId, int studyId
     * 출력: void
     * 기능: 사용자가 스터디에서 탈퇴합니다.
     */
    void leaveStudy(int userId, int studyId);

    /**
     * 입력: int adminUserId, int studyId, int targetUserId
     * 출력: void
     * 기능: 관리자가 스터디 멤버를 강제 탈퇴시킵니다.
     */
    void deleteMember(int adminUserId, int studyId, int targetUserId);

    /**
     * 입력: int adminUserId, int studyId, int targetUserId, String newRole
     * 출력: void
     * 기능: 관리자가 스터디 멤버의 역할을 변경합니다.
     */
    void changeMemberRole(int adminUserId, int studyId, int targetUserId, String newRole);

    /**
     * 입력: int adminUserId, int studyId, int targetUserId
     * 출력: void
     * 기능: 관리자가 스터디 가입 요청을 거절합니다.
     */
    void rejectJoinRequest(int adminUserId, int studyId, int targetUserId);

    /**
     * 입력: int adminUserId, int studyId, int targetUserId, String newRole
     * 출력: void
     * 기능: 관리자가 스터디 가입 요청을 승인하고 역할을 부여합니다.
     */
    void acceptJoinRequest(int adminUserId, int studyId, int targetUserId, String newRole);

    /**
     * 입력: int adminUserId, int studyId
     * 출력: List<JoinRequestResponseDto>
     * 기능: 관리자가 스터디의 대기 중인 가입 요청 목록을 조회합니다.
     */
    List<JoinRequestResponseDto> getPendingJoinRequests(int adminUserId, int studyId);

    /**
     * 입력: int userId
     * 출력: List<JoinStudyListResponseDto>
     * 기능: 사용자가 승인되어 참여 중인 스터디 목록을 조회합니다.
     */
    List<JoinStudyListResponseDto> getJoinedStudies(int userId);

    /**
     * 입력: int userId, String hashId
     * 출력: StudyDetailResponseDto
     * 기능: hashId로 스터디 상세 정보를 조회합니다.
     */
    StudyDetailResponseDto getStudyDetailByHashId(int userId, String hashId);

    /**
     * 입력: int userId, int studyId
     * 출력: StudyNoticeResponseDto
     * 기능: 승인된 멤버만 접근 가능한 스터디 공지사항을 조회합니다.
     */
    StudyNoticeResponseDto getStudyNotice(int userId, int studyId);

    /**
     * 입력: int userId, int studyId, String notice
     * 출력: void
     * 기능: 관리자가 스터디 공지사항을 수정합니다.
     */
    void updateStudyNotice(int userId, int studyId, String notice);

    /**
     * 입력: int userId, int studyId, UpdateStudyRequestDto request
     * 출력: void
     * 기능: 관리자가 스터디 정보를 수정합니다.
     */
    void updateStudyGroup(int userId, int studyId, UpdateStudyRequestDto request);
}
