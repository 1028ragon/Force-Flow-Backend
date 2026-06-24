package ForceFlow.Military.workSchedule.dto.internal;

import java.time.LocalDate;
import java.time.LocalTime;

public record DutyBlock(
        LocalDate dutyDate,
        String dutyType,
        Integer requiredCount,
        LocalTime startTime,
        LocalTime endTime
) {
}
