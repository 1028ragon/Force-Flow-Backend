package ForceFlow.Military.repository;

import ForceFlow.Military.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByParentUnitId(Long parentUnitId);

    List<Unit> findByUnitType(String unitType);

}