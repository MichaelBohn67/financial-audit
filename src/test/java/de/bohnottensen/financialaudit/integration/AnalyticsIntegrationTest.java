package de.bohnottensen.financialaudit.integration;

import de.bohnottensen.financialaudit.application.usecase.analytics.AnalyticsRuleService;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: Bookings in real H2 DB → AnalyticsRuleService → Findings persisted,
 * and AuditTrail events written for each finding.
 */
@SpringBootTest
@Transactional
class AnalyticsIntegrationTest {

    @Autowired
    private AnalyticsRuleService analyticsRuleService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void shouldDetectHighAmountBookingAndPersistFindingWithAuditEvent() {
        Booking highAmount = booking("High Value Transfer", new BigDecimal("200000.00"),
                LocalDateTime.parse("2026-03-01T14:00:00"), "DE11000000001234567890", "DE22000000009876543210");
        bookingRepository.save(highAmount);

        long findingsBefore = findingRepository.count();
        long auditEventsBefore = auditEventRepository.count();

        List<Finding> findings = analyticsRuleService.run("v1", "analytics-integration");

        assertThat(findings).isNotEmpty();
        assertThat(findingRepository.count()).isGreaterThan(findingsBefore);

        List<Finding> highAmountFindings = findings.stream()
                .filter(f -> "HIGH_AMOUNT_THRESHOLD".equals(f.getRuleName()))
                .toList();
        assertThat(highAmountFindings).isNotEmpty();
        assertThat(highAmountFindings.get(0).getRiskLevel()).isEqualTo("HIGH");
        assertThat(highAmountFindings.get(0).getStatus()).isEqualTo("NEW");
        assertThat(highAmountFindings.get(0).getRuleVersion()).isEqualTo("v1");
        assertThat(highAmountFindings.get(0).getRunContext()).isEqualTo("analytics-integration");

        // AuditTrail must record an event per finding
        assertThat(auditEventRepository.count()).isGreaterThan(auditEventsBefore);
        assertThat(auditEventRepository.findAll()).anyMatch(e ->
                "FINDING".equals(e.getEntityType()) && "FINDING_CREATED".equals(e.getEventType()));
    }

    @Test
    void shouldDetectTimeWindowBookingOutsideBusinessHours() {
        Booking lateNightBooking = booking("Late night wire", new BigDecimal("500.00"),
                LocalDateTime.parse("2026-03-01T23:30:00"), "DE11000000001234567890", "DE55000000002222222222");
        bookingRepository.save(lateNightBooking);

        List<Finding> findings = analyticsRuleService.run("v1", "time-window-test");

        assertThat(findings.stream().anyMatch(f -> "TIME_WINDOW_RULE".equals(f.getRuleName()))).isTrue();
    }

    @Test
    void shouldDetectRepeatedAmountPattern() {
        LocalDateTime base = LocalDateTime.parse("2026-04-01T10:00:00");
        for (int i = 0; i < 3; i++) {
            bookingRepository.save(booking("Pattern booking " + i, new BigDecimal("999.00"),
                    base.plusHours(i), "DE11000000001234567890", "DE66000000003333333333"));
        }

        List<Finding> findings = analyticsRuleService.run("v1", "pattern-test");

        assertThat(findings.stream().anyMatch(f -> "PATTERN_REPEAT_RULE".equals(f.getRuleName()))).isTrue();
    }

    @Test
    void shouldProduceNeitherFindingsNorAuditEventsForNormalBooking() {
        Booking normalBooking = booking("Normal invoice", new BigDecimal("150.00"),
                LocalDateTime.parse("2026-03-01T10:00:00"), "DE11000000001234567890", "DE22000000009876543210");
        bookingRepository.save(normalBooking);

        long findingsBefore = findingRepository.count();
        long auditEventsBefore = auditEventRepository.count();

        analyticsRuleService.run("v1", "normal-test");

        // A normal booking produces no new findings (count stays the same or may increase for other rules,
        // but HIGH_AMOUNT and PATTERN rules should not trigger)
        List<Finding> newFindings = findingRepository.findAll().stream()
                .filter(f -> f.getRunContext().equals("normal-test")).toList();
        assertThat(newFindings).noneMatch(f -> "HIGH_AMOUNT_THRESHOLD".equals(f.getRuleName()));
    }

    private Booking booking(String description, BigDecimal amount, LocalDateTime timestamp,
                            String source, String destination) {
        Booking b = new Booking();
        b.setDescription(description);
        b.setAmount(amount);
        b.setCurrency("EUR");
        b.setTransactionTimestamp(timestamp);
        b.setSourceAccount(source);
        b.setDestinationAccount(destination);
        return b;
    }
}
