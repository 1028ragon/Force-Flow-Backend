package ForceFlow.Military.workSchedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record WorkScheduleConfirmAssignmentRequest(
        @NotNull Long userId,
        @NotNull Long unitId,
        @NotBlank String role,
        @NotNull LocalDate dutyDate,
        @NotBlank String dutyType,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String aiReason
) {
}
