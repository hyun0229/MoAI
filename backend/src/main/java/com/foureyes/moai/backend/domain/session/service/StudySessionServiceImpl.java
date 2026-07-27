// domain/study/session/service/StudySessionService.java
package com.foureyes.moai.backend.domain.session.service;

import com.foureyes.moai.backend.commons.exception.CustomException;
import com.foureyes.moai.backend.commons.exception.ErrorCode;
import com.foureyes.moai.backend.domain.session.dto.response.*;
import com.foureyes.moai.backend.domain.session.repository.StudySessionRepository;
import com.foureyes.moai.backend.domain.study.entity.StudyGroup;
import com.foureyes.moai.backend.domain.study.entity.StudyMembership;
import com.foureyes.moai.backend.domain.study.repository.StudyGroupRepository;
import com.foureyes.moai.backend.domain.study.repository.StudyMembershipRepository;
import com.foureyes.moai.backend.domain.session.entity.StudySession;
import com.foureyes.moai.backend.domain.user.entity.User;
import com.foureyes.moai.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.foureyes.moai.backend.domain.study.entity.StudyMembership.Role.ADMIN;
import static com.foureyes.moai.backend.domain.study.entity.StudyMembership.Role.DELEGATE;

@Service
@RequiredArgsConstructor
public class StudySessionServiceImpl implements StudySessionService{

    private final StudyGroupRepository studyGroupRepository;
    private final StudyMembershipRepository membershipRepository;
    private final StudySessionRepository sessionRepository;
    private final UserRepository userRepository;

    private final LiveKitAdminClient liveKitAdminClient;
    private final LiveKitTokenGenerator tokenGenerator;
    private final LiveKitRoomClient liveKitRoomClient;

    /**
     * 입력: String studyHashId, int meUserId
     * 출력: SessionResponseDto
     * 기능: 세션을 열거나(없으면 생성) 이미 열려 있으면 그대로 반환합니다.
     */
    @Override
    @Transactional
    public SessionResponseDto openOrGetByHashId(String studyHashId, int meUserId) {

        // 1) 스터디 존재 확인
        StudyGroup group = studyGroupRepository.findByHashId(studyHashId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));

        int studyId = group.getId();

