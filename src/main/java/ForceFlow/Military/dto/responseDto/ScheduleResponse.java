package ForceFlow.Military.dto.responseDto;

import java.time.LocalDateTime;

public record ScheduleResponse(
        Long scheduleId,
        Long userId,
        String type,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        String reason,
        LocalDateTime createdAt
) {}