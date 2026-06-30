package ForceFlow.Military.dto.requestDto;

import ForceFlow.Military.dto.responseDto.DutyAssignmentResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record AiRecommendationResponse(
        Long recommendationId,
        Long unitId,
        LocalDate dutyDate,
        String dutyType,
        Integer requiredCount,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String warningMessage,
        List<DutyAssignmentResponse> assignments,
        LocalDateTime createdAt
) {}