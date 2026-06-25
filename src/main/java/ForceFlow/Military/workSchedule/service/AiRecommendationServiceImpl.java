package ForceFlow.Military.workSchedule.service;

import ForceFlow.Military.dto.AiRecommendedUser;
import ForceFlow.Military.dto.requestDto.AiRecommendationCreateRequest;
import ForceFlow.Military.dto.requestDto.AiRecommendationResponse;
import ForceFlow.Military.dto.responseDto.AiModelResponse;
import ForceFlow.Military.dto.responseDto.DutyAssignmentResponse;
import ForceFlow.Military.entity.AiRecommendation;
import ForceFlow.Military.entity.DutyAssignment;
import ForceFlow.Military.entity.Unit;
import ForceFlow.Military.entity.User;
import ForceFlow.Military.repository.AiRecommendationRepository;
import ForceFlow.Military.repository.DutyAssignmentRepository;
import ForceFlow.Military.repository.ScheduleRepository;
import ForceFlow.Military.repository.UnitRepository;
import ForceFlow.Military.repository.UserRepository;
import ForceFlow.Military.workSchedule.constant.DutyStatus;
import ForceFlow.Military.workSchedule.dto.WorkScheduleConfirmAssignmentRequest;
import ForceFlow.Military.workSchedule.dto.WorkScheduleConfirmRequest;
import ForceFlow.Military.workSchedule.dto.WorkScheduleAssignmentResponse;
import ForceFlow.Military.workSchedule.dto.WorkScheduleDailyResponse;
import ForceFlow.Military.workSchedule.dto.WorkSchedulePreviewAssignmentResponse;
import ForceFlow.Military.workSchedule.dto.WorkSchedulePreviewResponse;
import ForceFlow.Military.workSchedule.dto.internal.AiInternalRequest;
import ForceFlow.Military.workSchedule.dto.internal.DutySlotBlock;
import ForceFlow.Military.workSchedule.dto.internal.SoldierBlock;
import ForceFlow.Military.workSchedule.entity.WorkScheduleSetting;
import ForceFlow.Military.workSchedule.exception.WorkScheduleServiceException;
import ForceFlow.Military.workSchedule.repository.WorkScheduleSettingRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    private static final String PRIVATE_RANK_NAME = "이병";

    private final AiInternalRequestBuilder aiInternalRequestBuilder;
    private final OpenAiScheduleClient openAiScheduleClient;
    private final AiRecommendationValidator aiRecommendationValidator;
    private final ObjectMapper objectMapper;
    private final UnitRepository unitRepository;
    private final WorkScheduleSettingRepository workScheduleSettingRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final DutyAssignmentRepository dutyAssignmentRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public WorkSchedulePreviewResponse previewSchedule(AiRecommendationCreateRequest request) {
        AiInternalRequest internalRequest = aiInternalRequestBuilder.build(request);
        AiModelResponse aiResponse = openAiScheduleClient.recommend(internalRequest);
        // AI는 추천만 생성하므로, 백엔드에서 추천 인원 수/중복/부적격 병사 여부를 검증한다.
        aiRecommendationValidator.validate(internalRequest, aiResponse);
        String requestJson = writeJson(internalRequest);
        String responseJson = writeJson(aiResponse);
        AiRecommendation aiRecommendation = aiRecommendationRepository.save(
                toPreviewRecommendation(internalRequest, aiResponse, requestJson, responseJson)
        );

        return toPreviewResponse(
                aiRecommendation.getId(),
                internalRequest,
                aiResponse
        );
    }

    @Override
    @Transactional
    public AiRecommendationResponse confirmSchedule(WorkScheduleConfirmRequest request) {
        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new IllegalArgumentException("부대를 찾을 수 없습니다."));
        WorkScheduleSetting setting = workScheduleSettingRepository.findByUnitIdAndDutyType(
                        request.unitId(),
                        request.dutyType()
                )
                .orElseThrow(() -> new IllegalArgumentException("부대 근무 설정을 찾을 수 없습니다."));
        // 프론트에서 preview 인원을 수정할 수 있으므로, confirm 단계에서 최종 근무 규칙을 다시 검증한다.
        validateConfirmRequest(request, setting);

        Map<Long, User> usersById = getUsersById(request);
        Set<Long> allowedUnitIds = new HashSet<>(unitRepository.findAllSubUnitIds(request.unitId()));
        // 확정 저장 전 부대 소속, 제외 상태, 일정 충돌, 중복 근무, 시간대별 허용 역할을 강제 검증한다.
        validateConfirmAssignments(request, setting, usersById, allowedUnitIds);

        AiRecommendation aiRecommendation = resolveAiRecommendation(request, unit);
        List<DutyAssignment> assignments = request.assignments().stream()
                .map(assignmentRequest -> toApprovedAssignment(
                        request,
                        assignmentRequest,
                        aiRecommendation,
                        unit,
                        usersById.get(assignmentRequest.userId())
                ))
                .toList();
        List<DutyAssignment> savedAssignments = dutyAssignmentRepository.saveAll(assignments);

        return toAiRecommendationResponse(aiRecommendation, savedAssignments);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkScheduleDailyResponse getDailySchedule(Long unitId, LocalDate dutyDate, String dutyType) {
        if (!unitRepository.existsById(unitId)) {
            throw new IllegalArgumentException("Unit not found.");
        }

        List<Long> unitIds = unitRepository.findAllSubUnitIds(unitId);
        List<DutyAssignment> assignments = findLatestApprovedAssignments(unitIds, dutyDate, dutyType);
        List<WorkScheduleAssignmentResponse> assignmentResponses = assignments.stream()
                .map(this::toWorkScheduleAssignmentResponse)
                .toList();

        return new WorkScheduleDailyResponse(
                unitId,
                dutyDate,
                dutyType,
                assignmentResponses.size(),
                assignmentResponses
        );
    }

    private List<DutyAssignment> findLatestApprovedAssignments(
            List<Long> unitIds,
            LocalDate dutyDate,
            String dutyType
    ) {
        Long recommendationId = findLatestApprovedRecommendationId(unitIds, dutyDate, dutyType);
        if (recommendationId == null) {
            return List.of();
        }

        String query = """
                select d
                from DutyAssignment d
                join fetch d.user u
                join fetch d.unit unit
                where d.aiRecommendation.id = :recommendationId
                order by d.startTime asc, u.rankName asc, u.name asc
                """;

        return entityManager.createQuery(query, DutyAssignment.class)
                .setParameter("recommendationId", recommendationId)
                .getResultList();
    }

    private Long findLatestApprovedRecommendationId(
            List<Long> unitIds,
            LocalDate dutyDate,
            String dutyType
    ) {
        String query = """
                select ar.id
                from AiRecommendation ar
                where ar.unit.id in :unitIds
                  and ar.dutyDate = :dutyDate
                  and ar.status = :status
                """;

        if (!isBlank(dutyType)) {
            query += " and ar.dutyType = :dutyType";
        }

        query += " order by ar.id desc";

        var typedQuery = entityManager.createQuery(query, Long.class)
                .setParameter("unitIds", unitIds)
                .setParameter("dutyDate", dutyDate)
                .setParameter("status", DutyStatus.APPROVED)
                .setMaxResults(1);

        if (!isBlank(dutyType)) {
            typedQuery.setParameter("dutyType", dutyType);
        }

        return typedQuery.getResultStream()
                .findFirst()
                .orElse(null);
    }

    private WorkSchedulePreviewResponse toPreviewResponse(
            Long recommendationId,
            AiInternalRequest internalRequest,
            AiModelResponse aiResponse
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
                assignments
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
        List<PreviewSlot> slots = expandPreviewSlots(internalRequest);
        List<AiRecommendedUser> remainingUsers = new ArrayList<>(recommendedUsers);
        Map<SlotKey, Integer> privateRankCounts = new java.util.HashMap<>();

        List<WorkSchedulePreviewAssignmentResponse> assignments = new ArrayList<>();
        for (PreviewSlot slot : slots) {
            SlotKey slotKey = SlotKey.from(slot);
            AiRecommendedUser recommendedUser = findAndRemoveRecommendedUserByAllowedRoles(
                    remainingUsers,
                    soldiersByUserId,
                    slot.allowedRoles(),
                    privateRankCounts.getOrDefault(slotKey, 0) > 0
            );
            SoldierBlock soldier = soldiersByUserId.get(recommendedUser.userId());
            if (isPrivateRank(soldier.rankName())) {
                privateRankCounts.merge(slotKey, 1, Integer::sum);
            }
            assignments.add(toPreviewAssignment(
                        internalRequest,
                        slot,
                        recommendedUser,
                        soldier
            ));
        }
        return assignments;
    }

    private WorkSchedulePreviewAssignmentResponse toPreviewAssignment(
            AiInternalRequest internalRequest,
            PreviewSlot slot,
            AiRecommendedUser recommendedUser,
            SoldierBlock soldier
    ) {
        return new WorkSchedulePreviewAssignmentResponse(
                slot.slotOrder(),
                soldier.userId(),
                internalRequest.unit().unitId(),
                soldier.name(),
                soldier.rankName(),
                soldier.role(),
                internalRequest.duty().dutyDate(),
                internalRequest.duty().dutyType(),
                slot.startTime(),
                slot.endTime(),
                DutyStatus.PREVIEW,
                recommendedUser.reason()
        );
    }

    private AiRecommendedUser findAndRemoveRecommendedUserByAllowedRoles(
            List<AiRecommendedUser> recommendedUsers,
            Map<Long, SoldierBlock> soldiersByUserId,
            List<String> allowedRoles,
            boolean privateRankAlreadyAssigned
    ) {
        boolean blockedByPrivateRankRule = false;
        for (int index = 0; index < recommendedUsers.size(); index++) {
            AiRecommendedUser recommendedUser = recommendedUsers.get(index);
            SoldierBlock soldier = soldiersByUserId.get(recommendedUser.userId());
            if (soldier == null || !allowedRoles.contains(soldier.role())) {
                continue;
            }
            if (privateRankAlreadyAssigned && isPrivateRank(soldier.rankName())) {
                blockedByPrivateRankRule = true;
                continue;
            }
            if (soldier != null) {
                return recommendedUsers.remove(index);
            }
        }

        if (blockedByPrivateRankRule) {
            throw new IllegalArgumentException("AI 추천 결과가 같은 시간대에 이병을 2명 이상 배정했습니다.");
        }
        throw new IllegalArgumentException("AI 추천 결과가 시간대별 허용 역할과 일치하지 않습니다.");
    }

    private List<PreviewSlot> expandPreviewSlots(AiInternalRequest internalRequest) {
        return internalRequest.duty().timeSlots().stream()
                .sorted(Comparator.comparing(DutySlotBlock::slotOrder))
                .flatMap(slot -> java.util.stream.IntStream.range(0, slot.requiredCount())
                        .mapToObj(index -> new PreviewSlot(
                                slot.slotOrder(),
                                slot.startTime(),
                                slot.endTime(),
                                slot.allowedRoles()
                        )))
                .toList();
    }

    private void validateConfirmRequest(WorkScheduleConfirmRequest request, WorkScheduleSetting setting) {
        if (!setting.getDutyType().equals(request.dutyType())) {
            throw new IllegalArgumentException("요청한 근무 유형과 부대 근무 설정이 일치하지 않습니다.");
        }
        if (request.assignments().size() != setting.getRequiredCount()) {
            throw new IllegalArgumentException("승인 배정 인원 수가 필요 인원과 일치하지 않습니다.");
        }
        if (!setting.getRequiredCount().equals(request.requiredCount())) {
            throw new IllegalArgumentException("요청 필요 인원과 부대 근무 설정 인원이 일치하지 않습니다.");
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
            WorkScheduleSetting setting,
            Map<Long, User> usersById,
            Set<Long> allowedUnitIds
    ) {
        Set<String> excludedStatuses = setting.getExcludeStatusList().stream()
                .map(String::trim)
                .collect(Collectors.toSet());
        Set<Long> seenUserIds = new HashSet<>();
        Map<SlotKey, SlotRequirement> slotRequirements = getSlotRequirements(setting);
        Map<SlotKey, Integer> assignedCounts = new java.util.HashMap<>();
        Map<SlotKey, Integer> privateRankCounts = new java.util.HashMap<>();

        for (WorkScheduleConfirmAssignmentRequest assignment : request.assignments()) {
            User user = usersById.get(assignment.userId());
            validateConfirmAssignment(
                    request,
                    assignment,
                    setting,
                    usersById,
                    allowedUnitIds,
                    excludedStatuses,
                    seenUserIds
            );
            SlotKey key = SlotKey.from(assignment);
            SlotRequirement requirement = slotRequirements.get(key);
            if (requirement == null || !requirement.allowedRoles().contains(user.getRole())) {
                throw new IllegalArgumentException("승인 배정의 시간대별 허용 역할이 부대 근무 설정과 일치하지 않습니다.");
            }
            if (isPrivateRank(user.getRankName())) {
                int privateRankCount = privateRankCounts.merge(key, 1, Integer::sum);
                if (privateRankCount > 1) {
                    throw new IllegalArgumentException("같은 시간대에 이병끼리 배정할 수 없습니다.");
                }
            }
            assignedCounts.merge(key, 1, Integer::sum);
        }

        for (Map.Entry<SlotKey, SlotRequirement> entry : slotRequirements.entrySet()) {
            int assignedCount = assignedCounts.getOrDefault(entry.getKey(), 0);
            if (assignedCount != entry.getValue().requiredCount()) {
                throw new IllegalArgumentException("승인 배정의 시간대별 필요 인원이 부대 근무 설정과 일치하지 않습니다.");
            }
        }
    }

    private Map<SlotKey, SlotRequirement> getSlotRequirements(WorkScheduleSetting setting) {
        return setting.getTimeSlots().stream()
                .map(slot -> Map.entry(
                        new SlotKey(
                                slot.getSlotOrder(),
                                slot.getStartTime(),
                                slot.getEndTime()
                        ),
                        new SlotRequirement(
                                slot.getRequiredCount(),
                                slot.getAllowedRoles().stream()
                                        .map(role -> role.getRole())
                                        .collect(Collectors.toSet())
                        )
                ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void validateConfirmAssignment(
            WorkScheduleConfirmRequest request,
            WorkScheduleConfirmAssignmentRequest assignment,
            WorkScheduleSetting setting,
            Map<Long, User> usersById,
            Set<Long> allowedUnitIds,
            Set<String> excludedStatuses,
            Set<Long> seenUserIds
    ) {
        // AI 추천 또는 프론트 수정 결과가 실제 저장 가능한 병사인지 한 명씩 최종 검증한다.
        if (!seenUserIds.add(assignment.userId())) {
            throw new IllegalArgumentException("동일 병사가 중복 배정되었습니다.");
        }
        User user = usersById.get(assignment.userId());
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 병사가 포함되어 있습니다.");
        }
        if (!allowedUnitIds.contains(user.getUnit().getId())) {
            throw new IllegalArgumentException("요청 부대 소속이 아닌 병사가 포함되어 있습니다.");
        }
        if (excludedStatuses.contains(user.getCurrentStatus())) {
            throw new IllegalArgumentException("제외 상태의 병사가 포함되어 있습니다.");
        }
        if (hasScheduleConflict(request, assignment)) {
            throw new IllegalArgumentException("일정 충돌이 있는 병사가 포함되어 있습니다.");
        }
        if (hasExistingApprovedDuty(request, assignment)) {
            throw new IllegalArgumentException("이미 승인된 동일 근무가 있는 병사가 포함되어 있습니다.");
        }
        if (Boolean.TRUE.equals(setting.getPreventConsecutive()) && hasApprovedDutyYesterday(request, assignment)) {
            throw new IllegalArgumentException("전날 승인 근무자가 포함되어 있습니다.");
        }
    }

    private boolean hasScheduleConflict(
            WorkScheduleConfirmRequest request,
            WorkScheduleConfirmAssignmentRequest assignment
    ) {
        return scheduleRepository.existsByUserIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                assignment.userId(),
                resolveDutyEndAt(request, assignment),
                request.dutyDate().atTime(assignment.startTime())
        );
    }

    private boolean hasExistingApprovedDuty(
            WorkScheduleConfirmRequest request,
            WorkScheduleConfirmAssignmentRequest assignment
    ) {
        return dutyAssignmentRepository.existsByUserIdAndDutyDateAndStatus(
                assignment.userId(),
                request.dutyDate(),
                DutyStatus.APPROVED
        );
    }

    private boolean hasApprovedDutyYesterday(
            WorkScheduleConfirmRequest request,
            WorkScheduleConfirmAssignmentRequest assignment
    ) {
        return dutyAssignmentRepository.existsByUserIdAndDutyDateAndStatus(
                assignment.userId(),
                request.dutyDate().minusDays(1),
                DutyStatus.APPROVED
        );
    }

    private LocalDateTime resolveDutyEndAt(
            WorkScheduleConfirmRequest request,
            WorkScheduleConfirmAssignmentRequest assignment
    ) {
        LocalDate endDate = assignment.endTime().isAfter(assignment.startTime())
                ? request.dutyDate()
                : request.dutyDate().plusDays(1);
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

    private DutyAssignment toApprovedAssignment(
            WorkScheduleConfirmRequest confirmRequest,
            WorkScheduleConfirmAssignmentRequest request,
            AiRecommendation aiRecommendation,
            Unit unit,
            User user
    ) {
        DutyAssignment assignment = DutyAssignment.builder()
                .aiRecommendation(aiRecommendation)
                .user(user)
                .unit(unit)
                .dutyDate(confirmRequest.dutyDate())
                .dutyType(confirmRequest.dutyType())
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

    private WorkScheduleAssignmentResponse toWorkScheduleAssignmentResponse(DutyAssignment assignment) {
        return new WorkScheduleAssignmentResponse(
                assignment.getId(),
                assignment.getUser().getId(),
                assignment.getUnit().getId(),
                assignment.getUser().getName(),
                assignment.getUser().getRankName(),
                assignment.getUser().getRole(),
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

    private boolean isPrivateRank(String rankName) {
        return PRIVATE_RANK_NAME.equals(rankName);
    }

    private record SlotKey(
            Integer slotOrder,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime
    ) {

        static SlotKey from(WorkScheduleConfirmAssignmentRequest assignment) {
            return new SlotKey(
                    assignment.slotOrder(),
                    assignment.startTime(),
                    assignment.endTime()
            );
        }

        static SlotKey from(PreviewSlot slot) {
            return new SlotKey(
                    slot.slotOrder(),
                    slot.startTime(),
                    slot.endTime()
            );
        }
    }

    private record SlotRequirement(
            Integer requiredCount,
            Set<String> allowedRoles
    ) {
    }

    private record PreviewSlot(
            Integer slotOrder,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime,
            List<String> allowedRoles
    ) {
    }
}
