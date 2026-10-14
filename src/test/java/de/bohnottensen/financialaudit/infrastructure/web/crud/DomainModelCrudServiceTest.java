package de.bohnottensen.financialaudit.infrastructure.web.crud;

import de.bohnottensen.financialaudit.domain.model.Tenant;
import de.bohnottensen.financialaudit.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DomainModelCrudServiceTest {

    @Autowired
    private DomainModelCrudService crudService;

    @Test
    void shouldDiscoverAll33DomainModelDescriptors() {
        List<DomainModelDescriptor> allDescriptors = crudService.getAllDescriptors();
        assertThat(allDescriptors).hasSize(33);

        // Verify specific known models exist
        assertThat(crudService.findDescriptor("User")).isPresent();
        assertThat(crudService.findDescriptor("Industriekontenrahmen")).isPresent();
        assertThat(crudService.findDescriptor("Role")).isPresent();
        assertThat(crudService.findDescriptor("Account")).isPresent();
        assertThat(crudService.findDescriptor("AccountHolder")).isPresent();
        assertThat(crudService.findDescriptor("Address")).isPresent();
        assertThat(crudService.findDescriptor("AuditEngagement")).isPresent();
        assertThat(crudService.findDescriptor("AuditEvent")).isPresent();
        assertThat(crudService.findDescriptor("AuditProcedure")).isPresent();
        assertThat(crudService.findDescriptor("AuditProject")).isPresent();
        assertThat(crudService.findDescriptor("AuditRisk")).isPresent();
        assertThat(crudService.findDescriptor("BenfordAnalysisRun")).isPresent();
        assertThat(crudService.findDescriptor("BenfordDigitStat")).isPresent();
        assertThat(crudService.findDescriptor("Booking")).isPresent();
        assertThat(crudService.findDescriptor("Finding")).isPresent();
        assertThat(crudService.findDescriptor("ImportJob")).isPresent();
        assertThat(crudService.findDescriptor("ImportJobProtocolEntry")).isPresent();
        assertThat(crudService.findDescriptor("MaterialityConfig")).isPresent();
        assertThat(crudService.findDescriptor("PatternAnalysisIssue")).isPresent();
        assertThat(crudService.findDescriptor("PatternAnalysisRun")).isPresent();
        assertThat(crudService.findDescriptor("Permission")).isPresent();
        assertThat(crudService.findDescriptor("ProjectMembership")).isPresent();
        assertThat(crudService.findDescriptor("ReportArchive")).isPresent();
        assertThat(crudService.findDescriptor("ReportRun")).isPresent();
        assertThat(crudService.findDescriptor("ReportTemplate")).isPresent();
        assertThat(crudService.findDescriptor("ReviewAction")).isPresent();
        assertThat(crudService.findDescriptor("RolePermission")).isPresent();
        assertThat(crudService.findDescriptor("SamplingRun")).isPresent();
        assertThat(crudService.findDescriptor("SamplingRunItem")).isPresent();
        assertThat(crudService.findDescriptor("Tenant")).isPresent();
        assertThat(crudService.findDescriptor("UserRole")).isPresent();
        assertThat(crudService.findDescriptor("Workpaper")).isPresent();
        assertThat(crudService.findDescriptor("WorkpaperEvidence")).isPresent();
    }

    @Test
    void shouldFindDescriptorByVariousSlugFormats() {
        assertThat(crudService.findDescriptor("account-holder")).isPresent();
        assertThat(crudService.findDescriptor("account-holders")).isPresent();
        assertThat(crudService.findDescriptor("AccountHolder")).isPresent();
        assertThat(crudService.findDescriptor("accountholder")).isPresent();
        assertThat(crudService.findDescriptor("account_holder")).isPresent();
        assertThat(crudService.findDescriptor("users")).isPresent();
        assertThat(crudService.findDescriptor("user")).isPresent();
        assertThat(crudService.findDescriptor("unknown-entity")).isEmpty();
        assertThat(crudService.findDescriptor("")).isEmpty();
        assertThat(crudService.findDescriptor(null)).isEmpty();
    }

    @Test
    void shouldPerformCrudOperationsOnTenant() {
        DomainModelDescriptor tenantDesc = crudService.findDescriptor("tenant").orElseThrow();

        Map<String, String[]> createParams = new HashMap<>();
        createParams.put("tenantKey", new String[]{"TENANT-TEST-1"});
        createParams.put("name", new String[]{"Test Corp"});

        Object created = crudService.create(tenantDesc, createParams);
        assertThat(created).isInstanceOf(Tenant.class);

        Tenant tenant = (Tenant) created;
        assertThat(tenant.getId()).isNotNull();
        assertThat(tenant.getTenantKey()).isEqualTo("TENANT-TEST-1");
        assertThat(tenant.getName()).isEqualTo("Test Corp");

        Object found = crudService.findById(tenantDesc, tenant.getId()).orElse(null);
        assertThat(found).isNotNull();

        Map<String, String[]> updateParams = new HashMap<>();
        updateParams.put("tenantKey", new String[]{"TENANT-TEST-1-UPDATED"});
        updateParams.put("name", new String[]{"Updated Test Corp"});

        Object updated = crudService.update(tenantDesc, tenant.getId(), updateParams);
        assertThat(((Tenant) updated).getName()).isEqualTo("Updated Test Corp");
        assertThat(((Tenant) updated).getTenantKey()).isEqualTo("TENANT-TEST-1-UPDATED");

        boolean deleted = crudService.delete(tenantDesc, tenant.getId());
        assertThat(deleted).isTrue();

        Optional<Object> deletedFound = crudService.findById(tenantDesc, tenant.getId());
        assertThat(deletedFound).isEmpty();
    }

    @Test
    void shouldConvertPropertyValuesAccurately() {
        DomainModelDescriptor userDesc = crudService.findDescriptor("user").orElseThrow();
        DomainPropertyDescriptor usernameProp = userDesc.getProperty("username");
        DomainPropertyDescriptor enabledProp = userDesc.getProperty("enabled");
        DomainPropertyDescriptor idProp = userDesc.getIdProperty();

        assertThat(crudService.convertValue("testuser", usernameProp)).isEqualTo("testuser");
        assertThat(crudService.convertValue("true", enabledProp)).isEqualTo(true);
        assertThat(crudService.convertValue("false", enabledProp)).isEqualTo(false);
        assertThat(crudService.convertValue("123", idProp)).isEqualTo(123L);
        assertThat(crudService.convertValue("", usernameProp)).isNull();
        assertThat(crudService.convertValue("", enabledProp)).isEqualTo(false);
    }
}
