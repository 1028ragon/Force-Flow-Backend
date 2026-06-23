package ForceFlow.Military.dto.responseDto;

import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        Long unitId,
        String serviceNumber,
        String name,
        String rankName,
        String role,
        String currentStatus,
        String phone,
        LocalDateTime createdAt
) {}