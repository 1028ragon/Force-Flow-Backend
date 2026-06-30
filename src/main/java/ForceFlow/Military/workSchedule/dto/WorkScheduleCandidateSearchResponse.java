package ForceFlow.Military.workSchedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record WorkScheduleCandidateSearchResponse(
        Long unitId,
        LocalDate dutyDate,
        String dutyType,
        Integer slotOrder,
        LocalTime startTime,
        LocalTime endTime,
        Integer requiredCount,
        List<String> allowedRoles,
        List<WorkScheduleCandidateResponse> candidates
) {
}
