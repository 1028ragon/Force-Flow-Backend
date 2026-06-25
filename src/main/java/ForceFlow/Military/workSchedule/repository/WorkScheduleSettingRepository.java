package ForceFlow.Military.workSchedule.repository;

import ForceFlow.Military.workSchedule.entity.WorkScheduleSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkScheduleSettingRepository extends JpaRepository<WorkScheduleSetting, Long> {

    Optional<WorkScheduleSetting> findByUnitId(Long unitId);

    Optional<WorkScheduleSetting> findByUnitIdAndDutyType(Long unitId, String dutyType);
}
