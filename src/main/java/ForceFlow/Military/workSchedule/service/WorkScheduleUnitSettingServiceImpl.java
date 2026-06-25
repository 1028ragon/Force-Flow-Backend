package ForceFlow.Military.workSchedule.service;

import ForceFlow.Military.entity.Unit;
import ForceFlow.Military.workSchedule.dto.WorkScheduleTimeSlotRequest;
import ForceFlow.Military.workSchedule.dto.WorkScheduleTimeSlotResponse;
import ForceFlow.Military.workSchedule.dto.WorkScheduleUnitSettingRequest;
import ForceFlow.Military.workSchedule.dto.WorkScheduleUnitSettingResponse;
import ForceFlow.Military.workSchedule.entity.WorkScheduleSetting;
import ForceFlow.Military.workSchedule.entity.WorkScheduleSlotRole;
import ForceFlow.Military.workSchedule.entity.WorkScheduleTimeSlot;
import ForceFlow.Military.workSchedule.repository.WorkScheduleSettingRepository;
import jakarta.persistence.EntityManager;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkScheduleUnitSettingServiceImpl implements WorkScheduleUnitSettingService {

    private final EntityManager entityManager;
    private final WorkScheduleSettingRepository workScheduleSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public WorkScheduleUnitSettingResponse getSetting(Long unitId, String dutyType) {
        WorkScheduleSetting setting = workScheduleSettingRepository.findByUnitIdAndDutyType(unitId, dutyType)
                .orElseThrow(() -> new IllegalArgumentException("부대 근무 설정을 찾을 수 없습니다."));

        return toResponse(setting);
    }

    @Override
    @Transactional
    public WorkScheduleUnitSettingResponse saveSetting(Long unitId, WorkScheduleUnitSettingRequest request) {
        Unit unit = entityManager.find(Unit.class, unitId);
        if (unit == null) {
            throw new IllegalArgumentException("부대를 찾을 수 없습니다.");
        }

        WorkScheduleSetting setting = workScheduleSettingRepository.findByUnitIdAndDutyType(unitId, request.dutyType())
                .map(existingSetting -> updateSetting(existingSetting, request))
                .orElseGet(() -> createSetting(unit, request));

        WorkScheduleSetting savedSetting = workScheduleSettingRepository.save(setting);
        return toResponse(savedSetting);
    }

    private WorkScheduleSetting createSetting(Unit unit, WorkScheduleUnitSettingRequest request) {
        WorkScheduleSetting setting = new WorkScheduleSetting(
                unit,
                request.dutyType(),
                request.description(),
                request.lookbackDays(),
                request.preventConsecutive(),
                request.maxDutyCount(),
                request.excludeStatuses()
        );
        setting.replaceTimeSlots(toTimeSlots(request.timeSlots()));
        return setting;
    }

    private WorkScheduleSetting updateSetting(WorkScheduleSetting setting, WorkScheduleUnitSettingRequest request) {
        setting.update(
                request.dutyType(),
                request.description(),
                request.lookbackDays(),
                request.preventConsecutive(),
                request.maxDutyCount(),
                request.excludeStatuses()
        );
        setting.replaceTimeSlots(toTimeSlots(request.timeSlots()));
        return setting;
    }

    private List<WorkScheduleTimeSlot> toTimeSlots(List<WorkScheduleTimeSlotRequest> requests) {
        return requests.stream()
                .sorted(Comparator.comparing(WorkScheduleTimeSlotRequest::slotOrder))
                .map(this::toTimeSlot)
                .toList();
    }

    private WorkScheduleTimeSlot toTimeSlot(WorkScheduleTimeSlotRequest request) {
        return new WorkScheduleTimeSlot(
                request.slotOrder(),
                request.startTime(),
                request.endTime(),
                request.requiredCount(),
                toSlotRoles(request.allowedRoles())
        );
    }

    private List<WorkScheduleSlotRole> toSlotRoles(List<String> roles) {
        return roles.stream()
                .map(WorkScheduleSlotRole::new)
                .toList();
    }

    private WorkScheduleUnitSettingResponse toResponse(WorkScheduleSetting setting) {
        return new WorkScheduleUnitSettingResponse(
                setting.getId(),
                setting.getUnit().getId(),
                setting.getDutyType(),
                setting.getDescription(),
                setting.getRequiredCount(),
                setting.getStartTime(),
                setting.getEndTime(),
                toTimeSlotResponses(setting),
                setting.getLookbackDays(),
                setting.getPreventConsecutive(),
                setting.getMaxDutyCount(),
                setting.getExcludeStatusList(),
                setting.getCreatedAt(),
                setting.getUpdatedAt()
        );
    }

    private List<WorkScheduleTimeSlotResponse> toTimeSlotResponses(WorkScheduleSetting setting) {
        return setting.getTimeSlots().stream()
                .sorted(Comparator.comparing(WorkScheduleTimeSlot::getSlotOrder))
                .map(this::toTimeSlotResponse)
                .toList();
    }

    private WorkScheduleTimeSlotResponse toTimeSlotResponse(WorkScheduleTimeSlot timeSlot) {
        List<String> allowedRoles = timeSlot.getAllowedRoles().stream()
                .map(WorkScheduleSlotRole::getRole)
                .toList();

        return new WorkScheduleTimeSlotResponse(
                timeSlot.getId(),
                timeSlot.getSlotOrder(),
                timeSlot.getStartTime(),
                timeSlot.getEndTime(),
                timeSlot.getRequiredCount(),
                allowedRoles
        );
    }
}
