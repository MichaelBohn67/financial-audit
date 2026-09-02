package de.bohnottensen.financialaudit.infrastructure.persistence;
import de.bohnottensen.financialaudit.domain.model.AuditProject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AuditProjectRepository extends JpaRepository<AuditProject, Long> {
    Optional<AuditProject> findByTenant_TenantKeyAndProjectKey(String tenantKey, String projectKey);
}