        // 2) 멤버십 + 승인 상태 확인 (APPROVED만 통과)
        StudyMembership membership = membershipRepository
            .findByUserIdAndStudyGroup_IdAndStatus(meUserId, studyId, StudyMembership.Status.APPROVED)
            .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_MEMBER));

        StudyMembership.Role role = membership.getRole();
         if (!(role == ADMIN || role == DELEGATE)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 3) 이미 열린 세션이 있으면 반환
        Optional<StudySession> openOpt = sessionRepository.findByStudyGroupAndClosedAtIsNull(group);
        if (openOpt.isPresent()) {
            StudySession s = openOpt.get();
            return SessionResponseDto.builder()
                .id(s.getId())
                .studyGroupId(studyId)
                .studyGroupHashId(studyHashId)
                .roomName(s.getRoomName())
                .created(false)
                .build();
        }

        // 4) 생성 시도 (동시성은 DB UNIQUE 제약으로 최종 보호)
        String roomName = "study-" + studyHashId;

        liveKitAdminClient.ensureRoom(roomName);

        StudySession entity = StudySession.builder()
            .studyGroup(group)
            .roomName(roomName)
            .createdBy(meUserId)
            .createdAt(LocalDateTime.now())
            .build();

        try {
            StudySession saved = sessionRepository.save(entity);
            return SessionResponseDto.builder()
                .id(saved.getId())
                .studyGroupId(studyId)
                .studyGroupHashId(studyHashId)
                .roomName(roomName)
                .created(true)
                .build();
        } catch (DataIntegrityViolationException race) {
            // 경쟁 생성으로 UNIQUE(uq_one_open_per_group) 충돌 → 방금 생긴 세션 재조회 후 반환
            Optional<StudySession> existingOpt = sessionRepository.findByStudyGroupAndClosedAtIsNull(group);
            StudySession existing = existingOpt.orElseThrow(() -> new CustomException(ErrorCode.DATABASE_ERROR));
            return SessionResponseDto.builder()
                .id(existing.getId())
                .studyGroupId(studyId)
                .studyGroupHashId(studyHashId)
                .roomName(existing.getRoomName())
                .created(false)
                .build();
        }
    }

    /**
     * 입력: int userId
     * 출력: String(표시명)
     * 기능: DB에 저장된 사용자 이름을 정제하여 참가자 표시명을 생성합니다.
     */
    private String resolveDisplayName(int userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND)); // 404 매핑

        String candidate = user.getName(); // DB의 사용자 이름

        // 공통 정제: null/공백/제어문자 제거 + 길이 제한(예: 32자)
        if (candidate == null) candidate = "";
        candidate = candidate.replaceAll("\\p{Cntrl}", "").trim();
        if (candidate.isBlank()) {
            candidate = Integer.toString(userId); // 최종 fallback
        }
        if (candidate.length() > 32) {
            candidate = candidate.substring(0, 32);
        }
        return candidate;
    }

    /** 참가(토큰 발급): 세션이 없으면 관리자/대리자일 때만 자동 오픈, 아니면 403 */
    @Override
    @Transactional
    public JoinSessionResponseDto joinByHashId(String studyHashId, int meUserId) {
        StudyGroup group = studyGroupRepository.findByHashId(studyHashId)
            .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));
        int studyId = group.getId();

        StudyMembership membership = membershipRepository
            .findByUserIdAndStudyGroup_IdAndStatus(meUserId, studyId, StudyMembership.Status.APPROVED)
            .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_MEMBER));

        String roomName;
        Optional<StudySession> openOpt = sessionRepository.findByStudyGroupAndClosedAtIsNull(group);
        if (openOpt.isPresent()) {
            roomName = openOpt.get().getRoomName();
        } else {
            boolean canOpen = (membership.getRole() == StudyMembership.Role.ADMIN)
                || (membership.getRole() == StudyMembership.Role.DELEGATE);
            if (!canOpen) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
            // 열고 나서 DTO에서 roomName만 가져와 사용
            SessionResponseDto opened = openOrGetByHashId(studyHashId, meUserId);
            roomName = opened.getRoomName();
        }

        String displayName = resolveDisplayName(meUserId);

        String token = tokenGenerator.createJoinToken(roomName, meUserId, displayName);
        return JoinSessionResponseDto.builder()
            .roomName(roomName)
            .wsUrl(tokenGenerator.getWsUrl())
            .displayName(displayName)
            .token(token)
            .build();
    }

    /**
     * 입력: String studyHashId, int meUserId
     * 출력: CloseSessionResponseDto
     * 기능: 세션을 종료합니다.
     */
    @Override
    @Transactional
    public CloseSessionResponseDto closeByHashId(String studyHashId, int meUserId) {
        // 1) 스터디 확인
        StudyGroup group = studyGroupRepository.findByHashId(studyHashId)
            .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));
        int studyId = group.getId();

        // 2) 승인 멤버 확인
        StudyMembership membership = membershipRepository
            .findByUserIdAndStudyGroup_IdAndStatus(meUserId, studyId, StudyMembership.Status.APPROVED)
            .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_MEMBER));

        // 3) 권한 확인
        boolean canClose = (membership.getRole() == StudyMembership.Role.ADMIN)
            || (membership.getRole() == StudyMembership.Role.DELEGATE);
        if (!canClose) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 4) 열린 세션 잠금 조회
        Optional<StudySession> openOpt = sessionRepository.findOpenForUpdate(group);
        if (openOpt.isEmpty()) {
            // 열린 세션 없음 → idempotent 응답
            return CloseSessionResponseDto.builder()
                .id(0)
                .studyGroupHashId(studyHashId)
                .roomName("study-" + studyHashId)
                .closed(false)
                .closedAt(null)
                .build();
        }

        StudySession session = openOpt.get();
        session.setClosedAt(LocalDateTime.now()); // UNIQUE 제약상 is_open이 NULL로 바뀜
        sessionRepository.save(session);

        return CloseSessionResponseDto.builder()
            .id(session.getId())
            .studyGroupHashId(studyHashId)
            .roomName(session.getRoomName())
            .closed(true)
            .closedAt(session.getClosedAt())
            .build();
    }

    /**
     * 입력: String studyHashId, int meUserId
     * 출력: ParticipantsResponseDto
     * 기능: 현재 세션 참여자 목록을 LiveKit에서 조회해 DB 정보와 매칭합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public ParticipantsResponseDto listParticipantsByHashId(String studyHashId, int meUserId) {

        // 1) 스터디 존재 + 내 멤버십 확인(승인 필수)
        StudyGroup group = studyGroupRepository.findByHashId(studyHashId)
            .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));
        int studyId = group.getId();

        membershipRepository.findByUserIdAndStudyGroup_IdAndStatus(
            meUserId, studyId, StudyMembership.Status.APPROVED
        ).orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_MEMBER));

        // 2) 열린 세션 확인
        Optional<StudySession> openOpt = sessionRepository.findByStudyGroupAndClosedAtIsNull(group);
        if (openOpt.isEmpty()) {
            return ParticipantsResponseDto.builder()
                .sessionOpen(false)
                .count(0)
                .participants(Collections.emptyList())
                .build();
        }

        String roomName = openOpt.get().getRoomName();

        // 3) LiveKit에서 현재 참가자(identity, name) 조회
        List<LiveKitRoomClient.RoomParticipant> livekitParticipants =
            liveKitRoomClient.listParticipants(roomName);

        if (livekitParticipants.isEmpty()) {
            return ParticipantsResponseDto.builder()
                .sessionOpen(true)
                .count(0)
                .participants(Collections.emptyList())
                .build();
        }

        // 4) identity(=userId 문자열)를 int로 변환 → 우리 DB에서 Users 조회
        List<Integer> userIds = new ArrayList<>();
        Map<Integer, String> nameFallback = new HashMap<>(); // LiveKit name fallback
        for (LiveKitRoomClient.RoomParticipant p : livekitParticipants) {
            try {
                int uid = Integer.parseInt(p.getIdentity());
                userIds.add(uid);
                if (p.getName() != null) {
                    nameFallback.put(uid, p.getName());
                }
            } catch (NumberFormatException ignore) {
                // 우리 룰(identity=userId)에 안 맞는 참가자는 무시(외부 게스트 방지)
            }
        }

        if (userIds.isEmpty()) {
            return ParticipantsResponseDto.builder()
                .sessionOpen(true)
                .count(0)
                .participants(Collections.emptyList())
                .build();
        }

        List<User> users = userRepository.findAllById(userIds);
        Map<Integer, User> userMap = users.stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        // 5) DTO 변환: 이름은 DB 우선, 없으면 LiveKit name, 최종 fallback은 userId
        List<ParticipantDto> dtos = new ArrayList<>();
        for (Integer uid : userIds) {
            User u = userMap.get(uid);
            String name;
            String profile;

            if (u != null) {
                name = (u.getName() != null && !u.getName().isBlank())
                    ? u.getName().trim()
                    : nameFallback.getOrDefault(uid, Integer.toString(uid));
                profile = u.getProfileImageUrl();
            } else {
                name = nameFallback.getOrDefault(uid, Integer.toString(uid));
                profile = null;
            }

            if (name.length() > 32) {
                name = name.substring(0, 32);
            }

            dtos.add(ParticipantDto.builder()
                .name(name)
                .profileImageUrl(profile)
                .build());
        }

        return ParticipantsResponseDto.builder()
            .sessionOpen(true)
            .count(dtos.size())
            .participants(dtos)
            .build();
    }
}
