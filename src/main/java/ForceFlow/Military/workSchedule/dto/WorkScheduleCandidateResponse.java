package ForceFlow.Military.workSchedule.dto;

public record WorkScheduleCandidateResponse(
        Long userId,
        Long unitId,
        String name,
        String rankName,
        String role,
        String currentStatus,
        Integer recentDutyCount,
        Boolean workedYesterday,
        Boolean hasScheduleConflict,
        Boolean eligible
) {
}
