package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.domain.model.Booking;

import java.util.List;

public interface AnalyticsRule {

    List<AnalyticsRuleMatch> evaluate(List<Booking> bookings);
}
