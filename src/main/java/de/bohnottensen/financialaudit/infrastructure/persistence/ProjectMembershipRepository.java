package de.bohnottensen.financialaudit.infrastructure.persistence;
import de.bohnottensen.financialaudit.domain.model.ProjectMembership;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, Long> {
    boolean existsByUsernameAndProject_Tenant_TenantKey(String username, String tenantKey);
    boolean existsByUsernameAndProject_Tenant_TenantKeyAndProject_ProjectKey(String username, String tenantKey, String projectKey);
}
