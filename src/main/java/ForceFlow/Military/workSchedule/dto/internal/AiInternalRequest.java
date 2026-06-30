package ForceFlow.Military.workSchedule.dto.internal;

import java.util.List;

public record AiInternalRequest(
        UnitBlock unit,
        DutyBlock duty,
        RuleBlock rules,
        List<SoldierBlock> soldiers
) {
}
