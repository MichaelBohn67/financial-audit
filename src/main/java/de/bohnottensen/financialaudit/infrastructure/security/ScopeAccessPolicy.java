package de.bohnottensen.financialaudit.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("scopeAccessPolicy")
public class ScopeAccessPolicy {

    public boolean canAccessTenant(Authentication authentication, String tenantId) {
        if (!hasText(tenantId) || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (hasRole(authentication, "ADMIN")) {
            return true;
        }
        return hasRole(authentication, "WIRTSCHAFTSPRUEFER");
    }

    public boolean canAccessProject(Authentication authentication, String tenantId, String projectId) {
        if (!hasText(tenantId) || !hasText(projectId) || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (hasRole(authentication, "ADMIN")) {
            return true;
        }
        return hasRole(authentication, "WIRTSCHAFTSPRUEFER")
                || hasRole(authentication, "SENIOR_AUDITOR");
    }

    public boolean canAccessDocument(Authentication authentication, String tenantId, String projectId, String documentId) {
        if (!hasText(tenantId) || !hasText(projectId) || !hasText(documentId) || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (hasRole(authentication, "ADMIN")) {
            return true;
        }
        return hasRole(authentication, "WIRTSCHAFTSPRUEFER")
                || hasRole(authentication, "SENIOR_AUDITOR")
                || hasRole(authentication, "ASSISTANT");
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
