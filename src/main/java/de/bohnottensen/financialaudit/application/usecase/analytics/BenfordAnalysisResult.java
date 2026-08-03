package de.bohnottensen.financialaudit.application.usecase.analytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BenfordAnalysisResult(
        Long runDbId,
        String runId,
        String ruleVersion,
        String runContext,
        int bookingCount,
        int suspiciousDigitCount,
        LocalDateTime createdAt,
        List<DigitResult> digitResults
) {
    public record DigitResult(
            int digit,
            BigDecimal expectedRatio,
            BigDecimal observedRatio,
            BigDecimal absoluteDeviation,
            int sampleSize
    ) {
    }
}
