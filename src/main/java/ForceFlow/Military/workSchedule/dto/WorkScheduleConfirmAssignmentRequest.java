package ForceFlow.Military.workSchedule.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record WorkScheduleConfirmAssignmentRequest(
        Integer slotOrder,
        @NotNull Long userId,
        @NotNull Long unitId,
        String role,
        @NotNull LocalDate dutyDate,
        @NotNull String dutyType,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String aiReason
) {
}
