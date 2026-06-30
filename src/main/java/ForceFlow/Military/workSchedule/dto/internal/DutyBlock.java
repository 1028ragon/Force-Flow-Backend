package ForceFlow.Military.workSchedule.dto.internal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DutyBlock(
        LocalDate dutyDate,
        String dutyType,
        Integer requiredCount,
        LocalTime startTime,
        LocalTime endTime,
        List<DutySlotBlock> timeSlots
) {
}
