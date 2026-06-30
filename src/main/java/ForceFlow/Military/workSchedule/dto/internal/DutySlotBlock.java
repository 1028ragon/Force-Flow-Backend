package ForceFlow.Military.workSchedule.dto.internal;

import java.time.LocalTime;
import java.util.List;

public record DutySlotBlock(
        Integer slotOrder,
        LocalTime startTime,
        LocalTime endTime,
        Integer requiredCount,
        List<String> allowedRoles
) {
}
