package ForceFlow.Military.workSchedule.service;

import ForceFlow.Military.entity.Unit;
import ForceFlow.Military.entity.UnitSetting;
import ForceFlow.Military.repository.UnitRepository;
import ForceFlow.Military.repository.UnitSettingRepository;
import ForceFlow.Military.workSchedule.dto.WorkScheduleUnitSettingRequest;
import ForceFlow.Military.workSchedule.dto.WorkScheduleUnitSettingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkScheduleUnitSettingServiceImpl implements WorkScheduleUnitSettingService {

    private final UnitRepository unitRepository;
    private final UnitSettingRepository unitSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public WorkScheduleUnitSettingResponse getSetting(Long unitId) {
        UnitSetting setting = unitSettingRepository.findByUnitId(unitId)
                .orElseThrow(() -> new IllegalArgumentException("부대 근무 설정을 찾을 수 없습니다."));

        return toResponse(setting);
    }

    @Override
    @Transactional
    public WorkScheduleUnitSettingResponse saveSetting(Long unitId, WorkScheduleUnitSettingRequest request) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("부대를 찾을 수 없습니다."));

        UnitSetting setting = unitSettingRepository.findByUnitId(unitId)
                .map(existingSetting -> updateSetting(existingSetting, request))
                .orElseGet(() -> createSetting(unit, request));

        UnitSetting savedSetting = unitSettingRepository.save(setting);
        return toResponse(savedSetting);
    }

    private UnitSetting createSetting(Unit unit, WorkScheduleUnitSettingRequest request) {
        return UnitSetting.builder()
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
    }

    private UnitSetting updateSetting(UnitSetting setting, WorkScheduleUnitSettingRequest request) {
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
        return setting;
    }

    private WorkScheduleUnitSettingResponse toResponse(UnitSetting setting) {
        return new WorkScheduleUnitSettingResponse(
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
}
