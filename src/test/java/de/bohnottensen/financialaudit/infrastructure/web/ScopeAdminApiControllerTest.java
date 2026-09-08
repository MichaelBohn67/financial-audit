package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.domain.model.AuditProject;
import de.bohnottensen.financialaudit.domain.model.ProjectMembership;
import de.bohnottensen.financialaudit.domain.model.Tenant;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditProjectRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ProjectMembershipRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ScopeAdminApiControllerTest {

    private TenantRepository tenants;
    private AuditProjectRepository projects;
    private ProjectMembershipRepository memberships;
    private ScopeAdminApiController controller;

    @BeforeEach
    void setUp() {
        tenants = mock(TenantRepository.class);
        projects = mock(AuditProjectRepository.class);
        memberships = mock(ProjectMembershipRepository.class);
        controller = new ScopeAdminApiController(tenants, projects, memberships);
    }

    @Test
    void shouldCreateTenant() {
        ScopeAdminApiController.TenantRequest request = new ScopeAdminApiController.TenantRequest("TENANT-1", "Tenant One");
        when(tenants.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tenant result = controller.createTenant(request);

        assertThat(result.getTenantKey()).isEqualTo("TENANT-1");
        assertThat(result.getName()).isEqualTo("Tenant One");
        verify(tenants).save(any(Tenant.class));
    }

    @Test
    void shouldCreateProjectWhenTenantExists() {
        Tenant tenant = new Tenant();
        tenant.setTenantKey("TENANT-1");
        when(tenants.findByTenantKey("TENANT-1")).thenReturn(Optional.of(tenant));
        when(projects.save(any(AuditProject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScopeAdminApiController.ProjectRequest request = new ScopeAdminApiController.ProjectRequest("TENANT-1", "PRJ-1", "Project One");

        AuditProject result = controller.createProject(request);

        assertThat(result.getTenant()).isEqualTo(tenant);
        assertThat(result.getProjectKey()).isEqualTo("PRJ-1");
        assertThat(result.getName()).isEqualTo("Project One");
        verify(projects).save(any(AuditProject.class));
    }

    @Test
    void shouldThrowWhenCreatingProjectAndTenantDoesNotExist() {
        when(tenants.findByTenantKey("NON-EXISTENT")).thenReturn(Optional.empty());

        ScopeAdminApiController.ProjectRequest request = new ScopeAdminApiController.ProjectRequest("NON-EXISTENT", "PRJ-1", "Project One");

        assertThatThrownBy(() -> controller.createProject(request))
                .isInstanceOf(NoSuchElementException.class);
        verify(projects, never()).save(any(AuditProject.class));
    }

    @Test
    void shouldGrantMembershipWhenProjectExists() {
        AuditProject project = new AuditProject();
        project.setProjectKey("PRJ-1");
        when(projects.findByTenant_TenantKeyAndProjectKey("TENANT-1", "PRJ-1")).thenReturn(Optional.of(project));
        when(memberships.save(any(ProjectMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScopeAdminApiController.MembershipRequest request = new ScopeAdminApiController.MembershipRequest("auditor_bob", "TENANT-1", "PRJ-1");

        ProjectMembership result = controller.grantMembership(request);

        assertThat(result.getUsername()).isEqualTo("auditor_bob");
        assertThat(result.getProject()).isEqualTo(project);
        verify(memberships).save(any(ProjectMembership.class));
    }

    @Test
    void shouldThrowWhenGrantingMembershipAndProjectDoesNotExist() {
        when(projects.findByTenant_TenantKeyAndProjectKey("TENANT-1", "NON-EXISTENT")).thenReturn(Optional.empty());

        ScopeAdminApiController.MembershipRequest request = new ScopeAdminApiController.MembershipRequest("auditor_bob", "TENANT-1", "NON-EXISTENT");

        assertThatThrownBy(() -> controller.grantMembership(request))
                .isInstanceOf(NoSuchElementException.class);
        verify(memberships, never()).save(any(ProjectMembership.class));
    }

    @Test
    void shouldTestRequestRecords() {
        ScopeAdminApiController.TenantRequest tr = new ScopeAdminApiController.TenantRequest("TK", "Name");
        assertThat(tr.tenantKey()).isEqualTo("TK");
        assertThat(tr.name()).isEqualTo("Name");

        ScopeAdminApiController.ProjectRequest pr = new ScopeAdminApiController.ProjectRequest("TK", "PK", "Name");
        assertThat(pr.tenantKey()).isEqualTo("TK");
        assertThat(pr.projectKey()).isEqualTo("PK");
        assertThat(pr.name()).isEqualTo("Name");

        ScopeAdminApiController.MembershipRequest mr = new ScopeAdminApiController.MembershipRequest("User", "TK", "PK");
        assertThat(mr.username()).isEqualTo("User");
        assertThat(mr.tenantKey()).isEqualTo("TK");
        assertThat(mr.projectKey()).isEqualTo("PK");
    }
}
