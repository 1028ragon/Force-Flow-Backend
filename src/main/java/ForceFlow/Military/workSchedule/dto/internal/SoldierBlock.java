package ForceFlow.Military.workSchedule.dto.internal;

public record SoldierBlock(
        Long userId,
        String name,
        String rankName,
        String role,
        String currentStatus,
        Integer recentDutyCount,
        Integer recentDutyFatigueScore,
        Boolean workedYesterday,
        Boolean hasScheduleConflict,
        Boolean eligible
) {
}
