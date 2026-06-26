package ForceFlow.Military.unit;

import ForceFlow.Military.dto.requestDto.UnitCreateRequest;
import ForceFlow.Military.dto.responseDto.UnitResponse;
import ForceFlow.Military.entity.Unit;
import ForceFlow.Military.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnitService {

    private final UnitRepository unitRepository;

    @Transactional
    public UnitResponse createUnit(UnitCreateRequest request) {
        Unit parentUnit = null;

        if (request.parentUnitId() != null) {
            parentUnit = unitRepository.findById(request.parentUnitId())
                    .orElseThrow(() -> new IllegalArgumentException("상위 부대를 찾을 수 없습니다."));
        }

        Unit unit = Unit.builder()
                .parentUnit(parentUnit)
                .unitName(request.unitName())
                .unitType(request.unitType())
                .build();

        Unit savedUnit = unitRepository.save(unit);

        return toResponse(savedUnit);
    }

    public UnitResponse getUnit(Long unitId) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("부대를 찾을 수 없습니다."));

        return toResponse(unit);
    }

    private UnitResponse toResponse(Unit unit) {
        Long parentUnitId = null;

        if (unit.getParentUnit() != null) {
            parentUnitId = unit.getParentUnit().getId();
        }

        return new UnitResponse(
                unit.getId(),
                parentUnitId,
                unit.getUnitName(),
                unit.getUnitType(),
                unit.getCreatedAt()
        );
    }
}