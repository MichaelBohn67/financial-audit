package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.domain.model.Booking;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RepeatedAmountPatternAnalyticsRule implements AnalyticsRule {

    @Override
    public List<AnalyticsRuleMatch> evaluate(List<Booking> bookings) {
        Map<String, List<Booking>> grouped = new HashMap<>();
        for (Booking booking : bookings) {
            if (booking.getAmount() == null
                    || booking.getCurrency() == null
                    || booking.getSourceAccount() == null
                    || booking.getDestinationAccount() == null
                    || booking.getTransactionTimestamp() == null) {
                continue;
            }
            String key = booking.getSourceAccount() + "|" + booking.getDestinationAccount() + "|"
                    + booking.getCurrency() + "|" + booking.getAmount();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(booking);
        }

        List<AnalyticsRuleMatch> matches = new ArrayList<>();
        grouped.forEach((key, groupBookings) -> {
            if (groupBookings.size() < 3) {
                return;
            }
            List<Booking> sorted = groupBookings.stream()
                    .sorted(Comparator.comparing(Booking::getTransactionTimestamp))
                    .toList();
            for (int i = 2; i < sorted.size(); i++) {
                Booking first = sorted.get(i - 2);
                Booking third = sorted.get(i);
                long hours = Duration.between(first.getTransactionTimestamp(), third.getTransactionTimestamp()).toHours();
                if (hours <= 24) {
                    Booking triggeredBooking = sorted.get(i - 1);
                    matches.add(new AnalyticsRuleMatch(
                            triggeredBooking,
                            "PATTERN_REPEAT_RULE",
                            "Repeated transfer pattern detected within 24h for " + key,
                            "MEDIUM"
                    ));
                    break;
                }
            }
        });
        return matches;
    }
}
