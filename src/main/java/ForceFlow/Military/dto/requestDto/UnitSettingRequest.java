package ForceFlow.Military.dto.requestDto;

import java.time.LocalTime;
import java.util.List;

public record UnitSettingRequest(
        String dutyType,
        Integer requiredCount,
        LocalTime startTime,
        LocalTime endTime,
        Integer lookbackDays,
        Boolean preventConsecutive,
        Integer maxDutyCount,
        List<String> excludeStatuses
) {}
