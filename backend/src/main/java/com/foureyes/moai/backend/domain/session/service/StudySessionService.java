package com.foureyes.moai.backend.domain.session.service;

import com.foureyes.moai.backend.domain.session.dto.response.CloseSessionResponseDto;
import com.foureyes.moai.backend.domain.session.dto.response.JoinSessionResponseDto;
import com.foureyes.moai.backend.domain.session.dto.response.ParticipantsResponseDto;
import com.foureyes.moai.backend.domain.session.dto.response.SessionResponseDto;

public interface StudySessionService {
    /**
     * 입력: String studyHashId, int meUserId
     * 출력: SessionResponseDto
     * 기능: 세션을 열거나(없으면 생성) 이미 열려 있으면 그대로 반환합니다.
     */
    SessionResponseDto openOrGetByHashId(String studyHashId, int meUserId);

    /**
     * 입력: String studyHashId, int meUserId
     * 출력: JoinSessionResponseDto
     * 기능: 세션 참가 토큰을 발급합니다.
     */
    JoinSessionResponseDto joinByHashId(String studyHashId, int meUserId);

    /**
     * 입력: String studyHashId, int meUserId
     * 출력: CloseSessionResponseDto
     * 기능: 세션을 종료합니다.
     */
    CloseSessionResponseDto closeByHashId(String studyHashId, int meUserId);

    /**
     * 입력: String studyHashId, int meUserId
     * 출력: ParticipantsResponseDto
     * 기능: 현재 세션 참여자 목록을 조회합니다.
     */
    ParticipantsResponseDto listParticipantsByHashId(String studyHashId, int meUserId);
}
