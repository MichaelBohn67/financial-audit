package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.Finding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FindingRepository extends JpaRepository<Finding, Long> {
    List<Finding> findByWorkpaperId(Long workpaperId);
    Optional<Finding> findFirstByBookingIdAndMaterialityConfigIdAndRuleName(
            Long bookingId, Long materialityConfigId, String ruleName);
    List<Finding> findByRemediationStatus(String status);
    List<Finding> findByRemediationOwner(String owner);
    long countByRemediationDueDateBeforeAndRemediationStatusNot(LocalDate date, String status);
    long countByRemediationStatusIn(List<String> statuses);
    long countByRiskLevel(String riskLevel);
    long countByStatus(String status);
    long countByRemediationStatus(String status);
}
