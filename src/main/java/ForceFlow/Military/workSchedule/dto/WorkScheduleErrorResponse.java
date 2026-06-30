package ForceFlow.Military.workSchedule.dto;

import java.time.LocalDateTime;

public record WorkScheduleErrorResponse(
        LocalDateTime timestamp,
        Integer status,
        String error,
        String message
) {
}
