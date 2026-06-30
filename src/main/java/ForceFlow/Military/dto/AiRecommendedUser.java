package ForceFlow.Military.dto;

public record AiRecommendedUser(
        Long userId,
        Integer score,
        String reason
) {}
