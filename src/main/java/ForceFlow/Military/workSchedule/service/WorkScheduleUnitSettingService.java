package ForceFlow.Military.workSchedule.service;

import ForceFlow.Military.workSchedule.dto.WorkScheduleUnitSettingRequest;
import ForceFlow.Military.workSchedule.dto.WorkScheduleUnitSettingResponse;

public interface WorkScheduleUnitSettingService {

    WorkScheduleUnitSettingResponse getSetting(Long unitId, String dutyType);

    WorkScheduleUnitSettingResponse saveSetting(Long unitId, WorkScheduleUnitSettingRequest request);
}
