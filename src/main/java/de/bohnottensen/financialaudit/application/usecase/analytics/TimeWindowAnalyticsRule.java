package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.domain.model.Booking;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TimeWindowAnalyticsRule implements AnalyticsRule {

    @Override
    public List<AnalyticsRuleMatch> evaluate(List<Booking> bookings) {
        List<AnalyticsRuleMatch> matches = new ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.getTransactionTimestamp() == null) {
                continue;
            }
            int hour = booking.getTransactionTimestamp().getHour();
            if (hour >= 22 || hour < 6) {
                matches.add(new AnalyticsRuleMatch(
                        booking,
                        "TIME_WINDOW_RULE",
                        "Booking outside normal working hours",
                        "MEDIUM"
                ));
            }
        }
        return matches;
    }
}
