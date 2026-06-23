package ForceFlow.Military.repository;

import ForceFlow.Military.entity.DutyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DutyAssignmentRepository extends JpaRepository<DutyAssignment, Long> {

    List<DutyAssignment> findByUserUserId(Long userId);

    List<DutyAssignment> findByUnitUnitId(Long unitId);

    List<DutyAssignment> findByRecommendationRecommendationId(
            Long recommendationId
    );

    List<DutyAssignment> findByUnitUnitIdAndStatus(
            Long unitId,
            String status
    );

    List<DutyAssignment> findByUnitUnitIdAndDutyDateBetweenAndStatus(
            Long unitId,
            LocalDate startDate,
            LocalDate endDate,
            String status
    );

    boolean existsByUserUserIdAndDutyDateAndDutyType(
            Long userId,
            LocalDate dutyDate,
            String dutyType
    );

    boolean existsByUserUserIdAndDutyDateAndStatus(
            Long userId,
            LocalDate dutyDate,
            String status
    );

    long countByUserUserIdAndDutyDateBetweenAndStatus(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            String status
    );

}