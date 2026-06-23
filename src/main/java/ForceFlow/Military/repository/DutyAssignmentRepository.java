package ForceFlow.Military.repository;

import ForceFlow.Military.entity.DutyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DutyAssignmentRepository extends JpaRepository<DutyAssignment, Long> {

    List<DutyAssignment> findByUserId(Long userId);

    List<DutyAssignment> findByUnitId(Long unitId);

    List<DutyAssignment> findByAiRecommendationId(
            Long recommendationId
    );

    List<DutyAssignment> findByUnitIdAndStatus(
            Long unitId,
            String status
    );

    List<DutyAssignment> findByUnitIdAndDutyDateBetweenAndStatus(
            Long unitId,
            LocalDate startDate,
            LocalDate endDate,
            String status
    );

    boolean existsByUserIdAndDutyDateAndDutyType(
            Long userId,
            LocalDate dutyDate,
            String dutyType
    );

    boolean existsByUserIdAndDutyDateAndStatus(
            Long userId,
            LocalDate dutyDate,
            String status
    );

    long countByUserIdAndDutyDateBetweenAndStatus(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            String status
    );

}