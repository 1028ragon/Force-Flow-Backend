package ForceFlow.Military.repository;

import ForceFlow.Military.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByParentUnitId(Long parentUnitId);

    List<Unit> findByUnitType(String unitType);

    @Query(value = """
        WITH RECURSIVE unit_tree AS (
            SELECT unit_id
            FROM unit
            WHERE unit_id = :unitId

            UNION ALL

            SELECT u.unit_id
            FROM unit u
            INNER JOIN unit_tree ut
                ON u.parent_unit_id = ut.unit_id
        )
        SELECT unit_id
        FROM unit_tree
        """, nativeQuery = true)
    List<Long> findAllSubUnitIds(@Param("unitId") Long unitId);
}