package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.domain.model.AuditProject;
import de.bohnottensen.financialaudit.domain.model.ProjectMembership;
import de.bohnottensen.financialaudit.domain.model.Tenant;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditProjectRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ProjectMembershipRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.TenantRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/scope")
@PreAuthorize("hasRole('ADMIN')")
public class ScopeAdminApiController {
    private final TenantRepository tenants;
    private final AuditProjectRepository projects;
    private final ProjectMembershipRepository memberships;

    public ScopeAdminApiController(TenantRepository tenants, AuditProjectRepository projects,
                                   ProjectMembershipRepository memberships) {
        this.tenants = tenants;
        this.projects = projects;
        this.memberships = memberships;
    }

    @PostMapping("/tenants")
    public Tenant createTenant(@Valid @RequestBody TenantRequest request) {
        Tenant tenant = new Tenant();
        tenant.setTenantKey(request.tenantKey());
        tenant.setName(request.name());
        return tenants.save(tenant);
    }

    @PostMapping("/projects")
    public AuditProject createProject(@Valid @RequestBody ProjectRequest request) {
        Tenant tenant = tenants.findByTenantKey(request.tenantKey()).orElseThrow();
        AuditProject project = new AuditProject();
        project.setTenant(tenant);
        project.setProjectKey(request.projectKey());
        project.setName(request.name());
        return projects.save(project);
    }

    @PostMapping("/memberships")
    public ProjectMembership grantMembership(@Valid @RequestBody MembershipRequest request) {
        AuditProject project = projects.findByTenant_TenantKeyAndProjectKey(
                request.tenantKey(), request.projectKey()).orElseThrow();
        ProjectMembership membership = new ProjectMembership();
        membership.setUsername(request.username());
        membership.setProject(project);
        return memberships.save(membership);
    }

    public record TenantRequest(@NotBlank String tenantKey, @NotBlank String name) {}
    public record ProjectRequest(@NotBlank String tenantKey, @NotBlank String projectKey, @NotBlank String name) {}
    public record MembershipRequest(@NotBlank String username, @NotBlank String tenantKey, @NotBlank String projectKey) {}
}
