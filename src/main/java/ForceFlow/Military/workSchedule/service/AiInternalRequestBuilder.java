package ForceFlow.Military.workSchedule.service;

import ForceFlow.Military.dto.requestDto.AiRecommendationCreateRequest;
import ForceFlow.Military.entity.DutyAssignment;
import ForceFlow.Military.entity.Unit;
import ForceFlow.Military.entity.User;
import ForceFlow.Military.repository.DutyAssignmentRepository;
import ForceFlow.Military.repository.ScheduleRepository;
import ForceFlow.Military.repository.UnitRepository;
import ForceFlow.Military.repository.UserRepository;
import ForceFlow.Military.workSchedule.constant.DutyStatus;
import ForceFlow.Military.workSchedule.dto.internal.AiInternalRequest;
import ForceFlow.Military.workSchedule.dto.internal.DutyBlock;
import ForceFlow.Military.workSchedule.dto.internal.DutySlotBlock;
import ForceFlow.Military.workSchedule.dto.internal.RuleBlock;
import ForceFlow.Military.workSchedule.dto.internal.SoldierBlock;
import ForceFlow.Military.workSchedule.dto.internal.UnitBlock;
import ForceFlow.Military.workSchedule.entity.WorkScheduleSetting;
import ForceFlow.Military.workSchedule.entity.WorkScheduleTimeSlot;
import ForceFlow.Military.workSchedule.repository.WorkScheduleSettingRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiInternalRequestBuilder {

    private final UnitRepository unitRepository;
    private final WorkScheduleSettingRepository workScheduleSettingRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final DutyAssignmentRepository dutyAssignmentRepository;
    private final WorkScheduleDutyFatigueCalculator dutyFatigueCalculator;
    private final EntityManager entityManager;

    public AiInternalRequest build(AiRecommendationCreateRequest request) {
        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new IllegalArgumentException("부대를 찾을 수 없습니다."));
        WorkScheduleSetting setting = workScheduleSettingRepository.findByUnitIdAndDutyType(
                        request.unitId(),
                        request.dutyType()
                )
                .orElseThrow(() -> new IllegalArgumentException("부대 근무 설정을 찾을 수 없습니다."));
        if (!setting.getDutyType().equals(request.dutyType())) {
            throw new IllegalArgumentException("요청한 근무 유형과 부대 근무 설정이 일치하지 않습니다.");
        }

        List<Long> unitIds = unitRepository.findAllSubUnitIds(request.unitId());
        List<User> users = userRepository.findByUnitIdIn(unitIds);
        List<SoldierBlock> soldiers = buildSoldiers(users, request.dutyDate(), setting);

        return new AiInternalRequest(
                toUnitBlock(unit),
                toDutyBlock(request, setting),
                toRuleBlock(setting),
                soldiers
        );
    }

    private List<SoldierBlock> buildSoldiers(List<User> users, LocalDate dutyDate, WorkScheduleSetting setting) {
        if (users.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();
        LocalDateTime dutyStartAt = LocalDateTime.of(dutyDate, setting.getStartTime());
        LocalDateTime dutyEndAt = resolveDutyEndAt(dutyDate, setting.getStartTime(), setting.getEndTime());
        LocalDate lookbackStartDate = dutyDate.minusDays(setting.getLookbackDays());
        LocalDate previousDutyDate = dutyDate.minusDays(1);

        Set<String> excludedStatuses = setting.getExcludeStatusList().stream()
                .map(String::trim)
                .collect(Collectors.toSet());
        Set<Long> conflictUserIds = toSet(scheduleRepository.findUserIdsWithConflict(
                userIds,
                dutyStartAt,
                dutyEndAt
        ));
        Set<Long> workedYesterdayUserIds = toSet(dutyAssignmentRepository.findUserIdsWithConsecutiveDuty(
                userIds,
                previousDutyDate,
                DutyStatus.APPROVED
        ));
        Map<Long, Integer> recentDutyCounts = getRecentDutyCounts(
                userIds,
                lookbackStartDate,
                dutyDate
        );
        Map<Long, Integer> recentDutyFatigueScores = getRecentDutyFatigueScores(
                userIds,
                lookbackStartDate,
                dutyDate
        );

        return users.stream()
                .map(user -> toSoldierBlock(
                        user,
                        excludedStatuses,
                        conflictUserIds,
                        workedYesterdayUserIds,
                        recentDutyCounts,
                        recentDutyFatigueScores,
                        setting
                ))
                .toList();
    }

    private SoldierBlock toSoldierBlock(
            User user,
            Set<String> excludedStatuses,
            Set<Long> conflictUserIds,
            Set<Long> workedYesterdayUserIds,
            Map<Long, Integer> recentDutyCounts,
            Map<Long, Integer> recentDutyFatigueScores,
            WorkScheduleSetting setting
    ) {
        Long userId = user.getId();
        Integer recentDutyCount = recentDutyCounts.getOrDefault(userId, 0);
        Integer recentDutyFatigueScore = recentDutyFatigueScores.getOrDefault(userId, 0);
        Boolean workedYesterday = workedYesterdayUserIds.contains(userId);
        Boolean hasScheduleConflict = conflictUserIds.contains(userId);
        Boolean eligible = isEligible(
                user,
                recentDutyCount,
                workedYesterday,
                hasScheduleConflict,
                excludedStatuses,
                setting
        );

        return new SoldierBlock(
                userId,
                user.getName(),
                user.getRankName(),
                user.getRole(),
                user.getCurrentStatus(),
                recentDutyCount,
                recentDutyFatigueScore,
                workedYesterday,
                hasScheduleConflict,
                eligible
        );
    }

    private boolean isEligible(
            User user,
            Integer recentDutyCount,
            Boolean workedYesterday,
            Boolean hasScheduleConflict,
            Set<String> excludedStatuses,
            WorkScheduleSetting setting
    ) {
        if (excludedStatuses.contains(user.getCurrentStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(hasScheduleConflict)) {
            return false;
        }
        if (Boolean.TRUE.equals(setting.getPreventConsecutive()) && Boolean.TRUE.equals(workedYesterday)) {
            return false;
        }
        return recentDutyCount < setting.getMaxDutyCount();
    }

    private Map<Long, Integer> getRecentDutyCounts(
            List<Long> userIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<Long, Integer> dutyCounts = new HashMap<>();
        dutyAssignmentRepository.countDutiesByUserIdsAndDateBetweenAndStatus(
                        userIds,
                        startDate,
                        endDate,
                        DutyStatus.APPROVED
                )
                .forEach(count -> dutyCounts.put(
                        count.getUserId(),
                        Math.toIntExact(count.getDutyCount())
                ));

        return dutyCounts;
    }

    private Map<Long, Integer> getRecentDutyFatigueScores(
            List<Long> userIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<Long, Integer> fatigueScores = new HashMap<>();
        List<DutyAssignment> assignments = entityManager.createQuery("""
                        select d
                        from DutyAssignment d
                        join fetch d.user u
                        where u.id in :userIds
                          and d.dutyDate between :startDate and :endDate
                          and d.status = :status
                        """, DutyAssignment.class)
                .setParameter("userIds", userIds)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("status", DutyStatus.APPROVED)
                .getResultList();

        for (DutyAssignment assignment : assignments) {
            fatigueScores.merge(
                    assignment.getUser().getId(),
                    dutyFatigueCalculator.calculate(assignment.getStartTime(), assignment.getEndTime()),
                    Integer::sum
            );
        }

        return fatigueScores;
    }

    private UnitBlock toUnitBlock(Unit unit) {
        Long parentUnitId = unit.getParentUnit() != null ? unit.getParentUnit().getId() : null;
        return new UnitBlock(
                unit.getId(),
                parentUnitId,
                unit.getUnitName(),
                unit.getUnitType()
        );
    }

    private DutyBlock toDutyBlock(AiRecommendationCreateRequest request, WorkScheduleSetting setting) {
        return new DutyBlock(
                request.dutyDate(),
                request.dutyType(),
                setting.getRequiredCount(),
                setting.getStartTime(),
                setting.getEndTime(),
                toDutySlots(setting)
        );
    }

    private List<DutySlotBlock> toDutySlots(WorkScheduleSetting setting) {
        return setting.getTimeSlots().stream()
                .sorted(Comparator.comparing(WorkScheduleTimeSlot::getSlotOrder))
                .map(slot -> new DutySlotBlock(
                        slot.getSlotOrder(),
                        slot.getStartTime(),
                        slot.getEndTime(),
                        slot.getRequiredCount(),
                        slot.getAllowedRoles().stream()
                                .map(role -> role.getRole())
                                .toList()
                ))
                .toList();
    }

    private RuleBlock toRuleBlock(WorkScheduleSetting setting) {
        return new RuleBlock(
                setting.getLookbackDays(),
                setting.getPreventConsecutive(),
                setting.getMaxDutyCount(),
                setting.getExcludeStatusList()
        );
    }

    private LocalDateTime resolveDutyEndAt(LocalDate dutyDate, LocalTime startTime, LocalTime endTime) {
        LocalDate endDate = endTime.isAfter(startTime) ? dutyDate : dutyDate.plusDays(1);
        return LocalDateTime.of(endDate, endTime);
    }

    private Set<Long> toSet(List<Long> values) {
        if (values == null) {
            return Collections.emptySet();
        }

        return new HashSet<>(values);
    }
}
