package ForceFlow.Military.workSchedule.service;

import ForceFlow.Military.workSchedule.dto.WorkScheduleConfirmRequest;
import ForceFlow.Military.workSchedule.dto.WorkScheduleUserDutyResponse;
import ForceFlow.Military.workSchedule.dto.WorkScheduleDailyResponse;
import ForceFlow.Military.workSchedule.dto.WorkSchedulePreviewResponse;
import ForceFlow.Military.dto.requestDto.AiRecommendationCreateRequest;
import ForceFlow.Military.dto.requestDto.AiRecommendationResponse;
import java.time.LocalDate;

public interface AiRecommendationService {

    WorkSchedulePreviewResponse previewSchedule(AiRecommendationCreateRequest request);

    AiRecommendationResponse confirmSchedule(WorkScheduleConfirmRequest request);

    WorkScheduleDailyResponse getDailySchedule(Long unitId, LocalDate dutyDate, String dutyType);

    WorkScheduleUserDutyResponse getAssignmentByUserAndDate(Long userId, LocalDate dutyDate);
}
