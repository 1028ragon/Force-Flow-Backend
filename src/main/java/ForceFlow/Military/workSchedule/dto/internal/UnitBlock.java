package ForceFlow.Military.workSchedule.dto.internal;

public record UnitBlock(
        Long unitId,
        Long parentUnitId,
        String unitName,
        String unitType
) {
}
