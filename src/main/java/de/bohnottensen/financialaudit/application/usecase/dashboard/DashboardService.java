package de.bohnottensen.financialaudit.application.usecase.dashboard;

import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import de.bohnottensen.financialaudit.domain.model.ReportRun;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ReportRunRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.WorkpaperRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
    private static final List<String> RISK_LEVELS = List.of("LOW", "MEDIUM", "HIGH");
    private static final List<String> FINDING_STATUSES = List.of("NEW", "EVALUATED", "ESCALATED", "CLOSED");
    private static final List<String> REMEDIATION_STATUSES =
            List.of("OPEN", "IN_PROGRESS", "READY_FOR_REVIEW", "RESOLVED", "REJECTED", "CLOSED");
    private static final List<String> WORKPAPER_STATUSES =
            List.of("DRAFT", "IN_REVIEW", "APPROVED", "SIGNED_OFF");

    private final BookingRepository bookings;
    private final FindingRepository findings;
    private final WorkpaperRepository workpapers;
    private final SamplingRunRepository samplingRuns;
    private final ReportRunRepository reportRuns;
    private final AuditEventRepository auditEvents;

    public DashboardService(BookingRepository bookings, FindingRepository findings,
                            WorkpaperRepository workpapers, SamplingRunRepository samplingRuns,
                            ReportRunRepository reportRuns, AuditEventRepository auditEvents) {
        this.bookings = bookings;
        this.findings = findings;
        this.workpapers = workpapers;
        this.samplingRuns = samplingRuns;
        this.reportRuns = reportRuns;
        this.auditEvents = auditEvents;
    }

    public Metrics metrics() {
        long totalWorkpapers = workpapers.count();
        long signedOffWorkpapers = workpapers.countByStatus("SIGNED_OFF");
        return new Metrics(
                bookings.count(),
                findings.count(),
                countsByRisk(),
                countsByFindingStatus(),
                countsByRemediationStatus(),
                findings.countByRemediationStatusIn(List.of("OPEN", "IN_PROGRESS", "READY_FOR_REVIEW", "REJECTED")),
                findings.countByRemediationDueDateBeforeAndRemediationStatusNot(LocalDate.now(), "CLOSED"),
                countsByWorkpaperStatus(),
                samplingRuns.count(),
                new AuditProgress(totalWorkpapers, signedOffWorkpapers, percentage(signedOffWorkpapers, totalWorkpapers)),
                reportRuns.findTop5ByOrderByGeneratedAtDesc().stream().map(this::report).toList(),
                auditEvents.findTop10ByOrderByMetadata_OccurredAtDesc().stream().map(this::auditEvent).toList()
        );
    }

    private Map<String, Long> countsByRisk() {
        return counts(RISK_LEVELS, findings::countByRiskLevel);
    }

    private Map<String, Long> countsByFindingStatus() {
        return counts(FINDING_STATUSES, findings::countByStatus);
    }

    private Map<String, Long> countsByRemediationStatus() {
        return counts(REMEDIATION_STATUSES, findings::countByRemediationStatus);
    }

    private Map<String, Long> countsByWorkpaperStatus() {
        return counts(WORKPAPER_STATUSES, workpapers::countByStatus);
    }

    private Map<String, Long> counts(List<String> keys, java.util.function.Function<String, Long> counter) {
        Map<String, Long> result = new LinkedHashMap<>();
        keys.forEach(key -> result.put(key, counter.apply(key)));
        return result;
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private LatestReport report(ReportRun run) {
        return new LatestReport(run.getId(), run.getReportName(), run.getStatus(), run.getGeneratedAt());
    }

    private RecentAuditEvent auditEvent(AuditEvent event) {
        return new RecentAuditEvent(event.getId(), event.getEntityType(), event.getEntityId(),
                event.getEventType(), event.getActor(), event.getOccurredAt());
    }

    public record Metrics(long totalBookings, long totalFindings,
                          Map<String, Long> findingsByRisk,
                          Map<String, Long> findingsByStatus,
                          Map<String, Long> remediationByStatus,
                          long openRemediation, long overdueRemediation,
                          Map<String, Long> workpapersByStatus,
                          long samplingRuns,
                          AuditProgress auditProgress,
                          List<LatestReport> latestReports,
                          List<RecentAuditEvent> recentAuditEvents) {
    }

    public record AuditProgress(long totalWorkpapers, long signedOffWorkpapers,
                                BigDecimal completionPercentage) {
    }

    public record LatestReport(Long id, String name, String status, java.time.LocalDateTime generatedAt) {
    }

    public record RecentAuditEvent(Long id, String entityType, Long entityId, String eventType,
                                   String actor, java.time.LocalDateTime occurredAt) {
    }
}
