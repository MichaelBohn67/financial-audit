package de.bohnottensen.financialaudit.application.usecase.dashboard;

import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import de.bohnottensen.financialaudit.domain.model.ReportRun;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ReportRunRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.WorkpaperRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock BookingRepository bookings;
    @Mock FindingRepository findings;
    @Mock WorkpaperRepository workpapers;
    @Mock SamplingRunRepository samplingRuns;
    @Mock ReportRunRepository reportRuns;
    @Mock AuditEventRepository auditEvents;
    @InjectMocks DashboardService service;

    @Test
    void aggregatesDashboardMetricsAndProgress() {
        when(bookings.count()).thenReturn(12L);
        when(findings.count()).thenReturn(4L);
        when(findings.countByRiskLevel(any())).thenReturn(1L);
        when(findings.countByStatus(any())).thenReturn(1L);
        when(findings.countByRemediationStatus(any())).thenReturn(1L);
        when(findings.countByRemediationStatusIn(any())).thenReturn(2L);
        when(findings.countByRemediationDueDateBeforeAndRemediationStatusNot(any(), any())).thenReturn(1L);
        when(workpapers.count()).thenReturn(4L);
        when(workpapers.countByStatus(any())).thenReturn(1L);
        when(workpapers.countByStatus("SIGNED_OFF")).thenReturn(2L);
        when(samplingRuns.count()).thenReturn(3L);
        when(reportRuns.findTop5ByOrderByGeneratedAtDesc()).thenReturn(List.of(report()));
        when(auditEvents.findTop10ByOrderByMetadata_OccurredAtDesc()).thenReturn(List.of());

        DashboardService.Metrics metrics = service.metrics();

        assertThat(metrics.totalBookings()).isEqualTo(12);
        assertThat(metrics.findingsByRisk()).containsEntry("HIGH", 1L);
        assertThat(metrics.openRemediation()).isEqualTo(2);
        assertThat(metrics.overdueRemediation()).isEqualTo(1);
        assertThat(metrics.auditProgress().completionPercentage()).isEqualByComparingTo("50.0");
        assertThat(metrics.latestReports()).hasSize(1);
    }

    private ReportRun report() {
        ReportRun report = new ReportRun();
        report.setId(7L);
        report.setReportName("Quarterly report");
        report.setStatus("COMPLETED");
        report.setTemplateVersion("1.0");
        report.setCompletedAt(LocalDateTime.now());
        return report;
    }
}
