package ForceFlow.Military.workSchedule.service;

import ForceFlow.Military.dto.AiRecommendedUser;
import ForceFlow.Military.dto.requestDto.AiRecommendationCreateRequest;
import ForceFlow.Military.dto.requestDto.AiRecommendationResponse;
import ForceFlow.Military.dto.responseDto.AiModelResponse;
import ForceFlow.Military.dto.responseDto.DutyAssignmentResponse;
import ForceFlow.Military.entity.AiRecommendation;
import ForceFlow.Military.entity.DutyAssignment;
import ForceFlow.Military.entity.Unit;
import ForceFlow.Military.entity.UnitSetting;
import ForceFlow.Military.entity.User;
import ForceFlow.Military.repository.AiRecommendationRepository;
import ForceFlow.Military.repository.DutyAssignmentRepository;
import ForceFlow.Military.repository.ScheduleRepository;
import ForceFlow.Military.repository.UnitRepository;
import ForceFlow.Military.repository.UnitSettingRepository;
import ForceFlow.Military.repository.UserRepository;
import ForceFlow.Military.workSchedule.constant.DutyStatus;
import ForceFlow.Military.workSchedule.dto.WorkScheduleConfirmAssignmentRequest;
import ForceFlow.Military.workSchedule.dto.WorkScheduleConfirmRequest;
import ForceFlow.Military.workSchedule.dto.WorkSchedulePreviewAssignmentResponse;
import ForceFlow.Military.workSchedule.dto.WorkSchedulePreviewResponse;
import ForceFlow.Military.workSchedule.dto.internal.AiInternalRequest;
import ForceFlow.Military.workSchedule.dto.internal.SoldierBlock;
import ForceFlow.Military.workSchedule.exception.WorkScheduleServiceException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private final AiInternalRequestBuilder aiInternalRequestBuilder;
    private final OpenAiScheduleClient openAiScheduleClient;
    private final AiRecommendationValidator aiRecommendationValidator;
    private final ObjectMapper objectMapper;
    private final UnitRepository unitRepository;
    private final UnitSettingRepository unitSettingRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final DutyAssignmentRepository dutyAssignmentRepository;

    @Override
    @Transactional
    public WorkSchedulePreviewResponse previewSchedule(AiRecommendationCreateRequest request) {
        AiInternalRequest internalRequest = aiInternalRequestBuilder.build(request);
        AiModelResponse aiResponse = openAiScheduleClient.recommend(internalRequest);
        aiRecommendationValidator.validate(internalRequest, aiResponse);
        String requestJson = writeJson(internalRequest);
        String responseJson = writeJson(aiResponse);
        AiRecommendation aiRecommendation = aiRecommendationRepository.save(
                toPreviewRecommendation(internalRequest, aiResponse, requestJson, responseJson)
        );

        return toPreviewResponse(
                aiRecommendation.getId(),
                internalRequest,
                aiResponse,
                requestJson,
                responseJson
        );
    }

    @Override
    @Transactional
    public AiRecommendationResponse confirmSchedule(WorkScheduleConfirmRequest request) {
        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new IllegalArgumentException("부대를 찾을 수 없습니다."));
        UnitSetting setting = unitSettingRepository.findByUnitId(request.unitId())
                .orElseThrow(() -> new IllegalArgumentException("부대 근무 설정을 찾을 수 없습니다."));
        validateConfirmRequest(request, setting);

        Map<Long, User> usersById = getUsersById(request);
        Set<Long> allowedUnitIds = new HashSet<>(unitRepository.findAllSubUnitIds(request.unitId()));
        validateConfirmAssignments(request, setting, usersById, allowedUnitIds);

        AiRecommendation aiRecommendation = resolveAiRecommendation(request, unit);
        List<DutyAssignment> assignments = request.assignments().stream()
                .map(assignmentRequest -> toApprovedAssignment(
                        assignmentRequest,
                        aiRecommendation,
                        unit,
                        usersById.get(assignmentRequest.userId())
                ))
                .toList();
        List<DutyAssignment> savedAssignments = dutyAssignmentRepository.saveAll(assignments);

        return toAiRecommendationResponse(aiRecommendation, savedAssignments);
    }

    private WorkSchedulePreviewResponse toPreviewResponse(
            Long recommendationId,
            AiInternalRequest internalRequest,
            AiModelResponse aiResponse,
            String requestJson,
            String responseJson
    ) {
        List<WorkSchedulePreviewAssignmentResponse> assignments = toPreviewAssignments(
                internalRequest,
                aiResponse.recommendedUsers()
        );

        return new WorkSchedulePreviewResponse(
                recommendationId,
                internalRequest.unit().unitId(),
                internalRequest.duty().dutyDate(),
                internalRequest.duty().dutyType(),
                internalRequest.duty().requiredCount(),
                internalRequest.duty().startTime(),
                internalRequest.duty().endTime(),
                DutyStatus.PREVIEW,
                aiResponse.warningMessage(),
                assignments,
                requestJson,
                responseJson
        );
    }

    private AiRecommendation toPreviewRecommendation(
            AiInternalRequest internalRequest,
            AiModelResponse aiResponse,
            String requestJson,
            String responseJson
    ) {
        Unit unit = unitRepository.findById(internalRequest.unit().unitId())
                .orElseThrow(() -> new IllegalArgumentException("부대를 찾을 수 없습니다."));

        return AiRecommendation.builder()
                .unit(unit)
                .dutyDate(internalRequest.duty().dutyDate())
                .dutyType(internalRequest.duty().dutyType())
                .requiredCount(internalRequest.duty().requiredCount())
                .startTime(internalRequest.duty().startTime())
                .endTime(internalRequest.duty().endTime())
                .status(DutyStatus.PREVIEW)
                .warningMessage(aiResponse.warningMessage())
                .requestJson(requestJson)
                .responseJson(responseJson)
                .build();
    }

    private List<WorkSchedulePreviewAssignmentResponse> toPreviewAssignments(
            AiInternalRequest internalRequest,
            List<AiRecommendedUser> recommendedUsers
    ) {
        Map<Long, SoldierBlock> soldiersByUserId = internalRequest.soldiers().stream()
                .collect(Collectors.toMap(SoldierBlock::userId, Function.identity()));

        return recommendedUsers.stream()
                .map(recommendedUser -> toPreviewAssignment(
                        internalRequest,
                        recommendedUser,
                        soldiersByUserId.get(recommendedUser.userId())
                ))
                .toList();
    }

    private WorkSchedulePreviewAssignmentResponse toPreviewAssignment(
            AiInternalRequest internalRequest,
            AiRecommendedUser recommendedUser,
            SoldierBlock soldier
    ) {
        return new WorkSchedulePreviewAssignmentResponse(
                soldier.userId(),
                internalRequest.unit().unitId(),
                soldier.name(),
                soldier.rankName(),
                soldier.role(),
                internalRequest.duty().dutyDate(),
                internalRequest.duty().dutyType(),
                internalRequest.duty().startTime(),
                internalRequest.duty().endTime(),
                DutyStatus.PREVIEW,
                recommendedUser.reason()
        );
    }

    private void validateConfirmRequest(WorkScheduleConfirmRequest request, UnitSetting setting) {
        if (!setting.getDutyType().equals(request.dutyType())) {
            throw new IllegalArgumentException("요청한 근무 유형과 부대 근무 설정이 일치하지 않습니다.");
        }
        if (request.assignments().size() != request.requiredCount()) {
            throw new IllegalArgumentException("승인 배정 인원 수가 필요 인원과 일치하지 않습니다.");
        }
        if (request.recommendationId() == null && isBlank(request.requestJson())) {
            throw new IllegalArgumentException("AI 내부 요청 JSON이 비어 있습니다.");
        }
    }

    private Map<Long, User> getUsersById(WorkScheduleConfirmRequest request) {
        List<Long> userIds = request.assignments().stream()
                .map(WorkScheduleConfirmAssignmentRequest::userId)
                .distinct()
                .toList();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private void validateConfirmAssignments(
            WorkScheduleConfirmRequest request,
            UnitSetting setting,
            Map<Long, User> usersById,
            Set<Long> allowedUnitIds
    ) {
        Set<String> excludedStatuses = setting.getExcludeStatusList().stream()
                .map(String::trim)
                .collect(Collectors.toSet());
        Set<Long> seenUserIds = new HashSet<>();

        for (WorkScheduleConfirmAssignmentRequest assignment : request.assignments()) {
            validateConfirmAssignment(
                    request,
                    assignment,
                    setting,
                    usersById,
                    allowedUnitIds,
                    excludedStatuses,
                    seenUserIds
            );
        }
    }

    private void validateConfirmAssignment(
            WorkScheduleConfirmRequest request,
            WorkScheduleConfirmAssignmentRequest assignment,
            UnitSetting setting,
            Map<Long, User> usersById,
            Set<Long> allowedUnitIds,
            Set<String> excludedStatuses,
            Set<Long> seenUserIds
    ) {
        if (!seenUserIds.add(assignment.userId())) {
            throw new IllegalArgumentException("동일 병사가 중복 배정되었습니다.");
        }
        if (!request.unitId().equals(assignment.unitId())
                || !request.dutyDate().equals(assignment.dutyDate())
                || !request.dutyType().equals(assignment.dutyType())) {
            throw new IllegalArgumentException("승인 배정 정보가 근무표 기본 정보와 일치하지 않습니다.");
        }

        User user = usersById.get(assignment.userId());
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 병사가 포함되어 있습니다.");
        }
        if (!allowedUnitIds.contains(user.getUnit().getId())) {
            throw new IllegalArgumentException("요청 부대 소속이 아닌 병사가 포함되어 있습니다.");
        }
        if (!assignment.role().equals(user.getRole())) {
            throw new IllegalArgumentException("병사 역할과 배정 역할이 일치하지 않습니다.");
        }
        if (excludedStatuses.contains(user.getCurrentStatus())) {
            throw new IllegalArgumentException("제외 상태의 병사가 포함되어 있습니다.");
        }
        if (hasScheduleConflict(assignment)) {
            throw new IllegalArgumentException("일정 충돌이 있는 병사가 포함되어 있습니다.");
        }
        if (hasExistingApprovedDuty(assignment)) {
            throw new IllegalArgumentException("이미 승인된 동일 근무가 있는 병사가 포함되어 있습니다.");
        }
        if (Boolean.TRUE.equals(setting.getPreventConsecutive()) && hasApprovedDutyYesterday(assignment)) {
            throw new IllegalArgumentException("전날 승인 근무자가 포함되어 있습니다.");
        }
    }

    private boolean hasScheduleConflict(WorkScheduleConfirmAssignmentRequest assignment) {
        return scheduleRepository.existsByUserIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                assignment.userId(),
                resolveDutyEndAt(assignment),
                assignment.dutyDate().atTime(assignment.startTime())
        );
    }

    private boolean hasExistingApprovedDuty(WorkScheduleConfirmAssignmentRequest assignment) {
        return dutyAssignmentRepository.existsByUserIdAndDutyDateAndStatus(
                assignment.userId(),
                assignment.dutyDate(),
                DutyStatus.APPROVED
        );
    }

    private boolean hasApprovedDutyYesterday(WorkScheduleConfirmAssignmentRequest assignment) {
        return dutyAssignmentRepository.existsByUserIdAndDutyDateAndStatus(
                assignment.userId(),
                assignment.dutyDate().minusDays(1),
                DutyStatus.APPROVED
        );
    }

    private LocalDateTime resolveDutyEndAt(WorkScheduleConfirmAssignmentRequest assignment) {
        LocalDate endDate = assignment.endTime().isAfter(assignment.startTime())
                ? assignment.dutyDate()
                : assignment.dutyDate().plusDays(1);
        return endDate.atTime(assignment.endTime());
    }

    private AiRecommendation toAiRecommendation(WorkScheduleConfirmRequest request, Unit unit) {
        String responseJson = isBlank(request.responseJson()) ? writeJson(request.assignments()) : request.responseJson();

        return AiRecommendation.builder()
                .unit(unit)
                .dutyDate(request.dutyDate())
                .dutyType(request.dutyType())
                .requiredCount(request.requiredCount())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(DutyStatus.APPROVED)
                .warningMessage(null)
                .requestJson(request.requestJson())
                .responseJson(responseJson)
                .build();
    }

    private AiRecommendation resolveAiRecommendation(WorkScheduleConfirmRequest request, Unit unit) {
        if (request.recommendationId() == null) {
            return aiRecommendationRepository.save(toAiRecommendation(request, unit));
        }

        AiRecommendation aiRecommendation = aiRecommendationRepository.findById(request.recommendationId())
                .orElseThrow(() -> new IllegalArgumentException("AI 추천 기록을 찾을 수 없습니다."));
        validateRecommendationMatchesRequest(aiRecommendation, request);
        validateConfirmAssignmentsMatchPreview(aiRecommendation, request);
        return aiRecommendationRepository.save(toApprovedRecommendation(aiRecommendation));
    }

    private AiRecommendation toApprovedRecommendation(AiRecommendation aiRecommendation) {
        return AiRecommendation.builder()
                .unit(aiRecommendation.getUnit())
                .dutyDate(aiRecommendation.getDutyDate())
                .dutyType(aiRecommendation.getDutyType())
                .requiredCount(aiRecommendation.getRequiredCount())
                .startTime(aiRecommendation.getStartTime())
                .endTime(aiRecommendation.getEndTime())
                .status(DutyStatus.APPROVED)
                .warningMessage(aiRecommendation.getWarningMessage())
                .requestJson(aiRecommendation.getRequestJson())
                .responseJson(aiRecommendation.getResponseJson())
                .build();
    }

    private void validateRecommendationMatchesRequest(
            AiRecommendation aiRecommendation,
            WorkScheduleConfirmRequest request
    ) {
        if (!aiRecommendation.getUnit().getId().equals(request.unitId())
                || !aiRecommendation.getDutyDate().equals(request.dutyDate())
                || !aiRecommendation.getDutyType().equals(request.dutyType())
                || !aiRecommendation.getRequiredCount().equals(request.requiredCount())
                || !aiRecommendation.getStartTime().equals(request.startTime())
                || !aiRecommendation.getEndTime().equals(request.endTime())) {
            throw new IllegalArgumentException("AI 추천 기록과 확정 요청 정보가 일치하지 않습니다.");
        }
        if (DutyStatus.APPROVED.equals(aiRecommendation.getStatus())) {
            throw new IllegalArgumentException("이미 승인된 AI 추천 기록입니다.");
        }
    }

    private void validateConfirmAssignmentsMatchPreview(
            AiRecommendation aiRecommendation,
            WorkScheduleConfirmRequest request
    ) {
        AiModelResponse aiResponse = readAiModelResponse(aiRecommendation.getResponseJson());
        Set<Long> previewUserIds = aiResponse.recommendedUsers().stream()
                .map(AiRecommendedUser::userId)
                .collect(Collectors.toSet());
        Set<Long> confirmUserIds = request.assignments().stream()
                .map(WorkScheduleConfirmAssignmentRequest::userId)
                .collect(Collectors.toSet());

        if (!previewUserIds.equals(confirmUserIds)) {
            throw new IllegalArgumentException("확정 배정 인원이 미리보기 추천 결과와 일치하지 않습니다.");
        }
    }

    private AiModelResponse readAiModelResponse(String responseJson) {
        if (isBlank(responseJson)) {
            throw new IllegalArgumentException("AI 추천 응답 JSON이 비어 있습니다.");
        }

        try {
            return objectMapper.readValue(responseJson, AiModelResponse.class);
        } catch (Exception exception) {
            throw new WorkScheduleServiceException("AI 추천 응답 JSON 처리에 실패했습니다.", exception);
        }
    }

    private DutyAssignment toApprovedAssignment(
            WorkScheduleConfirmAssignmentRequest request,
            AiRecommendation aiRecommendation,
            Unit unit,
            User user
    ) {
        DutyAssignment assignment = DutyAssignment.builder()
                .aiRecommendation(aiRecommendation)
                .user(user)
                .unit(unit)
                .dutyDate(request.dutyDate())
                .dutyType(request.dutyType())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(DutyStatus.APPROVED)
                .aiReason(request.aiReason())
                .build();
        assignment.approve();
        return assignment;
    }

    private AiRecommendationResponse toAiRecommendationResponse(
            AiRecommendation aiRecommendation,
            List<DutyAssignment> assignments
    ) {
        return new AiRecommendationResponse(
                aiRecommendation.getId(),
                aiRecommendation.getUnit().getId(),
                aiRecommendation.getDutyDate(),
                aiRecommendation.getDutyType(),
                aiRecommendation.getRequiredCount(),
                aiRecommendation.getStartTime(),
                aiRecommendation.getEndTime(),
                aiRecommendation.getStatus(),
                aiRecommendation.getWarningMessage(),
                assignments.stream()
                        .map(this::toDutyAssignmentResponse)
                        .toList(),
                aiRecommendation.getCreatedAt()
        );
    }

    private DutyAssignmentResponse toDutyAssignmentResponse(DutyAssignment assignment) {
        return new DutyAssignmentResponse(
                assignment.getId(),
                assignment.getUser().getId(),
                assignment.getUnit().getId(),
                assignment.getUser().getName(),
                assignment.getUser().getRankName(),
                assignment.getDutyDate(),
                assignment.getDutyType(),
                assignment.getStartTime(),
                assignment.getEndTime(),
                assignment.getStatus(),
                assignment.getAiReason(),
                assignment.getApprovedAt(),
                assignment.getCreatedAt()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new WorkScheduleServiceException("근무표 JSON 생성에 실패했습니다.", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
