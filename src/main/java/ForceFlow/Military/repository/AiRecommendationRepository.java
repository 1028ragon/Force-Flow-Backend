package ForceFlow.Military.repository;

import ForceFlow.Military.entity.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, Long> {

    List<AiRecommendation> findByUnitId(Long unitId);

    List<AiRecommendation> findByUnitIdAndStatus(
            Long unitId,
            String status
    );

}