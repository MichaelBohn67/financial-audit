package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsRuleServiceTest {

    @Test
    void shouldCreateFindingsForThresholdTimeWindowAndPatternRules() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        FindingRepository findingRepository = mock(FindingRepository.class);
        AuditTrailWriter auditTrailWriter = mock(AuditTrailWriter.class);

        Booking highAndLate = booking("HIGH", new BigDecimal("150000.00"), "EUR",
                LocalDateTime.parse("2026-08-01T23:15:00"), "DE900", "DE901");
        Booking p1 = booking("P1", new BigDecimal("999.00"), "EUR",
                LocalDateTime.parse("2026-08-01T10:00:00"), "DE111", "DE222");
        Booking p2 = booking("P2", new BigDecimal("999.00"), "EUR",
                LocalDateTime.parse("2026-08-01T11:00:00"), "DE111", "DE222");
        Booking p3 = booking("P3", new BigDecimal("999.00"), "EUR",
                LocalDateTime.parse("2026-08-01T12:00:00"), "DE111", "DE222");

        when(bookingRepository.findAll()).thenReturn(List.of(highAndLate, p1, p2, p3));
        AtomicLong ids = new AtomicLong(1L);
        when(findingRepository.save(any(Finding.class))).thenAnswer(invocation -> {
            Finding finding = invocation.getArgument(0);
            finding.setId(ids.getAndIncrement());
            return finding;
        });

        AnalyticsRuleService service = new AnalyticsRuleService(
                bookingRepository,
                findingRepository,
                auditTrailWriter,
                List.of(
                        new HighAmountThresholdAnalyticsRule(),
                        new TimeWindowAnalyticsRule(),
                        new RepeatedAmountPatternAnalyticsRule()
                )
        );

        List<Finding> findings = service.run("v1", "task11");

        assertThat(findings).hasSize(3);
        List<String> rules = findings.stream().map(Finding::getRuleName).collect(Collectors.toList());
        assertThat(rules).containsExactlyInAnyOrder(
                "HIGH_AMOUNT_THRESHOLD",
                "TIME_WINDOW_RULE",
                "PATTERN_REPEAT_RULE"
        );
        verify(auditTrailWriter, times(3)).record(any(), any(), any(), any(), any(), any(), any());
    }

    private Booking booking(String description,
                            BigDecimal amount,
                            String currency,
                            LocalDateTime timestamp,
                            String sourceAccount,
                            String destinationAccount) {
        Booking booking = new Booking();
        booking.setDescription(description);
        booking.setAmount(amount);
        booking.setCurrency(currency);
        booking.setTransactionTimestamp(timestamp);
        booking.setSourceAccount(sourceAccount);
        booking.setDestinationAccount(destinationAccount);
        return booking;
    }
}
