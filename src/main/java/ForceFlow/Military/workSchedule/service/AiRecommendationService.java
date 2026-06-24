package ForceFlow.Military.workSchedule.service;

import ForceFlow.Military.workSchedule.dto.WorkScheduleConfirmRequest;
import ForceFlow.Military.workSchedule.dto.WorkSchedulePreviewResponse;
import ForceFlow.Military.dto.requestDto.AiRecommendationCreateRequest;
import ForceFlow.Military.dto.requestDto.AiRecommendationResponse;

public interface AiRecommendationService {

    WorkSchedulePreviewResponse previewSchedule(AiRecommendationCreateRequest request);

    AiRecommendationResponse confirmSchedule(WorkScheduleConfirmRequest request);
}
