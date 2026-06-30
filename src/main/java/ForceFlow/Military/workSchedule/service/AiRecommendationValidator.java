package ForceFlow.Military.workSchedule.service;

import ForceFlow.Military.dto.AiRecommendedUser;
import ForceFlow.Military.dto.responseDto.AiModelResponse;
import ForceFlow.Military.workSchedule.dto.internal.AiInternalRequest;
import ForceFlow.Military.workSchedule.dto.internal.SoldierBlock;
import ForceFlow.Military.workSchedule.exception.InvalidAiRecommendationException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AiRecommendationValidator {

    // AI 추천 결과는 그대로 저장하지 않고, 백엔드 기준으로 최소 유효성을 먼저 검증한다.
    public void validate(AiInternalRequest request, AiModelResponse response) {
        if (response == null) {
            throw new InvalidAiRecommendationException("AI 추천 응답이 비어 있습니다.");
        }
        if (response.recommendedUsers() == null) {
            throw new InvalidAiRecommendationException("AI 추천 사용자 목록이 비어 있습니다.");
        }
        if (request.duty().requiredCount() == null) {
            throw new InvalidAiRecommendationException("필요 근무 인원이 설정되지 않았습니다.");
        }
        if (response.recommendedUsers().size() != request.duty().requiredCount()) {
            throw new InvalidAiRecommendationException("AI 추천 인원 수가 필요 인원과 일치하지 않습니다.");
        }

        Map<Long, SoldierBlock> soldiersByUserId = request.soldiers().stream()
                .collect(Collectors.toMap(SoldierBlock::userId, Function.identity()));
        Set<Long> seenUserIds = new HashSet<>();

        for (AiRecommendedUser recommendedUser : response.recommendedUsers()) {
            validateRecommendedUser(recommendedUser, soldiersByUserId, seenUserIds);
        }
    }

    private void validateRecommendedUser(
            AiRecommendedUser recommendedUser,
            Map<Long, SoldierBlock> soldiersByUserId,
            Set<Long> seenUserIds
    ) {
        if (recommendedUser == null || recommendedUser.userId() == null) {
            throw new InvalidAiRecommendationException("AI 추천 사용자 ID가 비어 있습니다.");
        }
        if (!seenUserIds.add(recommendedUser.userId())) {
            throw new InvalidAiRecommendationException("AI 추천 사용자 ID가 중복되었습니다.");
        }

        SoldierBlock soldier = soldiersByUserId.get(recommendedUser.userId());
        if (soldier == null) {
            throw new InvalidAiRecommendationException("AI 추천 사용자가 요청 부대 병사 목록에 없습니다.");
        }
        if (!Boolean.TRUE.equals(soldier.eligible())) {
            throw new InvalidAiRecommendationException("AI가 부적격 병사를 추천했습니다.");
        }
        if (recommendedUser.reason() == null || recommendedUser.reason().isBlank()) {
            throw new InvalidAiRecommendationException("AI 추천 사유가 비어 있습니다.");
        }
    }
}
