package ForceFlow.Military.workSchedule.controller;

import ForceFlow.Military.dto.requestDto.AiRecommendationCreateRequest;
import ForceFlow.Military.dto.requestDto.AiRecommendationResponse;
import ForceFlow.Military.workSchedule.dto.WorkScheduleConfirmRequest;
import ForceFlow.Military.workSchedule.dto.WorkSchedulePreviewResponse;
import ForceFlow.Military.workSchedule.service.AiRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/work-schedules")
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;

    @PostMapping("/preview")
    public WorkSchedulePreviewResponse previewSchedule(
            @RequestBody AiRecommendationCreateRequest request
    ) {
        return aiRecommendationService.previewSchedule(request);
    }

    @PostMapping("/confirm")
    public AiRecommendationResponse confirmSchedule(
            @Valid @RequestBody WorkScheduleConfirmRequest request
    ) {
        return aiRecommendationService.confirmSchedule(request);
    }
}
