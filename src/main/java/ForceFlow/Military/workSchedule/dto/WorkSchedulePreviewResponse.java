package ForceFlow.Military.workSchedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record WorkSchedulePreviewResponse(
        Long unitId,
        LocalDate dutyDate,
        String dutyType,
        Integer requiredCount,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String warningMessage,
        List<WorkSchedulePreviewAssignmentResponse> assignments,
        String requestJson,
        String responseJson
) {
}
