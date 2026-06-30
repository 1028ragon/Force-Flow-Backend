package ForceFlow.Military.workSchedule.service;

import ForceFlow.Military.workSchedule.dto.WorkScheduleCandidateSearchResponse;
import java.time.LocalDate;

public interface WorkScheduleCandidateService {

    WorkScheduleCandidateSearchResponse searchCandidates(
            Long unitId,
            LocalDate dutyDate,
            String dutyType,
            Integer slotOrder,
            String keyword
    );
}
