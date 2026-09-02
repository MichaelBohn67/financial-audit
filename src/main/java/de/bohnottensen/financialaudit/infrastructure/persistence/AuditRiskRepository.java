package de.bohnottensen.financialaudit.infrastructure.persistence;
import de.bohnottensen.financialaudit.domain.model.AuditRisk;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditRiskRepository extends JpaRepository<AuditRisk, Long> {
    List<AuditRisk> findByEngagementId(Long engagementId);
}
