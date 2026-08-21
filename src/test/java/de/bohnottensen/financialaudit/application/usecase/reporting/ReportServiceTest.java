package de.bohnottensen.financialaudit.application.usecase.reporting;

import de.bohnottensen.financialaudit.domain.model.ReportRun;
import de.bohnottensen.financialaudit.domain.model.ReportRunStatus;
import de.bohnottensen.financialaudit.domain.model.ReportTemplate;
import de.bohnottensen.financialaudit.infrastructure.persistence.ReportRunRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ReportTemplateRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private final ReportRunRepository runRepo = mock(ReportRunRepository.class);
    private final ReportTemplateRepository templateRepo = mock(ReportTemplateRepository.class);
    private final ReportService service = new ReportService(runRepo, templateRepo);

    // --- registerTemplate ---

    @Test
    void shouldRegisterNewTemplate() {
        when(templateRepo.findByNameAndVersion("AML-Report", "1.0.0")).thenReturn(Optional.empty());
        when(templateRepo.findFirstByNameAndActiveTrueOrderByCreatedAtDesc("AML-Report")).thenReturn(Optional.empty());
        when(templateRepo.save(any(ReportTemplate.class))).thenAnswer(inv -> {
            ReportTemplate t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        ReportTemplate result = service.registerTemplate("AML-Report", "1.0.0", "AML findings report");

        assertThat(result.getName()).isEqualTo("AML-Report");
        assertThat(result.getVersion()).isEqualTo("1.0.0");
        assertThat(result.getDescription()).isEqualTo("AML findings report");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void shouldDeactivatePreviousVersionWhenRegisteringNewOne() {
        ReportTemplate previous = new ReportTemplate();
        previous.setId(1L);
        previous.setName("AML-Report");
        previous.setVersion("1.0.0");
        previous.setActive(true);

        when(templateRepo.findByNameAndVersion("AML-Report", "2.0.0")).thenReturn(Optional.empty());
        when(templateRepo.findFirstByNameAndActiveTrueOrderByCreatedAtDesc("AML-Report"))
                .thenReturn(Optional.of(previous));
        when(templateRepo.save(any(ReportTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registerTemplate("AML-Report", "2.0.0", "Updated AML report");

        assertThat(previous.isActive()).isFalse();
    }

    @Test
    void shouldRejectDuplicateTemplateVersion() {
        ReportTemplate existing = new ReportTemplate();
        existing.setName("AML-Report");
        existing.setVersion("1.0.0");

        when(templateRepo.findByNameAndVersion("AML-Report", "1.0.0")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.registerTemplate("AML-Report", "1.0.0", "duplicate"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template already exists");
    }

    @Test
    void shouldThrowWhenNoActiveTemplateFound() {
        when(templateRepo.findFirstByNameAndActiveTrueOrderByCreatedAtDesc("Missing-Report"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findActiveTemplate("Missing-Report"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No active template found");
    }

    // --- startRun ---

    @Test
    void shouldStartRunForActiveTemplate() {
        ReportTemplate template = activeTemplate(5L, "AML-Report", "1.0.0");
        when(templateRepo.findFirstByNameAndActiveTrueOrderByCreatedAtDesc("AML-Report"))
                .thenReturn(Optional.of(template));
        when(runRepo.save(any(ReportRun.class))).thenAnswer(inv -> {
            ReportRun r = inv.getArgument(0);
            r.setId(42L);
            return r;
        });

        ReportRun run = service.startRun("AML-Report", "wirtschaftspruefer", "tenantId=T1");

        assertThat(run.getReportName()).isEqualTo("AML-Report");
        assertThat(run.getTemplateVersion()).isEqualTo("1.0.0");
        assertThat(run.getTemplateId()).isEqualTo(5L);
        assertThat(run.getTriggeredBy()).isEqualTo("wirtschaftspruefer");
        assertThat(run.getParameters()).isEqualTo("tenantId=T1");
        assertThat(run.getStatus()).isEqualTo(ReportRunStatus.RUNNING.name());
    }

    // --- completeRun ---

    @Test
    void shouldMarkRunAsCompleted() {
        ReportRun run = runningRun(10L);
        when(runRepo.findById(10L)).thenReturn(Optional.of(run));
        when(runRepo.save(any(ReportRun.class))).thenAnswer(inv -> inv.getArgument(0));

        ReportRun result = service.completeRun(10L, "/reports/aml-2026.pdf");

        assertThat(result.getStatus()).isEqualTo(ReportRunStatus.COMPLETED.name());
        assertThat(result.getOutputPath()).isEqualTo("/reports/aml-2026.pdf");
        assertThat(result.getCompletedAt()).isNotNull();
    }

    // --- failRun ---

    @Test
    void shouldMarkRunAsFailed() {
        ReportRun run = runningRun(11L);
        when(runRepo.findById(11L)).thenReturn(Optional.of(run));
        when(runRepo.save(any(ReportRun.class))).thenAnswer(inv -> inv.getArgument(0));

        ReportRun result = service.failRun(11L, "Database timeout");

        assertThat(result.getStatus()).isEqualTo(ReportRunStatus.FAILED.name());
        assertThat(result.getErrorMessage()).isEqualTo("Database timeout");
        assertThat(result.getCompletedAt()).isNotNull();
    }

    // --- findRunsByStatus ---

    @Test
    void shouldReturnRunsByStatus() {
        ReportRun completed = new ReportRun();
        completed.setStatus(ReportRunStatus.COMPLETED.name());
        when(runRepo.findByStatusOrderByGeneratedAtDesc(ReportRunStatus.COMPLETED.name()))
                .thenReturn(List.of(completed));

        List<ReportRun> results = service.findRunsByStatus(ReportRunStatus.COMPLETED);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo("COMPLETED");
    }

    // --- findTemplateVersions ---

    @Test
    void shouldReturnAllVersionsForTemplate() {
        when(templateRepo.findByNameOrderByCreatedAtDesc("AML-Report"))
                .thenReturn(List.of(
                        activeTemplate(2L, "AML-Report", "2.0.0"),
                        activeTemplate(1L, "AML-Report", "1.0.0")));

        List<ReportTemplate> versions = service.findTemplateVersions("AML-Report");

        assertThat(versions).hasSize(2);
        assertThat(versions.get(0).getVersion()).isEqualTo("2.0.0");
    }

    @Test
    void shouldFindRunByIdAndFindRunsByTemplate() {
        ReportRun run = runningRun(12L);
        when(runRepo.findById(12L)).thenReturn(Optional.of(run));
        when(runRepo.findByReportNameAndTemplateVersionOrderByGeneratedAtDesc("AML-Report", "1.0.0"))
                .thenReturn(List.of(run));

        assertThat(service.findRunById(12L)).isSameAs(run);
        assertThat(service.findRunsByTemplate("AML-Report", "1.0.0"))
                .containsExactly(run);
    }

    @Test
    void shouldGenerateLegacyRunWithAllFields() {
        when(runRepo.save(any(ReportRun.class))).thenAnswer(inv -> inv.getArgument(0));

        ReportRun result = service.generate("AML-Report", "legacy-1", "COMPLETED", "/tmp/report.pdf");

        assertThat(result.getReportName()).isEqualTo("AML-Report");
        assertThat(result.getTemplateVersion()).isEqualTo("legacy-1");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getOutputPath()).isEqualTo("/tmp/report.pdf");
    }

    private ReportTemplate activeTemplate(Long id, String name, String version) {
        ReportTemplate t = new ReportTemplate();
        t.setId(id);
        t.setName(name);
        t.setVersion(version);
        t.setActive(true);
        return t;
    }

    private ReportRun runningRun(Long id) {
        ReportRun run = new ReportRun();
        run.setId(id);
        run.setReportName("AML-Report");
        run.setTemplateVersion("1.0.0");
        run.setStatus(ReportRunStatus.RUNNING.name());
        return run;
    }
}
