package ForceFlow.Military.dto.responseDto;

import ForceFlow.Military.dto.AiRecommendedUser;

import java.util.List;

public record AiModelResponse(
        List<AiRecommendedUser> recommendedUsers,
        String warningMessage
) {}
