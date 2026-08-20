package de.bohnottensen.financialaudit.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEntitiesTest {

    @Test
    void testAccountAndHolderAndAddress() {
        AccountHolder holder = new AccountHolder();
        holder.setId(1L);
        holder.setFirstName("John");
        holder.setLastName("Doe");
        holder.setCustomerNumber("CUST-1");
        holder.setBirthdate(LocalDate.of(1990, 1, 1));
        holder.setTin("TIN-1");
        holder.setCompany(true);
        holder.setNationality("US");
        holder.onCreate();

        assertThat(holder.getId()).isEqualTo(1L);
        assertThat(holder.getFirstName()).isEqualTo("John");
        assertThat(holder.getLastName()).isEqualTo("Doe");
        assertThat(holder.getCustomerNumber()).isEqualTo("CUST-1");
        assertThat(holder.getBirthdate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(holder.getTin()).isEqualTo("TIN-1");
        assertThat(holder.isCompany()).isTrue();
        assertThat(holder.getNationality()).isEqualTo("US");
        assertThat(holder.getCreatedAt()).isNotNull();

        Account account = new Account();
        account.setId(2L);
        account.setIban("DE123");
        account.setCurrency("EUR");
        account.setAccountHolder(holder);
        account.onCreate();

        assertThat(account.getId()).isEqualTo(2L);
        assertThat(account.getIban()).isEqualTo("DE123");
        assertThat(account.getCurrency()).isEqualTo("EUR");
        assertThat(account.getAccountHolder()).isEqualTo(holder);
        assertThat(account.getCreatedAt()).isNotNull();

        Address address = new Address();
        address.setId(3L);
        address.setAccountHolder(holder);
        address.setStreet("Main St 1");
        address.setPostalCode("12345");
        address.setCity("City");
        address.setCountry("DE");
        address.setValidFrom(LocalDate.of(2020, 1, 1));
        address.setValidTo(LocalDate.of(2030, 1, 1));

        assertThat(address.getId()).isEqualTo(3L);
        assertThat(address.getAccountHolder()).isEqualTo(holder);
        assertThat(address.getStreet()).isEqualTo("Main St 1");
        assertThat(address.getPostalCode()).isEqualTo("12345");
        assertThat(address.getCity()).isEqualTo("City");
        assertThat(address.getCountry()).isEqualTo("DE");
        assertThat(address.getValidFrom()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(address.getValidTo()).isEqualTo(LocalDate.of(2030, 1, 1));
    }

    @Test
    void testAuditEventAndMetadata() {
        AuditEvent event = new AuditEvent();
        event.setId(10L);
        event.setEntityType("BOOKING");
        event.setEntityId(20L);
        event.setEventType("UPDATE");
        event.setActor("auditor");
        event.setSummary("Updated booking amount");
        event.setPreviousValue("100.00");
        event.setCurrentValue("200.00");

        assertThat(event.getId()).isEqualTo(10L);
        assertThat(event.getEntityType()).isEqualTo("BOOKING");
        assertThat(event.getEntityId()).isEqualTo(20L);
        assertThat(event.getEventType()).isEqualTo("UPDATE");
        assertThat(event.getActor()).isEqualTo("auditor");
        assertThat(event.getSummary()).isEqualTo("Updated booking amount");
        assertThat(event.getPreviousValue()).isEqualTo("100.00");
        assertThat(event.getCurrentValue()).isEqualTo("200.00");

        event.onCreate();
        assertThat(event.getOccurredAt()).isNotNull();

        event.setMetadata(null);
        assertThat(event.getMetadata()).isNotNull();
    }

    @Test
    void testBenfordEntities() {
        BenfordAnalysisRun run = new BenfordAnalysisRun();
        run.setId(1L);
        run.setRunId("BEN-1");
        run.setRuleVersion("v1");
        run.setRunContext("ctx");
        run.setBookingCount(100);
        run.setSuspiciousDigitCount(2);
        run.onCreate();

        assertThat(run.getId()).isEqualTo(1L);
        assertThat(run.getRunId()).isEqualTo("BEN-1");
        assertThat(run.getRuleVersion()).isEqualTo("v1");
        assertThat(run.getRunContext()).isEqualTo("ctx");
        assertThat(run.getBookingCount()).isEqualTo(100);
        assertThat(run.getSuspiciousDigitCount()).isEqualTo(2);
        assertThat(run.getCreatedAt()).isNotNull();

        BenfordDigitStat stat = new BenfordDigitStat();
        stat.setBenfordRunId(1L);
        stat.setLeadingDigit(1);
        stat.setExpectedRatio(new BigDecimal("0.301"));
        stat.setObservedRatio(new BigDecimal("0.350"));
        stat.setAbsoluteDeviation(new BigDecimal("0.049"));
        stat.setSampleSize(100);
        stat.onCreate();

        assertThat(stat.getId()).isNull();
        assertThat(stat.getBenfordRunId()).isEqualTo(1L);
        assertThat(stat.getLeadingDigit()).isEqualTo(1);
        assertThat(stat.getExpectedRatio()).isEqualByComparingTo("0.301");
        assertThat(stat.getObservedRatio()).isEqualByComparingTo("0.350");
        assertThat(stat.getAbsoluteDeviation()).isEqualByComparingTo("0.049");
        assertThat(stat.getSampleSize()).isEqualTo(100);
        assertThat(stat.getCreatedAt()).isNotNull();
    }

    @Test
    void testImportJobAndProtocolEntry() {
        ImportJob job = new ImportJob();
        job.setId(1L);
        job.setSourceType("CSV");
        job.setStatus("COMPLETED");
        job.setRunContext("default");
        job.setTenantId("T1");
        job.setProjectId("P1");
        job.setDocumentId("D1");
        job.setRecordCount(100);
        job.setImportedCount(98);
        job.setInvalidCount(2);
        job.setChecksum("SHA-256");
        job.setErrorMessage("none");
        job.setStartedAt(LocalDateTime.now());
        job.setFinishedAt(LocalDateTime.now());

        assertThat(job.getId()).isEqualTo(1L);
        assertThat(job.getSourceType()).isEqualTo("CSV");
        assertThat(job.getStatus()).isEqualTo("COMPLETED");
        assertThat(job.getRunContext()).isEqualTo("default");
        assertThat(job.getTenantId()).isEqualTo("T1");
        assertThat(job.getProjectId()).isEqualTo("P1");
        assertThat(job.getDocumentId()).isEqualTo("D1");
        assertThat(job.getRecordCount()).isEqualTo(100);
        assertThat(job.getImportedCount()).isEqualTo(98);
        assertThat(job.getInvalidCount()).isEqualTo(2);
        assertThat(job.getChecksum()).isEqualTo("SHA-256");
        assertThat(job.getErrorMessage()).isEqualTo("none");
        assertThat(job.getStartedAt()).isNotNull();
        assertThat(job.getFinishedAt()).isNotNull();

        ImportJobProtocolEntry entry = new ImportJobProtocolEntry();
        entry.setImportJobId(1L);
        entry.setEntryIndex(10);
        entry.setLevel("ERROR");
        entry.setMessage("Invalid format");
        entry.onCreate();

        assertThat(entry.getId()).isNull();
        assertThat(entry.getImportJobId()).isEqualTo(1L);
        assertThat(entry.getEntryIndex()).isEqualTo(10);
        assertThat(entry.getLevel()).isEqualTo("ERROR");
        assertThat(entry.getMessage()).isEqualTo("Invalid format");
        assertThat(entry.getCreatedAt()).isNotNull();
    }

    @Test
    void testSamplingEntities() {
        SamplingRun run = new SamplingRun();
        run.setId(1L);
        run.setRunName("MUS-1");
        run.setSamplingStrategy("MUS");
        run.setSeed(42L);
        run.setPopulationSize(1000L);
        run.setSampleSize(50L);
        run.setParametersJson("{}");
        run.onCreate();

        assertThat(run.getId()).isEqualTo(1L);
        assertThat(run.getRunName()).isEqualTo("MUS-1");
        assertThat(run.getSamplingStrategy()).isEqualTo("MUS");
        assertThat(run.getSeed()).isEqualTo(42L);
        assertThat(run.getPopulationSize()).isEqualTo(1000L);
        assertThat(run.getSampleSize()).isEqualTo(50L);
        assertThat(run.getParametersJson()).isEqualTo("{}");
        assertThat(run.getCreatedAt()).isNotNull();

        SamplingRunItem item = new SamplingRunItem();
        item.setSamplingRunId(1L);
        item.setSampleUnitIndex(1);
        item.setSelectionPoint(new BigDecimal("100.50"));
        item.setBookingId(10L);
        item.setBookingAmount(new BigDecimal("500.00"));
        item.setCumulativeAmount(new BigDecimal("1500.00"));
        item.onCreate();

        assertThat(item.getId()).isNull();
        assertThat(item.getSamplingRunId()).isEqualTo(1L);
        assertThat(item.getSampleUnitIndex()).isEqualTo(1);
        assertThat(item.getSelectionPoint()).isEqualByComparingTo("100.50");
        assertThat(item.getBookingId()).isEqualTo(10L);
        assertThat(item.getBookingAmount()).isEqualByComparingTo("500.00");
        assertThat(item.getCumulativeAmount()).isEqualByComparingTo("1500.00");
        assertThat(item.getCreatedAt()).isNotNull();
    }

    @Test
    void testPatternEntities() {
        PatternAnalysisRun run = new PatternAnalysisRun();
        run.setId(1L);
        run.setRunId("PAT-1");
        run.setRuleVersion("v1");
        run.setRunContext("ctx");
        run.setBookingCount(50);
        run.setIssueCount(3);
        run.onCreate();

        assertThat(run.getId()).isEqualTo(1L);
        assertThat(run.getRunId()).isEqualTo("PAT-1");
        assertThat(run.getRuleVersion()).isEqualTo("v1");
        assertThat(run.getRunContext()).isEqualTo("ctx");
        assertThat(run.getBookingCount()).isEqualTo(50);
        assertThat(run.getIssueCount()).isEqualTo(3);
        assertThat(run.getCreatedAt()).isNotNull();

        PatternAnalysisIssue issue = new PatternAnalysisIssue();
        issue.setPatternRunId(1L);
        issue.setIssueType("DUPLICATE");
        issue.setSeverity("HIGH");
        issue.setReferenceKey("REF-1");
        issue.setDescription("Desc");
        issue.setPrimaryBookingId(10L);
        issue.setOccurrenceCount(2);
        issue.setDetailsJson("{}");
        issue.onCreate();

        assertThat(issue.getId()).isNull();
        assertThat(issue.getPatternRunId()).isEqualTo(1L);
        assertThat(issue.getIssueType()).isEqualTo("DUPLICATE");
        assertThat(issue.getSeverity()).isEqualTo("HIGH");
        assertThat(issue.getReferenceKey()).isEqualTo("REF-1");
        assertThat(issue.getDescription()).isEqualTo("Desc");
        assertThat(issue.getPrimaryBookingId()).isEqualTo(10L);
        assertThat(issue.getOccurrenceCount()).isEqualTo(2);
        assertThat(issue.getDetailsJson()).isEqualTo("{}");
        assertThat(issue.getCreatedAt()).isNotNull();
    }

    @Test
    void testSecurityRoleAndPermissionEntities() {
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_AUDITOR");
        role.setDescription("Auditor role");

        assertThat(role.getId()).isEqualTo(1L);
        assertThat(role.getName()).isEqualTo("ROLE_AUDITOR");
        assertThat(role.getDescription()).isEqualTo("Auditor role");

        Permission permission = new Permission();
        permission.setId(2L);
        permission.setName("READ_BOOKINGS");
        permission.setDescription("Read bookings");

        assertThat(permission.getId()).isEqualTo(2L);
        assertThat(permission.getName()).isEqualTo("READ_BOOKINGS");
        assertThat(permission.getDescription()).isEqualTo("Read bookings");

        RolePermission rp = new RolePermission();
        rp.setId(3L);
        rp.setRoleId(1L);
        rp.setPermissionId(2L);

        assertThat(rp.getId()).isEqualTo(3L);
        assertThat(rp.getRoleId()).isEqualTo(1L);
        assertThat(rp.getPermissionId()).isEqualTo(2L);

        UserRole ur = new UserRole();
        ur.setId(4L);
        ur.setUserId(10L);
        ur.setRoleId(1L);
        ur.onCreate();

        assertThat(ur.getId()).isEqualTo(4L);
        assertThat(ur.getUserId()).isEqualTo(10L);
        assertThat(ur.getRoleId()).isEqualTo(1L);
        assertThat(ur.getAssignedAt()).isNotNull();
    }

    @Test
    void testReportingEntities() {
        ReportTemplate template = new ReportTemplate();
        template.setId(1L);
        template.setName("Audit-Report");
        template.setVersion("1.0");
        template.setDescription("Sample description");
        template.setActive(true);
        template.onCreate();

        assertThat(template.getId()).isEqualTo(1L);
        assertThat(template.getName()).isEqualTo("Audit-Report");
        assertThat(template.getVersion()).isEqualTo("1.0");
        assertThat(template.getDescription()).isEqualTo("Sample description");
        assertThat(template.isActive()).isTrue();
        assertThat(template.getCreatedAt()).isNotNull();

        ReportRun run = new ReportRun();
        run.setId(2L);
        run.setReportName("Q1-Report");
        run.setTemplateVersion("1.0");
        run.setTemplateId(1L);
        run.setStatus("COMPLETED");
        run.setTriggeredBy("auditor");
        run.setParameters("{}");
        run.setOutputPath("q1.json");
        run.setErrorMessage("none");
        run.setCompletedAt(LocalDateTime.now());
        run.onCreate();

        assertThat(run.getId()).isEqualTo(2L);
        assertThat(run.getReportName()).isEqualTo("Q1-Report");
        assertThat(run.getTemplateVersion()).isEqualTo("1.0");
        assertThat(run.getTemplateId()).isEqualTo(1L);
        assertThat(run.getStatus()).isEqualTo("COMPLETED");
        assertThat(run.getTriggeredBy()).isEqualTo("auditor");
        assertThat(run.getParameters()).isEqualTo("{}");
        assertThat(run.getOutputPath()).isEqualTo("q1.json");
        assertThat(run.getErrorMessage()).isEqualTo("none");
        assertThat(run.getGeneratedAt()).isNotNull();
        assertThat(run.getCompletedAt()).isNotNull();
    }

    @Test
    void testWorkpaperAndReviewAction() {
        Workpaper wp = new Workpaper();
        wp.setId(1L);
        wp.setTitle("Testing WP");
        wp.setProjectId(100L);
        wp.setStatus("DRAFT");
        wp.setCreatedBy("auditor");
        wp.setAssignedTo("lead");
        wp.onCreate();
        wp.onUpdate();

        assertThat(wp.getId()).isEqualTo(1L);
        assertThat(wp.getTitle()).isEqualTo("Testing WP");
        assertThat(wp.getProjectId()).isEqualTo(100L);
        assertThat(wp.getStatus()).isEqualTo("DRAFT");
        assertThat(wp.getCreatedBy()).isEqualTo("auditor");
        assertThat(wp.getAssignedTo()).isEqualTo("lead");
        assertThat(wp.getCreatedAt()).isNotNull();
        assertThat(wp.getUpdatedAt()).isNotNull();

        ReviewAction ra = new ReviewAction();
        ra.setId(2L);
        ra.setWorkpaper(wp);
        ra.setActor("lead");
        ra.setAction(ReviewActionType.APPROVE);
        ra.setComment("Approved");
        ra.onCreate();

        assertThat(ra.getId()).isEqualTo(2L);
        assertThat(ra.getWorkpaper()).isEqualTo(wp);
        assertThat(ra.getActor()).isEqualTo("lead");
        assertThat(ra.getAction()).isEqualTo(ReviewActionType.APPROVE);
        assertThat(ra.getComment()).isEqualTo("Approved");
        assertThat(ra.getCreatedAt()).isNotNull();
    }

    @Test
    void testPrePersistAndPreUpdateLifecycleBranches() throws Exception {
        LocalDateTime fixedTime = LocalDateTime.of(2020, 1, 1, 12, 0);

        // Workpaper with null status
        Workpaper wpDefault = new Workpaper();
        wpDefault.onCreate();
        assertThat(wpDefault.getStatus()).isEqualTo("DRAFT");
        assertThat(wpDefault.getCreatedAt()).isNotNull();
        assertThat(wpDefault.getUpdatedAt()).isEqualTo(wpDefault.getCreatedAt());

        // Workpaper with explicit status
        Workpaper wpCustom = new Workpaper();
        wpCustom.setStatus("APPROVED");
        wpCustom.onCreate();
        assertThat(wpCustom.getStatus()).isEqualTo("APPROVED");
        wpCustom.onUpdate();
        assertThat(wpCustom.getUpdatedAt()).isNotNull();

        // Finding with all nulls in onCreate
        Finding fDefault = new Finding();
        fDefault.onCreate();
        assertThat(fDefault.getStatus()).isEqualTo("NEW");
        assertThat(fDefault.getAnalysisRunId()).isEqualTo("DEFAULT");
        assertThat(fDefault.getRuleVersion()).isEqualTo("v0");
        assertThat(fDefault.getRunContext()).isEqualTo("default");
        assertThat(fDefault.getCreatedAt()).isNotNull();

        // Finding with explicit fields in onCreate
        Finding fCustom = new Finding();
        fCustom.setStatus("EVALUATED");
        fCustom.setAnalysisRunId("CUSTOM_RUN");
        fCustom.setRuleVersion("v2");
        fCustom.setRunContext("custom_context");
        fCustom.onCreate();
        assertThat(fCustom.getStatus()).isEqualTo("EVALUATED");
        assertThat(fCustom.getAnalysisRunId()).isEqualTo("CUSTOM_RUN");
        assertThat(fCustom.getRuleVersion()).isEqualTo("v2");
        assertThat(fCustom.getRunContext()).isEqualTo("custom_context");

        // MaterialityConfig when createdAt is null vs preset
        MaterialityConfig mcNull = new MaterialityConfig();
        mcNull.onCreate();
        assertThat(mcNull.getCreatedAt()).isNotNull();

        MaterialityConfig mcPreset = new MaterialityConfig();
        java.lang.reflect.Field mcField = MaterialityConfig.class.getDeclaredField("createdAt");
        mcField.setAccessible(true);
        mcField.set(mcPreset, fixedTime);
        mcPreset.onCreate();
        assertThat(mcPreset.getCreatedAt()).isEqualTo(fixedTime);

        // Booking onCreate
        Booking b = new Booking();
        b.onCreate();
        assertThat(b.getCreatedAt()).isNotNull();
    }
}
