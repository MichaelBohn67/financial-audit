package de.bohnottensen.financialaudit.infrastructure.persistence;
import de.bohnottensen.financialaudit.domain.model.AuditEngagement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface AuditEngagementRepository extends JpaRepository<AuditEngagement, Long> {
    Optional<AuditEngagement> findByEngagementKey(String key);
    List<AuditEngagement> findByTenantIdAndProjectId(String tenantId, String projectId);
}
