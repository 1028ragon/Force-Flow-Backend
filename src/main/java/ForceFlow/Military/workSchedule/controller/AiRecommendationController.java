package ForceFlow.Military.workSchedule.controller;

import ForceFlow.Military.dto.requestDto.AiRecommendationCreateRequest;
import ForceFlow.Military.dto.requestDto.AiRecommendationResponse;
import ForceFlow.Military.workSchedule.dto.WorkScheduleCandidateSearchResponse;
import ForceFlow.Military.workSchedule.dto.WorkScheduleUserDutyResponse;
import ForceFlow.Military.workSchedule.dto.WorkScheduleConfirmRequest;
import ForceFlow.Military.workSchedule.dto.WorkScheduleDailyResponse;
import ForceFlow.Military.workSchedule.dto.WorkSchedulePreviewResponse;
import ForceFlow.Military.workSchedule.service.AiRecommendationService;
import ForceFlow.Military.workSchedule.service.WorkScheduleCandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/work-schedules")
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;
    private final WorkScheduleCandidateService workScheduleCandidateService;

    @GetMapping
    public WorkScheduleDailyResponse getDailySchedule(
            @RequestParam Long unitId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dutyDate,
            @RequestParam(required = false) String dutyType
    ) {
        return aiRecommendationService.getDailySchedule(unitId, dutyDate, dutyType);
    }

    @GetMapping("/candidates")
    public WorkScheduleCandidateSearchResponse searchCandidates(
            @RequestParam Long unitId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dutyDate,
            @RequestParam String dutyType,
            @RequestParam(required = false) Integer slotOrder,
            @RequestParam(required = false) String keyword
    ) {
        return workScheduleCandidateService.searchCandidates(
                unitId,
                dutyDate,
                dutyType,
                slotOrder,
                keyword
        );
    }

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

    @GetMapping("/users/{userId}")
    public WorkScheduleUserDutyResponse getAssignmentByUserAndDate(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dutyDate
    ) {
        return aiRecommendationService.getAssignmentByUserAndDate(userId, dutyDate);
    }
}
