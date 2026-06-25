package ForceFlow.Military.workSchedule.service;

import ForceFlow.Military.entity.DutyAssignment;
import ForceFlow.Military.entity.User;
import ForceFlow.Military.repository.DutyAssignmentRepository;
import ForceFlow.Military.repository.ScheduleRepository;
import ForceFlow.Military.repository.UnitRepository;
import ForceFlow.Military.repository.UserRepository;
import ForceFlow.Military.workSchedule.constant.DutyStatus;
import ForceFlow.Military.workSchedule.dto.WorkScheduleCandidateResponse;
import ForceFlow.Military.workSchedule.dto.WorkScheduleCandidateSearchResponse;
import ForceFlow.Military.workSchedule.entity.WorkScheduleSetting;
import ForceFlow.Military.workSchedule.entity.WorkScheduleTimeSlot;
import ForceFlow.Military.workSchedule.repository.WorkScheduleSettingRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkScheduleCandidateServiceImpl implements WorkScheduleCandidateService {

    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final DutyAssignmentRepository dutyAssignmentRepository;
    private final WorkScheduleSettingRepository workScheduleSettingRepository;
    private final WorkScheduleDutyFatigueCalculator dutyFatigueCalculator;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public WorkScheduleCandidateSearchResponse searchCandidates(
            Long unitId,
            LocalDate dutyDate,
            String dutyType,
            Integer slotOrder,
            String keyword
    ) {
        WorkScheduleSetting setting = workScheduleSettingRepository.findByUnitIdAndDutyType(unitId, dutyType)
                .orElseThrow(() -> new IllegalArgumentException("부대 근무 설정을 찾을 수 없습니다."));
        WorkScheduleTimeSlot slot = resolveSlot(setting, slotOrder);
        List<Long> unitIds = unitRepository.findAllSubUnitIds(unitId);
        List<User> users = userRepository.findByUnitIdIn(unitIds);
        List<WorkScheduleCandidateResponse> candidates = buildCandidates(users, dutyDate, setting, slot, keyword);

        return new WorkScheduleCandidateSearchResponse(
                unitId,
                dutyDate,
                dutyType,
                slot != null ? slot.getSlotOrder() : null,
                slot != null ? slot.getStartTime() : setting.getStartTime(),
                slot != null ? slot.getEndTime() : setting.getEndTime(),
                slot != null ? slot.getRequiredCount() : setting.getRequiredCount(),
                resolveAllowedRoles(setting, slot),
                candidates
        );
    }

    private WorkScheduleTimeSlot resolveSlot(WorkScheduleSetting setting, Integer slotOrder) {
        if (slotOrder == null) {
            return null;
        }

        return setting.getTimeSlots().stream()
                .filter(slot -> slot.getSlotOrder().equals(slotOrder))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("시간대 설정을 찾을 수 없습니다."));
    }

    private List<WorkScheduleCandidateResponse> buildCandidates(
            List<User> users,
            LocalDate dutyDate,
            WorkScheduleSetting setting,
            WorkScheduleTimeSlot slot,
            String keyword
    ) {
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
        Set<String> allowedRoles = new HashSet<>(resolveAllowedRoles(setting, slot));
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
        Map<Long, Integer> recentDutyCounts = getRecentDutyCounts(userIds, lookbackStartDate, dutyDate);
        Map<Long, Integer> recentDutyFatigueScores = getRecentDutyFatigueScores(userIds, lookbackStartDate, dutyDate);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        return users.stream()
                .filter(user -> normalizedKeyword.isBlank()
                        || user.getName().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .filter(user -> allowedRoles.isEmpty() || allowedRoles.contains(user.getRole()))
                .map(user -> toCandidate(
                        user,
                        excludedStatuses,
                        conflictUserIds,
                        workedYesterdayUserIds,
                        recentDutyCounts,
                        recentDutyFatigueScores,
                        setting
                ))
                .sorted(Comparator
                        .comparing(WorkScheduleCandidateResponse::eligible).reversed()
                        .thenComparing(WorkScheduleCandidateResponse::recentDutyFatigueScore)
                        .thenComparing(WorkScheduleCandidateResponse::recentDutyCount)
                        .thenComparing(WorkScheduleCandidateResponse::rankName)
                        .thenComparing(WorkScheduleCandidateResponse::name))
                .toList();
    }

    private WorkScheduleCandidateResponse toCandidate(
            User user,
            Set<String> excludedStatuses,
            Set<Long> conflictUserIds,
            Set<Long> workedYesterdayUserIds,
            Map<Long, Integer> recentDutyCounts,
            Map<Long, Integer> recentDutyFatigueScores,
            WorkScheduleSetting setting
    ) {
        Integer recentDutyCount = recentDutyCounts.getOrDefault(user.getId(), 0);
        Integer recentDutyFatigueScore = recentDutyFatigueScores.getOrDefault(user.getId(), 0);
        Boolean workedYesterday = workedYesterdayUserIds.contains(user.getId());
        Boolean hasScheduleConflict = conflictUserIds.contains(user.getId());
        Boolean eligible = isEligible(
                user,
                recentDutyCount,
                workedYesterday,
                hasScheduleConflict,
                excludedStatuses,
                setting
        );

        return new WorkScheduleCandidateResponse(
                user.getId(),
                user.getUnit().getId(),
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

    private List<String> resolveAllowedRoles(WorkScheduleSetting setting, WorkScheduleTimeSlot slot) {
        if (slot != null) {
            return slot.getAllowedRoles().stream()
                    .map(role -> role.getRole())
                    .toList();
        }

        return setting.getTimeSlots().stream()
                .flatMap(timeSlot -> timeSlot.getAllowedRoles().stream())
                .map(role -> role.getRole())
                .distinct()
                .toList();
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
