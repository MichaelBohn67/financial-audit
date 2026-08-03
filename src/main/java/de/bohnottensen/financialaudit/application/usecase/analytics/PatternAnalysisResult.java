package de.bohnottensen.financialaudit.application.usecase.analytics;

import java.time.LocalDateTime;
import java.util.List;

public record PatternAnalysisResult(
        Long runDbId,
        String runId,
        String ruleVersion,
        String runContext,
        int bookingCount,
        int issueCount,
        LocalDateTime createdAt,
        List<IssueResult> issues
) {
    public record IssueResult(
            String issueType,
            String severity,
            String referenceKey,
            String description,
            Long primaryBookingId,
            int occurrenceCount,
            String detailsJson
    ) {
    }
}
