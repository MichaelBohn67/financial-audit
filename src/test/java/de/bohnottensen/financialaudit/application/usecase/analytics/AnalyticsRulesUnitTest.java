package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.domain.model.Booking;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsRulesUnitTest {

    @Test
    void testHighAmountThresholdAnalyticsRule() {
        HighAmountThresholdAnalyticsRule rule = new HighAmountThresholdAnalyticsRule();

        Booking bookingNullAmount = new Booking();
        Booking bookingBelow = new Booking();
        bookingBelow.setAmount(new BigDecimal("100000.00"));
        Booking bookingAbove = new Booking();
        bookingAbove.setAmount(new BigDecimal("100000.01"));

        List<AnalyticsRuleMatch> matches = rule.evaluate(List.of(bookingNullAmount, bookingBelow, bookingAbove));
        assertThat(matches).hasSize(1);
        AnalyticsRuleMatch match = matches.get(0);
        assertThat(match.booking()).isSameAs(bookingAbove);
        assertThat(match.ruleName()).isEqualTo("HIGH_AMOUNT_THRESHOLD");
        assertThat(match.alertDescription()).isEqualTo("Booking exceeds threshold of 100000");
        assertThat(match.riskLevel()).isEqualTo("HIGH");

        assertThat(rule.evaluate(Collections.emptyList())).isEmpty();
    }

    @Test
    void testTimeWindowAnalyticsRule() {
        TimeWindowAnalyticsRule rule = new TimeWindowAnalyticsRule();

        Booking nullTime = new Booking();
        Booking regular1 = createBookingWithHour(6); // 06:00 -> within normal
        Booking regular2 = createBookingWithHour(21); // 21:00 -> within normal
        Booking late1 = createBookingWithHour(22); // 22:00 -> late
        Booking late2 = createBookingWithHour(23); // 23:00 -> late
        Booking early1 = createBookingWithHour(0); // 00:00 -> early
        Booking early2 = createBookingWithHour(5); // 05:00 -> early

        List<AnalyticsRuleMatch> matches = rule.evaluate(List.of(nullTime, regular1, regular2, late1, late2, early1, early2));
        assertThat(matches).hasSize(4);
        assertThat(matches.stream().map(AnalyticsRuleMatch::booking).toList())
                .containsExactly(late1, late2, early1, early2);

        for (AnalyticsRuleMatch match : matches) {
            assertThat(match.ruleName()).isEqualTo("TIME_WINDOW_RULE");
            assertThat(match.alertDescription()).isEqualTo("Booking outside normal working hours");
            assertThat(match.riskLevel()).isEqualTo("MEDIUM");
        }
    }

    @Test
    void testRepeatedAmountPatternAnalyticsRule() {
        RepeatedAmountPatternAnalyticsRule rule = new RepeatedAmountPatternAnalyticsRule();

        Booking invalid1 = new Booking(); // nulls
        Booking invalid2 = new Booking();
        invalid2.setAmount(new BigDecimal("100.00"));
        invalid2.setCurrency("EUR"); // missing accounts & timestamp

        // Valid group with < 3 items
        Booking g1_1 = createPatternBooking("ACC1", "ACC2", "EUR", "500.00", LocalDateTime.parse("2026-08-01T10:00:00"));
        Booking g1_2 = createPatternBooking("ACC1", "ACC2", "EUR", "500.00", LocalDateTime.parse("2026-08-01T11:00:00"));

        // Valid group with >= 3 items within 24h
        Booking g2_1 = createPatternBooking("ACC3", "ACC4", "EUR", "700.00", LocalDateTime.parse("2026-08-01T10:00:00"));
        Booking g2_2 = createPatternBooking("ACC3", "ACC4", "EUR", "700.00", LocalDateTime.parse("2026-08-01T15:00:00"));
        Booking g2_3 = createPatternBooking("ACC3", "ACC4", "EUR", "700.00", LocalDateTime.parse("2026-08-02T09:00:00")); // +23h
        Booking g2_4 = createPatternBooking("ACC3", "ACC4", "EUR", "700.00", LocalDateTime.parse("2026-08-02T09:30:00"));

        // Valid group with exactly 24 hours span
        Booking gExact_1 = createPatternBooking("ACC7", "ACC8", "EUR", "150.00", LocalDateTime.parse("2026-08-01T10:00:00"));
        Booking gExact_2 = createPatternBooking("ACC7", "ACC8", "EUR", "150.00", LocalDateTime.parse("2026-08-01T12:00:00"));
        Booking gExact_3 = createPatternBooking("ACC7", "ACC8", "EUR", "150.00", LocalDateTime.parse("2026-08-02T10:00:00")); // exactly +24h

        // Valid group with >= 3 items exceeding 24h
        Booking g3_1 = createPatternBooking("ACC5", "ACC6", "EUR", "900.00", LocalDateTime.parse("2026-08-01T10:00:00"));
        Booking g3_2 = createPatternBooking("ACC5", "ACC6", "EUR", "900.00", LocalDateTime.parse("2026-08-02T15:00:00"));
        Booking g3_3 = createPatternBooking("ACC5", "ACC6", "EUR", "900.00", LocalDateTime.parse("2026-08-03T12:00:00"));

        List<AnalyticsRuleMatch> matches = rule.evaluate(List.of(
                invalid1, invalid2, g1_1, g1_2, g2_1, g2_2, g2_3, g2_4, gExact_1, gExact_2, gExact_3, g3_1, g3_2, g3_3
        ));

        assertThat(matches).hasSize(2);
        assertThat(matches.stream().map(AnalyticsRuleMatch::booking).toList())
                .containsExactlyInAnyOrder(g2_2, gExact_2);
        for (AnalyticsRuleMatch match : matches) {
            assertThat(match.ruleName()).isEqualTo("PATTERN_REPEAT_RULE");
            assertThat(match.alertDescription()).startsWith("Repeated transfer pattern detected within 24h for ");
            assertThat(match.riskLevel()).isEqualTo("MEDIUM");
        }
    }

    private Booking createBookingWithHour(int hour) {
        Booking booking = new Booking();
        booking.setTransactionTimestamp(LocalDateTime.of(2026, 8, 1, hour, 30));
        return booking;
    }

    private Booking createPatternBooking(String src, String dest, String currency, String amount, LocalDateTime time) {
        Booking booking = new Booking();
        booking.setSourceAccount(src);
        booking.setDestinationAccount(dest);
        booking.setCurrency(currency);
        booking.setAmount(new BigDecimal(amount));
        booking.setTransactionTimestamp(time);
        return booking;
    }
}
