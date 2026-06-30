package ForceFlow.Military.dto.responseDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record DutyApproveResponse(
        Long dutyId,
        Long userId,
        Long unitId,
        LocalDate dutyDate,
        String dutyType,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String aiReason,
        LocalDateTime approvedAt,
        LocalDateTime createdAt
) {}
