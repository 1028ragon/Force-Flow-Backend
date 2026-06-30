package ForceFlow.Military.dto.requestDto;

import java.time.LocalDate;

public record AiRecommendationCreateRequest(
        Long unitId,
        LocalDate dutyDate,
        String dutyType
) {}