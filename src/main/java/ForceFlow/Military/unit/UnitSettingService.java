package ForceFlow.Military.unit;

import ForceFlow.Military.dto.requestDto.UnitSettingRequest;
import ForceFlow.Military.dto.responseDto.UnitSettingResponse;
import ForceFlow.Military.entity.Unit;
import ForceFlow.Military.entity.UnitSetting;
import ForceFlow.Military.repository.UnitRepository;
import ForceFlow.Military.repository.UnitSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnitSettingService {

    private final UnitSettingRepository unitSettingRepository;
    private final UnitRepository unitRepository;

    @Transactional
    public UnitSettingResponse saveOrUpdateSetting(
            Long unitId,
            UnitSettingRequest request
    ) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("부대를 찾을 수 없습니다."));

        UnitSetting setting = unitSettingRepository.findByUnitId(unitId)
                .orElse(null);

        if (setting == null) {
            setting = UnitSetting.builder()
                    .unit(unit)
                    .dutyType(request.dutyType())
                    .requiredCount(request.requiredCount())
                    .startTime(request.startTime())
                    .endTime(request.endTime())
                    .lookbackDays(request.lookbackDays())
                    .preventConsecutive(request.preventConsecutive())
                    .maxDutyCount(request.maxDutyCount())
                    .excludeStatuses(request.excludeStatuses())
                    .build();

            UnitSetting savedSetting = unitSettingRepository.save(setting);
            return toResponse(savedSetting);
        }

        setting.update(
                request.dutyType(),
                request.requiredCount(),
                request.startTime(),
                request.endTime(),
                request.lookbackDays(),
                request.preventConsecutive(),
                request.maxDutyCount(),
                request.excludeStatuses()
        );

        return toResponse(setting);
    }

    private UnitSettingResponse toResponse(UnitSetting setting) {
        return new UnitSettingResponse(
                setting.getId(),
                setting.getUnit().getId(),
                setting.getDutyType(),
                setting.getRequiredCount(),
                setting.getStartTime(),
                setting.getEndTime(),
                setting.getLookbackDays(),
                setting.getPreventConsecutive(),
                setting.getMaxDutyCount(),
                setting.getExcludeStatusList(),
                setting.getCreatedAt(),
                setting.getUpdatedAt()
        );
    }

    // 테스트용
    public UnitSettingResponse getSetting(Long unitId) {
        UnitSetting setting = unitSettingRepository.findByUnitId(unitId)
                .orElseThrow(() -> new IllegalArgumentException("근무 설정을 찾을 수 없습니다."));

        return toResponse(setting);
    }
}