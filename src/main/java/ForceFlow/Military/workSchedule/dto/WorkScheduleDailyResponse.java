package ForceFlow.Military.workSchedule.dto;

import java.time.LocalDate;
import java.util.List;

public record WorkScheduleDailyResponse(
        Long unitId,
        LocalDate dutyDate,
        String dutyType,
        int assignmentCount,
        List<WorkScheduleAssignmentResponse> assignments
) {
}
