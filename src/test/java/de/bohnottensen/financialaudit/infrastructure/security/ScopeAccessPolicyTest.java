package de.bohnottensen.financialaudit.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScopeAccessPolicyTest {

    private final ScopeAccessPolicy policy = new ScopeAccessPolicy();

    @Test
    void tenantAccessRequiresTextAndAuthenticatedUser() {
        assertThat(policy.canAccessTenant(authentication("ADMIN"), null)).isFalse();
        assertThat(policy.canAccessTenant(authentication("ADMIN"), " ")).isFalse();
        assertThat(policy.canAccessTenant(null, "tenant-1")).isFalse();
        assertThat(policy.canAccessTenant(unauthenticated(), "tenant-1")).isFalse();
    }

    @Test
    void onlyAdminAndLeadAuditorCanAccessTenant() {
        assertThat(policy.canAccessTenant(authentication("ADMIN"), "tenant-1")).isTrue();
        assertThat(policy.canAccessTenant(authentication("LEAD_AUDITOR"), "tenant-1")).isTrue();
        assertThat(policy.canAccessTenant(authentication("AUDITOR"), "tenant-1")).isFalse();
    }

    @Test
    void projectAccessRequiresTextAndAuthenticatedUser() {
        Authentication admin = authentication("ADMIN");

        assertThat(policy.canAccessProject(admin, null, "project-1")).isFalse();
        assertThat(policy.canAccessProject(admin, "tenant-1", " ")).isFalse();
        assertThat(policy.canAccessProject(null, "tenant-1", "project-1")).isFalse();
        assertThat(policy.canAccessProject(unauthenticated(), "tenant-1", "project-1")).isFalse();
    }

    @Test
    void adminLeadAuditorAndAuditorCanAccessProject() {
        assertThat(policy.canAccessProject(authentication("ADMIN"), "tenant-1", "project-1")).isTrue();
        assertThat(policy.canAccessProject(authentication("LEAD_AUDITOR"), "tenant-1", "project-1")).isTrue();
        assertThat(policy.canAccessProject(authentication("AUDITOR"), "tenant-1", "project-1")).isTrue();
        assertThat(policy.canAccessProject(authentication("USER"), "tenant-1", "project-1")).isFalse();
    }

    @Test
    void documentAccessRequiresTextAndAuthenticatedUser() {
        Authentication admin = authentication("ADMIN");

        assertThat(policy.canAccessDocument(admin, null, "project-1", "document-1")).isFalse();
        assertThat(policy.canAccessDocument(admin, "tenant-1", " ", "document-1")).isFalse();
        assertThat(policy.canAccessDocument(admin, "tenant-1", "project-1", "")).isFalse();
        assertThat(policy.canAccessDocument(null, "tenant-1", "project-1", "document-1")).isFalse();
        assertThat(policy.canAccessDocument(unauthenticated(), "tenant-1", "project-1", "document-1")).isFalse();
    }

    @Test
    void adminLeadAuditorAndAuditorCanAccessDocument() {
        assertThat(policy.canAccessDocument(authentication("ADMIN"), "tenant-1", "project-1", "document-1")).isTrue();
        assertThat(policy.canAccessDocument(authentication("LEAD_AUDITOR"), "tenant-1", "project-1", "document-1")).isTrue();
        assertThat(policy.canAccessDocument(authentication("AUDITOR"), "tenant-1", "project-1", "document-1")).isTrue();
        assertThat(policy.canAccessDocument(authentication("USER"), "tenant-1", "project-1", "document-1")).isFalse();
    }

    private Authentication authentication(String... roles) {
        return new UsernamePasswordAuthenticationToken(
                "user",
                "password",
                List.of(roles).stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList());
    }

    private Authentication unauthenticated() {
        return new UsernamePasswordAuthenticationToken("user", "password");
    }
}
