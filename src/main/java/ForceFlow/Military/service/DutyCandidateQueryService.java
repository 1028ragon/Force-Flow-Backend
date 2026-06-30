package ForceFlow.Military.service;

import ForceFlow.Military.dto.responseDto.UnitSettingResponse;
import ForceFlow.Military.dto.responseDto.UserResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface DutyCandidateQueryService {

    // 특정 부대의 근무 가능 인원을 공통 규칙으로 필터링한다.
    List<UserResponse> findAvailableUsers(
            Long unitId,
            LocalDate dutyDate,
            LocalTime startTime,
            LocalTime endTime,
            UnitSettingResponse setting
    );
}
