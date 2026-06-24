package ForceFlow.Military.workSchedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record WorkScheduleConfirmRequest(
        Long recommendationId,
        @NotNull Long unitId,
        @NotNull LocalDate dutyDate,
        @NotNull String dutyType,
        @NotNull @Positive Integer requiredCount,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String requestJson,
        String responseJson,
        @NotEmpty List<@Valid WorkScheduleConfirmAssignmentRequest> assignments
) {
}
