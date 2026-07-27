package com.foureyes.moai.backend.domain.study.service;

import com.foureyes.moai.backend.commons.exception.CustomException;
import com.foureyes.moai.backend.commons.exception.ErrorCode;
import com.foureyes.moai.backend.commons.util.StorageService;
import com.foureyes.moai.backend.domain.study.dto.request.CreateStudyRequest;
import com.foureyes.moai.backend.domain.study.dto.request.UpdateStudyRequestDto;
import com.foureyes.moai.backend.domain.study.dto.response.*;
import com.foureyes.moai.backend.domain.study.entity.StudyGroup;
import com.foureyes.moai.backend.domain.study.entity.StudyMembership;
import com.foureyes.moai.backend.domain.study.repository.StudyGroupRepository;
import com.foureyes.moai.backend.domain.study.repository.StudyMembershipRepository;
import com.foureyes.moai.backend.domain.user.entity.User;
import com.foureyes.moai.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hashids.Hashids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyServiceImpl implements StudyService {

    private static final Logger log = LoggerFactory.getLogger(StudyServiceImpl.class);

    private final StudyGroupRepository studyGroupRepository;
    private final StudyMembershipRepository studyMembershipRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final Hashids hashids;

    /**
     * 관리자 권한을 확인하는 공통 메서드
     */
    private StudyMembership validateAdminMembership(int adminUserId, int studyId) {
        StudyMembership adminMembership = studyMembershipRepository
            .findByUserIdAndStudyGroup_IdAndStatus(
                adminUserId, studyId, StudyMembership.Status.APPROVED)
            .orElseThrow(() -> {
                log.warn("관리자가 아닌 사용자의 권한 요청: adminUserId={}, studyId={}", adminUserId, studyId);
                return new CustomException(ErrorCode.STUDY_NOT_MEMBER);
            });

        if (adminMembership.getRole() != StudyMembership.Role.ADMIN) {
            log.warn("관리자 권한 없는 사용자의 권한 요청: adminUserId={}, studyId={}", adminUserId, studyId);
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return adminMembership;
    }

    /**
     * 스터디 그룹 존재 여부를 확인하는 공통 메서드
     */
    private StudyGroup validateStudyGroupExists(int studyGroupId) {
        return studyGroupRepository.findById(studyGroupId)
            .orElseThrow(() -> {
                log.warn("존재하지 않는 스터디 그룹: studyGroupId={}", studyGroupId);
                return new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND);
            });
    }

    /**
     * 입력: int userId (사용자 ID), CreateStudyRequest request (스터디 생성 요청)
     * 출력: StudyResponseDto (생성된 스터디 정보)
     * 기능: 새로운 스터디 그룹을 생성하고 요청자를 관리자로 등록
     **/
    @Override
    @Transactional
    public StudyResponseDto createStudy(int userId, CreateStudyRequest request) {
        log.info("스터디 생성 시작: userId={}, 스터디명={}", userId, request.getName());

        String imageUrl = null;
        try {
            if (request.getImage() != null){
                imageUrl = storageService.uploadFile(request.getImage());
                log.info("스터디 이미지 업로드 완료: imageUrl={}", imageUrl);
            }
        } catch (IOException e) {
            log.error("스터디 이미지 업로드 실패: userId={}, error={}", userId, e.getMessage());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        StudyGroup studyGroup = StudyGroup.builder()
            .name(request.getName())
            .description(request.getDescription())
            .imageUrl(imageUrl)
            .createdBy(userId)
            .createdAt(LocalDateTime.now())
            .maxCapacity(request.getMaxCapacity())
            .build();

        StudyGroup saved = studyGroupRepository.save(studyGroup);

        String encoded = hashids.encode(saved.getId());
        saved.setHashId(encoded);
        studyGroupRepository.save(saved);

        StudyMembership studyMembership = StudyMembership.builder()
            .userId(userId)
            .studyGroup(saved)
            .role(StudyMembership.Role.ADMIN)
            .status(StudyMembership.Status.APPROVED)
            .joinedAt(LocalDateTime.now())
            .build();
        studyMembershipRepository.save(studyMembership);

        log.info("스터디 생성 완료: studyId={}, userId={}", saved.getId(), userId);

        return StudyResponseDto.builder()
            .id(saved.getId())
            .name(saved.getName())
            .description(saved.getDescription())
            .imageUrl(saved.getImageUrl())
            .createdBy(saved.getCreatedBy())
            .createdAt(saved.getCreatedAt())
            .hashId(saved.getHashId())
            .build();
    }

    /**
     * 입력: int userId (사용자 ID), int studyGroupId (스터디 그룹 ID)
     * 출력: void
     * 기능: 사용자가 특정 스터디에 가입 요청을 전송
     **/
    @Override
    @Transactional
    public void sendJoinRequest(int userId, int studyGroupId) {
        log.info("스터디 가입 요청 시작: userId={}, studyGroupId={}", userId, studyGroupId);

        StudyGroup group = validateStudyGroupExists(studyGroupId);

        // PENDING이나 APPROVED 상태인 경우만 중복으로 처리
        Optional<StudyMembership> existingMembership =
            studyMembershipRepository.findByUserIdAndStudyGroup_Id(userId, studyGroupId);

        if (existingMembership.isPresent()) {
            StudyMembership.Status status = existingMembership.get().getStatus();
            if (status == StudyMembership.Status.PENDING || status == StudyMembership.Status.APPROVED) {
                log.warn("이미 가입 요청한 스터디에 중복 요청: userId={}, studyGroupId={}, status={}",
                        userId, studyGroupId, status);
                throw new CustomException(ErrorCode.ALREADY_JOINED_STUDY);
            }
            else{
                StudyMembership studyMembership = existingMembership.get();
                studyMembership.setStatus(StudyMembership.Status.PENDING);
                studyMembershipRepository.save(studyMembership);
            }
        }
        else{
            StudyMembership req = StudyMembership.builder()
                .userId(userId)
                .studyGroup(group)
                .role(StudyMembership.Role.MEMBER)
                .status(StudyMembership.Status.PENDING)
                .joinedAt(LocalDateTime.now())
                .build();
            studyMembershipRepository.save(req);
        }



        log.info("스터디 가입 요청 완료: userId={}, studyGroupId={}", userId, studyGroupId);
    }

    /**
     * 입력: int userId (사용자 ID), int studyId (스터디 ID)
     * 출력: List<StudyMemberListResponseDto> (스터디 멤버 목록)
     * 기능: 스터디에 승인된 모든 멤버의 정보를 조회
     **/
    @Override
    @Transactional(readOnly = true)
    public List<StudyMemberListResponseDto> getStudyMembers(int userId, int studyId) {
        log.info("스터디 멤버 목록 조회 시작: userId={}, studyId={}", userId, studyId);

        boolean joined = studyMembershipRepository
            .existsByUserIdAndStudyGroup_IdAndStatus(userId, studyId, StudyMembership.Status.APPROVED);
        if (!joined) {
            log.warn("스터디 멤버가 아닌 사용자의 멤버 목록 조회 시도: userId={}, studyId={}", userId, studyId);
            throw new CustomException(ErrorCode.STUDY_NOT_MEMBER);
        }

        List<StudyMemberListResponseDto> members = studyMembershipRepository
            .findAllByStudyGroup_IdAndStatus(studyId, StudyMembership.Status.APPROVED)
            .stream()
            .map(membership -> {
                User user = userRepository.findById(membership.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                return StudyMemberListResponseDto.builder()
                    .userId(user.getId())
                    .member(user.getName())
                    .email(user.getEmail())
                    .imageUrl(user.getProfileImageUrl())
                    .role(membership.getRole().name())
                    .build();
            })
            .collect(Collectors.toList());

        log.info("스터디 멤버 목록 조회 완료: studyId={}, memberCount={}", studyId, members.size());
        return members;
    }

    /**
     * 입력: int userId (사용자 ID)
     * 출력: List<StudyListResponseDto> (사용자 스터디 목록)
     * 기능: 사용자가 가입 신청했거나 승인된 모든 스터디 목록을 조회
     **/
    @Override
    @Transactional(readOnly = true)
    public List<StudyListResponseDto> getUserStudies(int userId) {
        log.info("사용자 스터디 목록 조회 시작: userId={}", userId);

        List<StudyMembership.Status> statuses = List.of(
            StudyMembership.Status.PENDING,
            StudyMembership.Status.APPROVED
        );

        List<StudyListResponseDto> studies = studyMembershipRepository
            .findAllByUserIdAndStatusIn(userId, statuses)
            .stream()
            .map(m -> StudyListResponseDto.builder()
                .name(m.getStudyGroup().getName())
                .description(m.getStudyGroup().getDescription())
                .imageUrl(m.getStudyGroup().getImageUrl())
                .creatorName(
                    userRepository.findById(m.getStudyGroup().getCreatedBy())
                    .map(User::getName)
                    .orElse("Unknown"))
                .status(m.getStatus().name())
                .studyId(m.getStudyGroup().getId())
                .hashId(m.getStudyGroup().getHashId())
                .build())
            .collect(Collectors.toList());

        log.info("사용자 스터디 목록 조회 완료: userId={}, studyCount={}", userId, studies.size());
        return studies;
    }

    /**
     * 입력: int userId (사용자 ID), int studyId (스터디 ID)
     * 출력: void
     * 기능: 사용자가 스터디에서 탈퇴
     **/
    @Override
    @Transactional
    public void leaveStudy(int userId, int studyId) {
        log.info("스터디 탈퇴 시작: userId={}, studyId={}", userId, studyId);

        StudyMembership membership = studyMembershipRepository
            .findByUserIdAndStudyGroup_IdAndStatus(
                userId, studyId, StudyMembership.Status.APPROVED)
            .orElseThrow(() -> {
                log.warn("스터디 멤버가 아닌 사용자의 탈퇴 시도: userId={}, studyId={}", userId, studyId);
                return new CustomException(ErrorCode.STUDY_NOT_MEMBER);
            });

        membership.setStatus(StudyMembership.Status.LEFT);
        studyMembershipRepository.save(membership);

        log.info("스터디 탈퇴 완료: userId={}, studyId={}", userId, studyId);
    }

    /**
     * 입력: int adminUserId (관리자 ID), int studyId (스터디 ID), int targetUserId (대상 사용자 ID)
     * 출력: void
     * 기능: 관리자가 스터디 멤버를 강제 탈퇴시킴
     **/
    @Override
    @Transactional
    public void deleteMember(int adminUserId, int studyId, int targetUserId) {
        log.info("스터디 멤버 강제 탈퇴 시작: adminUserId={}, studyId={}, targetUserId={}",
                adminUserId, studyId, targetUserId);

        validateAdminMembership(adminUserId, studyId);

        StudyMembership targetMember = studyMembershipRepository
            .findByUserIdAndStudyGroup_IdAndStatus(
                targetUserId, studyId, StudyMembership.Status.APPROVED)
            .orElseThrow(() -> {
                log.warn("존재하지 않는 멤버 삭제 시도: targetUserId={}, studyId={}", targetUserId, studyId);
                return new CustomException(ErrorCode.STUDY_MEMBERSHIP_NOT_FOUND);
            });

        targetMember.setStatus(StudyMembership.Status.LEFT);
        studyMembershipRepository.save(targetMember);

        log.info("스터디 멤버 강제 탈퇴 완료: adminUserId={}, studyId={}, targetUserId={}",
                adminUserId, studyId, targetUserId);
    }

    /**
     * 입력: int adminUserId (관리자 ID), int studyId (스터디 ID), int targetUserId (대상 사용자 ID), String newRole (새 역할)
     * 출력: void
     * 기능: 관리자가 스터디 멤버의 역할을 변경
     **/
    @Override
    @Transactional
    public void changeMemberRole(int adminUserId, int studyId, int targetUserId, String newRole) {
        log.info("스터디 멤버 역할 변경 시작: adminUserId={}, studyId={}, targetUserId={}, newRole={}",
                adminUserId, studyId, targetUserId, newRole);

        StudyMembership adminMembership = validateAdminMembership(adminUserId, studyId);

        // 변경 대상 멤버 조회 (APPROVED 상태)
        StudyMembership targetMembership = studyMembershipRepository
            .findByUserIdAndStudyGroup_IdAndStatus(
                targetUserId, studyId, StudyMembership.Status.APPROVED)
            .orElseThrow(() -> {
                log.warn("존재하지 않는 멤버의 역할 변경 시도: targetUserId={}, studyId={}", targetUserId, studyId);
                return new CustomException(ErrorCode.STUDY_MEMBERSHIP_NOT_FOUND);
            });

        // 새로운 역할 enum 변환
        StudyMembership.Role roleEnum;
        try {
            roleEnum = StudyMembership.Role.valueOf(newRole);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 역할로 변경 시도: newRole={}, studyId={}", newRole, studyId);
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // 만약 새 역할이 ADMIN 이면, 기존 ADMIN → MEMBER 로 강등
        if (roleEnum == StudyMembership.Role.ADMIN) {
            adminMembership.setRole(StudyMembership.Role.MEMBER);
            studyMembershipRepository.save(adminMembership);
            log.info("기존 관리자 강등: oldAdminUserId={}, studyId={}", adminUserId, studyId);
        }

        // 대상 멤버 역할 변경 및 저장
        targetMembership.setRole(roleEnum);
        studyMembershipRepository.save(targetMembership);

        log.info("스터디 멤버 역할 변경 완료: adminUserId={}, studyId={}, targetUserId={}, newRole={}",
                adminUserId, studyId, targetUserId, newRole);
    }

    /**
     * 입력: int adminUserId (관리자 ID), int studyId (스터디 ID), int targetUserId (대상 사용자 ID)
     * 출력: void
     * 기능: 관리자가 스터디 가입 요청을 거절
     **/
    @Override
    @Transactional
    public void rejectJoinRequest(int adminUserId, int studyId, int targetUserId) {
        log.info("스터디 가입 요청 거절 시작: adminUserId={}, studyId={}, targetUserId={}",
                adminUserId, studyId, targetUserId);

        validateAdminMembership(adminUserId, studyId);

        // 거절 대상이 PENDING 상태인지 조회
        StudyMembership target = studyMembershipRepository
            .findByUserIdAndStudyGroup_IdAndStatus(
                targetUserId, studyId, StudyMembership.Status.PENDING)
            .orElseThrow(() -> {
                log.warn("대기 중이지 않은 사용자의 가입 요청 거절 시도: targetUserId={}, studyId={}", targetUserId, studyId);
                return new CustomException(ErrorCode.STUDY_MEMBERSHIP_NOT_FOUND);
            });

        // 상태를 REJECTED로 변경
        target.setStatus(StudyMembership.Status.REJECTED);
        studyMembershipRepository.save(target);

        log.info("스터디 가입 요청 거절 완료: adminUserId={}, studyId={}, targetUserId={}",
                adminUserId, studyId, targetUserId);
    }

    /**
     * 입력: int adminUserId (관리자 ID), int studyId (스터디 ID), int targetUserId (대상 사용자 ID), String newRole (새 역할)
     * 출력: void
     * 기능: 관리자가 스터디 가입 요청을 승인하고 역할을 부여
     **/
    @Override
    @Transactional
    public void acceptJoinRequest(int adminUserId, int studyId, int targetUserId, String newRole) {
        log.info("스터디 가입 요청 승인 시작: adminUserId={}, studyId={}, targetUserId={}, newRole={}",
                adminUserId, studyId, targetUserId, newRole);

        StudyMembership admin = validateAdminMembership(adminUserId, studyId);

        StudyMembership target = studyMembershipRepository
            .findByUserIdAndStudyGroup_IdAndStatus(
                targetUserId, studyId, StudyMembership.Status.PENDING)
            .orElseThrow(() -> {
                log.warn("대기 중이지 않은 사용자의 가입 요청 승인 시도: targetUserId={}, studyId={}", targetUserId, studyId);
                return new CustomException(ErrorCode.STUDY_MEMBERSHIP_NOT_FOUND);
            });

        // 최대 인원 체크
        long currentApprovedCount = studyMembershipRepository
            .countByStudyGroup_IdAndStatus(studyId, StudyMembership.Status.APPROVED);
        StudyGroup studyGroup = admin.getStudyGroup();

        if (currentApprovedCount >= studyGroup.getMaxCapacity()) {
            log.warn("스터디 최대 인원 초과로 가입 승인 실패: studyId={}, currentCount={}, maxCapacity={}",
                    studyId, currentApprovedCount, studyGroup.getMaxCapacity());
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        StudyMembership.Role roleEnum;
        try {
            roleEnum = StudyMembership.Role.valueOf(newRole);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 역할로 가입 승인 시도: newRole={}, studyId={}", newRole, studyId);
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        target.setStatus(StudyMembership.Status.APPROVED);
        target.setRole(roleEnum);
        studyMembershipRepository.save(target);

        log.info("스터디 가입 요청 승인 완료: adminUserId={}, studyId={}, targetUserId={}, newRole={}",
                adminUserId, studyId, targetUserId, newRole);
    }

    /**
     * 입력: int adminUserId (관리자 ID), int studyId (스터디 ID)
     * 출력: List<JoinRequestResponseDto> (대기 중인 가입 요청 목록)
     * 기능: 관리자가 스터디의 대기 중인 가입 요청 목록을 조회
     **/
    @Override
    @Transactional(readOnly = true)
    public List<JoinRequestResponseDto> getPendingJoinRequests(int adminUserId, int studyId) {
        log.info("대기 중인 가입 요청 목록 조회 시작: adminUserId={}, studyId={}", adminUserId, studyId);

        validateAdminMembership(adminUserId, studyId);

        // PENDING 요청 조회
        List<JoinRequestResponseDto> pendingRequests = studyMembershipRepository
            .findAllByStudyGroup_IdAndStatus(studyId, StudyMembership.Status.PENDING)
            .stream()
            .map(m -> {
                User user = userRepository.findById(m.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                return new JoinRequestResponseDto(
                    m.getUserId(),
                    user.getEmail(),
                    user.getName(),
                    user.getProfileImageUrl(),
                    m.getStatus().name()
                );
            })
            .toList();


        log.info("대기 중인 가입 요청 목록 조회 완료: adminUserId={}, studyId={}, requestCount={}",
                adminUserId, studyId, pendingRequests.size());

        return pendingRequests;
    }

    /**
     * 입력: int userId
     * 출력: List<JoinStudyListResponseDto>
     * 기능: 사용자가 승인되어 참여 중인 스터디 목록을 조회합니다.
     */
    @Override
    @Transactional
    public List<JoinStudyListResponseDto> getJoinedStudies(int userId) {
        return studyMembershipRepository
            .findAllByUserIdAndStatus(userId, StudyMembership.Status.APPROVED)
            .stream()
            .map(membership -> {
                StudyGroup group = membership.getStudyGroup();
                return JoinStudyListResponseDto.builder()
                    .name(group.getName())
                    .imageUrl(group.getImageUrl())
                    .studyId(group.getId())
                    .hashId(group.getHashId())
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * 입력: int userId, String hashId
     * 출력: StudyDetailResponseDto
     * 기능: hashId로 스터디 상세 정보를 조회합니다. 멤버십 상태에 따라 반환 정보 범위가 다릅니다.
     */
    @Override
    @Transactional(readOnly = true)
    public StudyDetailResponseDto getStudyDetailByHashId(int userId, String hashId) {

        StudyGroup group = studyGroupRepository.findByHashId(hashId)
            .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));

        Optional<StudyMembership> membershipOpt =
            studyMembershipRepository.findByUserIdAndStudyGroup_Id(userId, group.getId());

        // 1) 멤버십 없음 → 이름, 이미지
        if (membershipOpt.isEmpty()) {
            return StudyDetailResponseDto.builder()
                .id(group.getId())
                .name(group.getName())
                .imageUrl(group.getImageUrl())
                .build();
        }
        StudyMembership membership = membershipOpt.get();
        // 2) PENDING → 이름, 이미지, 상태 만
        if (membership.getStatus() == StudyMembership.Status.PENDING) {
            return StudyDetailResponseDto.builder()
                .id(group.getId())
                .name(group.getName())
                .imageUrl(group.getImageUrl())
                .status(StudyMembership.Status.PENDING.name())
                .build();
        }
        // 3) APPROVED → 전부
        if (membership.getStatus() == StudyMembership.Status.APPROVED) {
            long approvedCount = studyMembershipRepository
                .countByStudyGroup_IdAndStatus(group.getId(), StudyMembership.Status.APPROVED);

            return StudyDetailResponseDto.builder()
                .id(group.getId())
                .name(group.getName())
                .imageUrl(group.getImageUrl())
                .status(StudyMembership.Status.APPROVED.name())
                .role(membership.getRole().name())
                .description(group.getDescription())
                .userCount((int) approvedCount)
                .build();
        }

        // 4) 그 외(LEFT/REJECTED) → 명시 없으셔서 이름, 이미지, 상태만 포함하도록 처리
        return StudyDetailResponseDto.builder()
            .id(group.getId())
            .name(group.getName())
            .imageUrl(group.getImageUrl())
            .status(membership.getStatus().name())
            .build();
    }

    /**
     * 입력: int userId, int studyId
     * 출력: StudyNoticeResponseDto
     * 기능: 승인된 멤버만 접근 가능한 스터디 공지사항을 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public StudyNoticeResponseDto getStudyNotice(int userId, int studyId) {
        StudyGroup group = studyGroupRepository.findById(studyId)
            .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));
        boolean approved = studyMembershipRepository
            .existsByUserIdAndStudyGroup_IdAndStatus(
                userId, studyId, StudyMembership.Status.APPROVED);
        if (!approved) {
            throw new CustomException(ErrorCode.STUDY_NOT_MEMBER);
        }
        return StudyNoticeResponseDto.builder()
            .notice(group.getNotice())
            .build();
    }

    /**
     * 입력: int userId, int studyId, String notice
     * 출력: void
     * 기능: 관리자가 스터디 공지사항을 수정합니다.
     */
    @Override
    @Transactional
    public void updateStudyNotice(int userId, int studyId, String notice) {
        StudyGroup group = validateStudyGroupExists(studyId);
        validateAdminMembership(userId, studyId);

        group.setNotice(notice == null ? null : notice.trim());
        studyGroupRepository.save(group);
    }

    /**
     * 입력: int userId, int studyId, UpdateStudyRequestDto request
     * 출력: void
     * 기능: 관리자가 스터디 이름/설명/이미지/최대 인원을 수정합니다.
     */
    @Override
    @Transactional
    public void updateStudyGroup(int userId, int studyId, UpdateStudyRequestDto request) {
        StudyGroup group = validateStudyGroupExists(studyId);
        validateAdminMembership(userId,studyId);

        String imageUrl = null;
        try {
            if (request.getImage() != null){
                imageUrl = storageService.uploadFile(request.getImage());
                group.setImageUrl(imageUrl);
                log.info("스터디 이미지 업로드 완료: imageUrl={}", imageUrl);
            }
        } catch (IOException e) {
            log.error("스터디 이미지 업로드 실패: userId={}, error={}", userId, e.getMessage());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        if (request.getName() != null){
            group.setName(request.getName());
        }
        if (request.getDescription() != null){
            group.setDescription(request.getDescription());
        }
        if (request.getMaxCapacity() != 0){
            group.setMaxCapacity(request.getMaxCapacity());
        }
        studyGroupRepository.save(group);

    }

}
