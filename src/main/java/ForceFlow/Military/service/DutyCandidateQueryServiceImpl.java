package ForceFlow.Military.service;

import ForceFlow.Military.dto.responseDto.UnitSettingResponse;
import ForceFlow.Military.dto.responseDto.UserResponse;
import ForceFlow.Military.entity.User;
import ForceFlow.Military.repository.DutyAssignmentRepository;
import ForceFlow.Military.repository.ScheduleRepository;
import ForceFlow.Military.repository.UnitRepository;
import ForceFlow.Military.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DutyCandidateQueryServiceImpl implements DutyCandidateQueryService {

    private static final String APPROVED_STATUS = "승인";

    private final UserRepository userRepository;
    private final UnitRepository unitRepository;
    private final ScheduleRepository scheduleRepository;
    private final DutyAssignmentRepository dutyAssignmentRepository;

    @Override
    public List<UserResponse> findAvailableUsers(
            Long unitId,
            LocalDate dutyDate,
            LocalTime startTime,
            LocalTime endTime,
            UnitSettingResponse setting
    ) {
        List<Long> unitIds = unitRepository.findAllSubUnitIds(unitId);
        List<User> users = userRepository.findByUnitIdIn(unitIds);
        if (users.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();
        Set<String> excludedStatuses = toSet(setting.excludeStatuses());
        LocalDateTime dutyStartAt = LocalDateTime.of(dutyDate, startTime);
        LocalDateTime dutyEndAt = resolveDutyEndAt(dutyDate, startTime, endTime);
        LocalDate previousDutyDate = dutyDate.minusDays(1);
        Set<Long> scheduleConflictUserIds = toSet(scheduleRepository.findUserIdsWithConflict(
                userIds,
                dutyStartAt,
                dutyEndAt
        ));
        Set<Long> sameDutyUserIds = toSet(dutyAssignmentRepository.findUserIdsWithSameDuty(
                userIds,
                dutyDate,
                setting.dutyType()
        ));
        Set<Long> consecutiveDutyUserIds = findConsecutiveDutyUserIds(
                userIds,
                previousDutyDate,
                setting.preventConsecutive()
        );
        Set<Long> dutyLimitExceededUserIds = findDutyLimitExceededUserIds(
                userIds,
                dutyDate,
                setting.lookbackDays(),
                setting.maxDutyCount()
        );

        return users.stream()
                .filter(user -> !excludedStatuses.contains(user.getCurrentStatus()))
                .filter(user -> !scheduleConflictUserIds.contains(user.getId()))
                .filter(user -> !sameDutyUserIds.contains(user.getId()))
                .filter(user -> !consecutiveDutyUserIds.contains(user.getId()))
                .filter(user -> !dutyLimitExceededUserIds.contains(user.getId()))
                .map(this::toResponse)
                .toList();
    }

    private LocalDateTime resolveDutyEndAt(LocalDate dutyDate, LocalTime startTime, LocalTime endTime) {
        LocalDate endDate = endTime.isAfter(startTime) ? dutyDate : dutyDate.plusDays(1);
        return LocalDateTime.of(endDate, endTime);
    }

    private Set<Long> findConsecutiveDutyUserIds(
            List<Long> userIds,
            LocalDate previousDutyDate,
            Boolean preventConsecutive
    ) {
        if (!Boolean.TRUE.equals(preventConsecutive)) {
            return Collections.emptySet();
        }

        return toSet(dutyAssignmentRepository.findUserIdsWithConsecutiveDuty(
                userIds,
                previousDutyDate,
                APPROVED_STATUS
        ));
    }

    private Set<Long> findDutyLimitExceededUserIds(
            List<Long> userIds,
            LocalDate dutyDate,
            Integer lookbackDays,
            Integer maxDutyCount
    ) {
        if (lookbackDays == null || maxDutyCount == null) {
            return Collections.emptySet();
        }

        LocalDate startDate = dutyDate.minusDays(lookbackDays);
        return toSet(dutyAssignmentRepository.findUserIdsExceedingDutyLimit(
                userIds,
                startDate,
                dutyDate,
                APPROVED_STATUS,
                maxDutyCount
        ));
    }

    private <T> Set<T> toSet(List<T> values) {
        if (values == null) {
            return Collections.emptySet();
        }

        return new HashSet<>(values);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUnit().getId(),
                user.getServiceNumber(),
                user.getName(),
                user.getRankName(),
                user.getRole(),
                user.getCurrentStatus(),
                user.getPhone(),
                user.getCreatedAt()
        );
    }
}
