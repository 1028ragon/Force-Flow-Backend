package ForceFlow.Military.workSchedule.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record WorkScheduleUnitSettingResponse(
        Long settingId,
        Long unitId,
        String dutyType,
        String description,
        Integer requiredCount,
        LocalTime startTime,
        LocalTime endTime,
        List<WorkScheduleTimeSlotResponse> timeSlots,
        Integer lookbackDays,
        Boolean preventConsecutive,
        Integer maxDutyCount,
        List<String> excludeStatuses,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
