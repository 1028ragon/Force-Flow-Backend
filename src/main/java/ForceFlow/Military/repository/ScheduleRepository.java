package ForceFlow.Military.repository;

import ForceFlow.Military.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByUserUserId(Long userId);

    List<Schedule> findByUserUnitUnitId(Long unitId);

    boolean existsByUserUserIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            Long userId,
            LocalDateTime dutyEndAt,
            LocalDateTime dutyStartAt
    );

}