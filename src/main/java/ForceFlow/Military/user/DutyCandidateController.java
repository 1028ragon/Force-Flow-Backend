package ForceFlow.Military.user;

import ForceFlow.Military.dto.responseDto.UnitSettingResponse;
import ForceFlow.Military.dto.responseDto.UserResponse;
import ForceFlow.Military.service.DutyCandidateQueryService;
import ForceFlow.Military.unit.UnitSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * [소영 담당 테스트/검증용 Controller]
 *
 * DutyCandidateQueryService가 정상 동작하는지 검증하기 위해 생성한 API.
 *
 * 실제 AI 추천 기능에서는 민구 담당의 AiRecommendationService가
 * DutyCandidateQueryService를 직접 호출하여 사용한다.
 *
 * 따라서 현재 Controller는 테스트 및 후보 인원 조회용으로 유지한다.
 * 삭제하지 말고 필요에 따라 AI 추천 API와 연동하거나
 * 별도 조회 API로 활용 가능하다.
 */
@RestController
@RequiredArgsConstructor
public class DutyCandidateController {

    private final DutyCandidateQueryService dutyCandidateQueryService;
    private final UnitSettingService unitSettingService;

    /**
     * 근무 가능 인원 조회
     *
     * 사용 목적: AI 추천 생성 전에 근무 가능 인원 후보군을 조회한다.
     * 1. DutyCandidateQueryService 검증
     * 2. 프론트에서 후보 인원 미리보기
     * 3. AI 추천 전 후보 인원 확인
     *
     * B 담당 참고:
     * AiRecommendationService 내부에서
     * dutyCandidateQueryService.findAvailableUsers(...)
     * 를 직접 호출하여 사용 가능
     */
    @GetMapping("/api/units/{unitId}/available-users")
    public ResponseEntity<List<UserResponse>> getAvailableUsers(
            @PathVariable Long unitId,
            @RequestParam LocalDate dutyDate,
            @RequestParam LocalTime startTime,
            @RequestParam LocalTime endTime
    ) {
        UnitSettingResponse setting = unitSettingService.getSetting(unitId);

        List<UserResponse> response = dutyCandidateQueryService.findAvailableUsers(
                unitId,
                dutyDate,
                startTime,
                endTime,
                setting
        );

        return ResponseEntity.ok(response);
    }
}