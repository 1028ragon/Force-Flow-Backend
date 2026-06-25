package ForceFlow.Military.workSchedule.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record WorkScheduleAssignmentResponse(
        Long dutyId,
        Long userId,
        Long unitId,
        String name,
        String rankName,
        String role,
        LocalDate dutyDate,
        String dutyType,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String aiReason,
        LocalDateTime approvedAt,
        LocalDateTime createdAt
) {
}
