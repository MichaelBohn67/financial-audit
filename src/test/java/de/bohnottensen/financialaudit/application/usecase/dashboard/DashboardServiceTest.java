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

        when(findings.countByRiskLevel("LOW")).thenReturn(1L);
        when(findings.countByRiskLevel("MEDIUM")).thenReturn(2L);
        when(findings.countByRiskLevel("HIGH")).thenReturn(3L);

        when(findings.countByStatus("NEW")).thenReturn(4L);
        when(findings.countByStatus("EVALUATED")).thenReturn(5L);
        when(findings.countByStatus("ESCALATED")).thenReturn(6L);
        when(findings.countByStatus("CLOSED")).thenReturn(7L);

        when(findings.countByRemediationStatus("OPEN")).thenReturn(8L);
        when(findings.countByRemediationStatus("IN_PROGRESS")).thenReturn(9L);
        when(findings.countByRemediationStatus("READY_FOR_REVIEW")).thenReturn(10L);
        when(findings.countByRemediationStatus("RESOLVED")).thenReturn(11L);
        when(findings.countByRemediationStatus("REJECTED")).thenReturn(12L);
        when(findings.countByRemediationStatus("CLOSED")).thenReturn(13L);

        when(workpapers.countByStatus("DRAFT")).thenReturn(14L);
        when(workpapers.countByStatus("IN_REVIEW")).thenReturn(15L);
        when(workpapers.countByStatus("APPROVED")).thenReturn(16L);
        when(workpapers.countByStatus("SIGNED_OFF")).thenReturn(2L);

        when(findings.countByRemediationStatusIn(any())).thenReturn(2L);
        when(findings.countByRemediationDueDateBeforeAndRemediationStatusNot(any(), any())).thenReturn(1L);
        when(workpapers.count()).thenReturn(4L);
        when(samplingRuns.count()).thenReturn(3L);
        when(reportRuns.findTop5ByOrderByGeneratedAtDesc()).thenReturn(List.of(report()));

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setId(100L);
        auditEvent.setEntityType("BOOKING");
        auditEvent.setEntityId(1L);
        auditEvent.setEventType("BOOKING_CREATED");
        auditEvent.setActor("admin");
        auditEvent.getMetadata().initializeOccurredAtIfMissing();
        when(auditEvents.findTop10ByOrderByMetadata_OccurredAtDesc()).thenReturn(List.of(auditEvent));

        DashboardService.Metrics metrics = service.metrics();

        assertThat(metrics.totalBookings()).isEqualTo(12);
        assertThat(metrics.totalFindings()).isEqualTo(4);
        assertThat(metrics.findingsByRisk()).containsEntry("LOW", 1L).containsEntry("MEDIUM", 2L).containsEntry("HIGH", 3L);
        assertThat(metrics.findingsByStatus()).containsEntry("NEW", 4L).containsEntry("EVALUATED", 5L).containsEntry("ESCALATED", 6L).containsEntry("CLOSED", 7L);
        assertThat(metrics.remediationByStatus()).containsEntry("OPEN", 8L).containsEntry("IN_PROGRESS", 9L).containsEntry("READY_FOR_REVIEW", 10L).containsEntry("RESOLVED", 11L).containsEntry("REJECTED", 12L).containsEntry("CLOSED", 13L);
        assertThat(metrics.workpapersByStatus()).containsEntry("DRAFT", 14L).containsEntry("IN_REVIEW", 15L).containsEntry("APPROVED", 16L).containsEntry("SIGNED_OFF", 2L);
        assertThat(metrics.openRemediation()).isEqualTo(2);
        assertThat(metrics.overdueRemediation()).isEqualTo(1);
        assertThat(metrics.samplingRuns()).isEqualTo(3);
        assertThat(metrics.auditProgress().totalWorkpapers()).isEqualTo(4);
        assertThat(metrics.auditProgress().signedOffWorkpapers()).isEqualTo(2);
        assertThat(metrics.auditProgress().completionPercentage()).isEqualByComparingTo("50.0");
        assertThat(metrics.latestReports()).hasSize(1);
        assertThat(metrics.recentAuditEvents()).hasSize(1);
        assertThat(metrics.recentAuditEvents().get(0).id()).isEqualTo(100L);
        assertThat(metrics.recentAuditEvents().get(0).entityType()).isEqualTo("BOOKING");
        assertThat(metrics.recentAuditEvents().get(0).entityId()).isEqualTo(1L);
        assertThat(metrics.recentAuditEvents().get(0).eventType()).isEqualTo("BOOKING_CREATED");
        assertThat(metrics.recentAuditEvents().get(0).actor()).isEqualTo("admin");
        assertThat(metrics.recentAuditEvents().get(0).occurredAt()).isNotNull();
    }

    @Test
    void handlesZeroWorkpapersProgressGracefully() {
        when(workpapers.count()).thenReturn(0L);
        when(workpapers.countByStatus("SIGNED_OFF")).thenReturn(0L);

        DashboardService.Metrics metrics = service.metrics();

        assertThat(metrics.auditProgress().totalWorkpapers()).isEqualTo(0);
        assertThat(metrics.auditProgress().completionPercentage()).isEqualByComparingTo(java.math.BigDecimal.ZERO);
    }

    private ReportRun report() {
        ReportRun report = new ReportRun();
        report.setId(7L);
        report.setReportName("Quarterly report");
        report.setStatus("COMPLETED");
        report.setTemplateVersion("1.0");
        report.setGeneratedAt(LocalDateTime.of(2026, 8, 19, 12, 0));
        report.setCompletedAt(LocalDateTime.of(2026, 8, 19, 12, 0));
        return report;
    }
}
