// === RiskSolutionRepository.java ===
package com.cloud1pm.backend.repository;

import com.cloud1pm.backend.entity.RiskSolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RiskSolutionRepository extends JpaRepository<RiskSolution, Long> {
    List<RiskSolution> findByUserIdAndRiskLevel(Long userId, Integer riskLevel);
    List<RiskSolution> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}