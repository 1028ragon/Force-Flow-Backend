package ForceFlow.Military.repository;

import ForceFlow.Military.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByUnitUnitId(Long unitId);

    List<User> findByUnitUnitIdAndCurrentStatus(
            Long unitId,
            String currentStatus
    );

    boolean existsByServiceNumber(String serviceNumber);

    long countByUnitUnitId(Long unitId);

    long countByUnitUnitIdAndCurrentStatus(
            Long unitId,
            String currentStatus
    );

}