package ForceFlow.Military.workSchedule.dto;

import java.time.LocalTime;
import java.util.List;

public record WorkScheduleTimeSlotResponse(
        Long slotId,
        Integer slotOrder,
        LocalTime startTime,
        LocalTime endTime,
        Integer requiredCount,
        List<String> allowedRoles
) {
}
