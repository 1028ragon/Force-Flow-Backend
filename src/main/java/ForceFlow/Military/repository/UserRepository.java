package ForceFlow.Military.repository;

import ForceFlow.Military.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByUnitId(Long unitId);

    List<User> findByUnitIdAndCurrentStatus(
            Long unitId,
            String currentStatus
    );

    boolean existsByServiceNumber(String serviceNumber);

    long countByUnitId(Long unitId);

    long countByUnitIdAndCurrentStatus(
            Long unitId,
            String currentStatus
    );

}