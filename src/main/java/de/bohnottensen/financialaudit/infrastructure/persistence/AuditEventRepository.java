package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findTop10ByOrderByMetadata_OccurredAtDesc();
    List<AuditEvent> findTop10ByTenantIdAndProjectIdOrderByMetadata_OccurredAtDesc(String tenantId, String projectId);
    Optional<AuditEvent> findTopByOrderByMetadata_OccurredAtDesc();
}
