package de.bohnottensen.financialaudit.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import de.bohnottensen.financialaudit.infrastructure.persistence.ProjectMembershipRepository;

@Component("scopeAccessPolicy")
public class ScopeAccessPolicy {

    private final ProjectMembershipRepository memberships;
    private final boolean scopeEnforcement;

    public ScopeAccessPolicy() {
        this.memberships = null;
        this.scopeEnforcement = false;
    }

    @Autowired
    public ScopeAccessPolicy(ProjectMembershipRepository memberships,
                             @Value("${financial-audit.security.scope-enforcement:true}") boolean scopeEnforcement) {
        this.memberships = memberships;
        this.scopeEnforcement = scopeEnforcement;
    }

    public boolean canAccessTenant(Authentication authentication, String tenantId) {
        if (!hasText(tenantId) || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (hasRole(authentication, "ADMIN")) {
            return true;
        }
        return hasRole(authentication, "LEAD_AUDITOR")
                && (scopeNotEnforced() || memberships.existsByUsernameAndProject_Tenant_TenantKey(
                authentication.getName(), tenantId));
    }

    public boolean canAccessProject(Authentication authentication, String tenantId, String projectId) {
        if (!hasText(tenantId) || !hasText(projectId) || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (hasRole(authentication, "ADMIN")) {
            return true;
        }
        return (hasRole(authentication, "LEAD_AUDITOR") || hasRole(authentication, "AUDITOR"))
                && (scopeNotEnforced() || memberships.existsByUsernameAndProject_Tenant_TenantKeyAndProject_ProjectKey(
                authentication.getName(), tenantId, projectId));
    }

    public boolean canAccessDocument(Authentication authentication, String tenantId, String projectId, String documentId) {
        if (!hasText(tenantId) || !hasText(projectId) || !hasText(documentId) || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (hasRole(authentication, "ADMIN")) {
            return true;
        }
        return (hasRole(authentication, "LEAD_AUDITOR") || hasRole(authentication, "AUDITOR"))
                && (scopeNotEnforced() || memberships.existsByUsernameAndProject_Tenant_TenantKeyAndProject_ProjectKey(
                authentication.getName(), tenantId, projectId));
    }

    private boolean scopeNotEnforced() {
        return !scopeEnforcement || memberships == null;
    }

    private boolean hasRole(Authentication authentication, String role) {
        String expectedAuthority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> expectedAuthority.equals(authority.getAuthority()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
