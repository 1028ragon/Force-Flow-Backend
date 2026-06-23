package ForceFlow.Military.dto.responseDto;

import java.time.LocalDateTime;

public record UnitResponse(
        Long unitId,
        Long parentUnitId,
        String unitName,
        String unitType,
        LocalDateTime createdAt
) {}
