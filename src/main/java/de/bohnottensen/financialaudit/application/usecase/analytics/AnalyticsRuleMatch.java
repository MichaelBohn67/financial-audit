package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.domain.model.Booking;

public record AnalyticsRuleMatch(
        Booking booking,
        String ruleName,
        String alertDescription,
        String riskLevel
) {
}
