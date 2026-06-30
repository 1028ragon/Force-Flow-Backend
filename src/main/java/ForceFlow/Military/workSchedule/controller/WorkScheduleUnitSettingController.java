package ForceFlow.Military.workSchedule.controller;

import ForceFlow.Military.workSchedule.dto.WorkScheduleUnitSettingRequest;
import ForceFlow.Military.workSchedule.dto.WorkScheduleUnitSettingResponse;
import ForceFlow.Military.workSchedule.service.WorkScheduleUnitSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/work-schedules/units/{unitId}/setting")
public class WorkScheduleUnitSettingController {

    private final WorkScheduleUnitSettingService workScheduleUnitSettingService;

    @GetMapping
    public WorkScheduleUnitSettingResponse getSetting(
            @PathVariable Long unitId,
            @RequestParam String dutyType
    ) {
        return workScheduleUnitSettingService.getSetting(unitId, dutyType);
    }

    @PutMapping
    public WorkScheduleUnitSettingResponse saveSetting(
            @PathVariable Long unitId,
            @Valid @RequestBody WorkScheduleUnitSettingRequest request
    ) {
        return workScheduleUnitSettingService.saveSetting(unitId, request);
    }
}
