package ForceFlow.Military.workSchedule.dto.internal;

import java.util.List;

public record RuleBlock(
        Integer lookbackDays,
        Boolean preventConsecutive,
        Integer maxDutyCount,
        List<String> excludeStatuses
) {
}
