package com.foureyes.moai.backend.domain.schedule.repository;

import com.foureyes.moai.backend.domain.schedule.entity.Schedule;
import com.foureyes.moai.backend.domain.study.entity.StudyMembership;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {
    Optional<Schedule> findById(Integer id);

    @Query("""
      select s from Schedule s
      where s.studyGroup.id = :studyId
        and s.startDatetime < :to
        and s.endDatetime   > :from
      order by s.startDatetime asc
    """)
    List<Schedule> findOverlappingByStudy(
        @Param("studyId") int studyId,
        @Param("from") LocalDateTime from,
        @Param("to")   LocalDateTime to
    );

    // 내가 APPROVED 상태로 속한 모든 스터디의 일정들 중 from~to와 겹치는 것만
    @Query("""
      select s from Schedule s
      join fetch s.studyGroup g
      where s.startDatetime < :to
        and s.endDatetime   > :from
        and exists (
          select 1 from StudyMembership m
          where m.studyGroup = g
            and m.userId = :userId
            and m.status = :status
        )
      order by s.startDatetime asc
    """)
    List<Schedule> findMyOverlapping(
        @Param("userId") int userId,
        @Param("status") StudyMembership.Status status,
        @Param("from") LocalDateTime from,
        @Param("to")   LocalDateTime to
    );
}
