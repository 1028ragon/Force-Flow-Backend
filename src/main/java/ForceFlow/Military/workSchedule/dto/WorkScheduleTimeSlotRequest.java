package ForceFlow.Military.workSchedule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

public record WorkScheduleTimeSlotRequest(
        @NotNull @Min(1) Integer slotOrder,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull @Min(1) Integer requiredCount,
        @NotEmpty List<String> allowedRoles
) {
}
