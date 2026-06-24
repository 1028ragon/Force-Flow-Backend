package ForceFlow.Military.repository;

import ForceFlow.Military.entity.DutyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select distinct d.user.id
            from DutyAssignment d
            where d.user.id in :userIds
              and d.dutyDate = :dutyDate
              and d.dutyType = :dutyType
            """)
    List<Long> findUserIdsWithSameDuty(
            @Param("userIds") List<Long> userIds,
            @Param("dutyDate") LocalDate dutyDate,
            @Param("dutyType") String dutyType
    );

    @Query("""
            select distinct d.user.id
            from DutyAssignment d
            where d.user.id in :userIds
              and d.dutyDate = :previousDutyDate
              and d.status = :status
            """)
    List<Long> findUserIdsWithConsecutiveDuty(
            @Param("userIds") List<Long> userIds,
            @Param("previousDutyDate") LocalDate previousDutyDate,
            @Param("status") String status
    );

    @Query("""
            select d.user.id
            from DutyAssignment d
            where d.user.id in :userIds
              and d.dutyDate between :startDate and :endDate
              and d.status = :status
            group by d.user.id
            having count(d.id) >= :maxCount
            """)
    List<Long> findUserIdsExceedingDutyLimit(
            @Param("userIds") List<Long> userIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") String status,
            @Param("maxCount") int maxCount
    );

}
