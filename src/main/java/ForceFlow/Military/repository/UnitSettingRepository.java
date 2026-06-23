package ForceFlow.Military.repository;

import ForceFlow.Military.entity.UnitSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnitSettingRepository extends JpaRepository<UnitSetting, Long> {

    Optional<UnitSetting> findByUnitUnitId(Long unitId);

    boolean existsByUnitUnitId(Long unitId);

}