package ForceFlow.Military.service;

import ForceFlow.Military.dto.responseDto.UnitSettingResponse;
import ForceFlow.Military.dto.responseDto.UserResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface DutyCandidateQueryService {

    List<UserResponse> findAvailableUsers(
            Long unitId,
            LocalDate dutyDate,
            LocalTime startTime,
            LocalTime endTime,
            UnitSettingResponse setting
    );
}
