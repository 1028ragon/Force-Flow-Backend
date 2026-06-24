package ForceFlow.Military.repository;

import ForceFlow.Military.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByUserId(Long userId);

    List<Schedule> findByUserUnitId(Long unitId);

    boolean existsByUserIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            Long userId,
            LocalDateTime dutyEndAt,
            LocalDateTime dutyStartAt
    );

    @Query("""
            select distinct s.user.id
            from Schedule s
            where s.user.id in :userIds
              and s.startAt <= :endAt
              and s.endAt >= :startAt
            """)
    List<Long> findUserIdsWithConflict(
            @Param("userIds") List<Long> userIds,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

}
