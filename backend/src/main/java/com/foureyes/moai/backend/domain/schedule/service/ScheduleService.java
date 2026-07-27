package com.foureyes.moai.backend.domain.schedule.service;

import com.foureyes.moai.backend.domain.schedule.dto.request.CreateScheduleRequestDto;
import com.foureyes.moai.backend.domain.schedule.dto.request.EditScheduleRequestDto;
import com.foureyes.moai.backend.domain.schedule.dto.response.*;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleService {
    /**
     * 입력: int userId, CreateScheduleRequestDto request
     * 출력: CreateScheduleResponseDto
     * 기능: 새로운 일정을 생성합니다.
     */
    public CreateScheduleResponseDto registerSchedule(
        int userId,
        CreateScheduleRequestDto request);

    /**
     * 입력: int userId, int scheduleId, EditScheduleRequestDto request
     * 출력: EditScheduleResponseDto
     * 기능: 기존 일정을 부분 수정합니다.
     */
    public EditScheduleResponseDto editSchedule(
        int userId,
        int scheduleId,
        EditScheduleRequestDto request);

    /**
     * 입력: int userId, int scheduleId
     * 출력: GetScheduleResponseDto
     * 기능: 일정 단건을 조회합니다.
     */
    GetScheduleResponseDto getSchedule(
        int userId,
        int scheduleId);

    /**
     * 입력: int userId, int studyId, LocalDateTime from, LocalDateTime to
     * 출력: List<GetScheduleListDto>
     * 기능: 스터디 기간 내 일정 목록을 조회합니다.
     */
    List<GetScheduleListDto> listByRange(
        int userId,
        int studyId,
        LocalDateTime from,
        LocalDateTime to);

    /**
     * 입력: int userId, LocalDateTime from, LocalDateTime to
     * 출력: List<MyScheduleListDto>
     * 기능: 마이페이지 일정 목록을 조회합니다.
     */
    List<MyScheduleListDto> listMySchedules(
        int userId,
        LocalDateTime from,
        LocalDateTime to);

    /**
     * 입력: int userId, int scheduleId
     * 출력: void
     * 기능: 일정을 삭제합니다.
     */
    void deleteSchedule(
        int userId,
        int scheduleId);
}
