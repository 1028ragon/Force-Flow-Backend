package ForceFlow.Military.workSchedule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record WorkScheduleUnitSettingRequest(
        @NotBlank String dutyType,
        String description,
        @NotEmpty List<@Valid WorkScheduleTimeSlotRequest> timeSlots,
        @NotNull @Min(1) Integer lookbackDays,
        @NotNull Boolean preventConsecutive,
        @NotNull @Min(1) Integer maxDutyCount,
        List<String> excludeStatuses
) {
}
