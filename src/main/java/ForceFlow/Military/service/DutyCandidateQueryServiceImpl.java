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
        // 대상 부대와 모든 하위 부대 소속 인원을 후보군으로 조회한다.
        List<Long> unitIds = unitRepository.findAllSubUnitIds(unitId);
        List<User> users = userRepository.findByUnitIdIn(unitIds);
        if (users.isEmpty()) {
            return List.of();
        }

        // N+1 쿼리를 방지하기 위해 필터 조건별 제외 대상 userId를 배치 조회한다.
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

        // DB에서 가져온 제외 대상 Set을 기준으로 메모리에서 최종 후보자를 걸러낸다.
        return users.stream()
                .filter(user -> !excludedStatuses.contains(user.getCurrentStatus()))
                .filter(user -> !scheduleConflictUserIds.contains(user.getId()))
                .filter(user -> !sameDutyUserIds.contains(user.getId()))
                .filter(user -> !consecutiveDutyUserIds.contains(user.getId()))
                .filter(user -> !dutyLimitExceededUserIds.contains(user.getId()))
                .map(this::toResponse)
                .toList();
    }

    // 종료 시간이 시작 시간보다 빠르면 다음날 종료되는 야간 근무로 처리한다.
    private LocalDateTime resolveDutyEndAt(LocalDate dutyDate, LocalTime startTime, LocalTime endTime) {
        LocalDate endDate = endTime.isAfter(startTime) ? dutyDate : dutyDate.plusDays(1);
        return LocalDateTime.of(endDate, endTime);
    }

    // 연속 근무 방지 옵션이 켜진 경우 전날 승인된 근무자가 있는지 배치 조회한다.
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

    // lookbackDays 또는 maxDutyCount 설정이 없으면 근무 횟수 제한 필터를 적용하지 않는다.
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

    // nullable List를 안전하게 Set으로 변환한다.
    private <T> Set<T> toSet(List<T> values) {
        if (values == null) {
            return Collections.emptySet();
        }

        return new HashSet<>(values);
    }

    // 서비스 외부에는 entity 대신 response DTO를 반환한다.
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
