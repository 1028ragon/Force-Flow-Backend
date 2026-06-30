package ForceFlow.Military.dto.requestDto;

import java.time.LocalDateTime;

public record ScheduleCreateRequest(
        Long userId,
        String type,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        String reason
) {}