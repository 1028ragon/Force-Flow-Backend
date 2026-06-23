package ForceFlow.Military.dto.requestDto;

public record UnitCreateRequest(
        Long parentUnitId,
        String unitName,
        String unitType
) {}