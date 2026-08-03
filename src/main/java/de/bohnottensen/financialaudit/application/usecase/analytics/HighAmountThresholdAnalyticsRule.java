package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.domain.model.Booking;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class HighAmountThresholdAnalyticsRule implements AnalyticsRule {

    private static final BigDecimal THRESHOLD = BigDecimal.valueOf(100000);

    @Override
    public List<AnalyticsRuleMatch> evaluate(List<Booking> bookings) {
        List<AnalyticsRuleMatch> matches = new ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.getAmount() != null && booking.getAmount().compareTo(THRESHOLD) > 0) {
                matches.add(new AnalyticsRuleMatch(
                        booking,
                        "HIGH_AMOUNT_THRESHOLD",
                        "Booking exceeds threshold of 100000",
                        "HIGH"
                ));
            }
        }
        return matches;
    }
}
