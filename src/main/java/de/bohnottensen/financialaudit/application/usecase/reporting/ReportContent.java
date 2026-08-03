package de.bohnottensen.financialaudit.application.usecase.reporting;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Immutable structured report content assembled from audit artefacts.
 * Designed to be serializable as a structured export (JSON / XBRL-ready).
 */
public record ReportContent(

        // --- Report header ---
        String reportName,
        String templateVersion,
        LocalDateTime generatedAt,
        String triggeredBy,
        String parameters,

        // --- Findings summary ---
        FindingsSummary findingsSummary,

        // --- Booking statistics ---
        BookingStats bookingStats,

        // --- Sampling overview ---
        List<SamplingRunSummary> samplingRuns

) {

    public record FindingsSummary(
            long totalFindings,
            long highRisk,
            long mediumRisk,
            long lowRisk,
            long newFindings,
            long escalatedFindings,
            List<FindingEntry> entries
    ) {}

    public record FindingEntry(
            Long id,
            Long bookingId,
            String ruleName,
            String alertDescription,
            String riskLevel,
            String status,
            String analysisRunId,
            String ruleVersion,
            LocalDateTime createdAt
    ) {}

    public record BookingStats(
            long totalBookings,
            BigDecimal totalAmount,
            long bookingsWithFindings
    ) {}

    public record SamplingRunSummary(
            Long id,
            String runName,
            String samplingStrategy,
            Long seed,
            Long populationSize,
            Long sampleSize,
            LocalDateTime createdAt
    ) {}
}
