package ForceFlow.Military.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record DutyHistoryItem(
        Long dutyId,
        Long userId,
        String name,
        String rankName,
        LocalDate dutyDate,
        String dutyType,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        LocalDateTime approvedAt
) {}