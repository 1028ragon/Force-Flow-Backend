package ForceFlow.Military.workSchedule.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record WorkScheduleConfirmAssignmentRequest(
        Integer slotOrder,
        @NotNull Long userId,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String aiReason
) {
}
