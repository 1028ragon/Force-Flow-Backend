package ForceFlow.Military.workSchedule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

public record WorkScheduleUnitSettingRequest(
        @NotBlank String dutyType,
        @NotNull @Min(1) Integer requiredCount,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull @Min(1) Integer lookbackDays,
        @NotNull Boolean preventConsecutive,
        @NotNull @Min(1) Integer maxDutyCount,
        List<String> excludeStatuses
) {
}
