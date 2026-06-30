package ForceFlow.Military.dto.responseDto;

public record UserStatusResponse(
        Long userId,
        String name,
        String currentStatus
) {}
