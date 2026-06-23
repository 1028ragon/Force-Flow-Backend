package ForceFlow.Military.dto.requestDto;

public record UserCreateRequest(
        Long unitId,
        String serviceNumber,
        String name,
        String rankName,
        String role,
        String currentStatus,
        String phone
) {}