package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class AnalyticsRuleServiceTest {

    @Test
    void shouldCreateFindingsForThresholdTimeWindowAndPatternRules() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        FindingRepository findingRepository = mock(FindingRepository.class);
        AuditTrailWriter auditTrailWriter = mock(AuditTrailWriter.class);

        Booking highAndLate = booking(10L, "HIGH", new BigDecimal("150000.00"), "EUR",
                LocalDateTime.parse("2026-08-01T23:15:00"), "DE900", "DE901");
        Booking p1 = booking(11L, "P1", new BigDecimal("999.00"), "EUR",
                LocalDateTime.parse("2026-08-01T10:00:00"), "DE111", "DE222");
        Booking p2 = booking(12L, "P2", new BigDecimal("999.00"), "EUR",
                LocalDateTime.parse("2026-08-01T11:00:00"), "DE111", "DE222");
        Booking p3 = booking(13L, "P3", new BigDecimal("999.00"), "EUR",
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

        for (Finding f : findings) {
            assertThat(f.getStatus()).isEqualTo("NEW");
            assertThat(f.getAnalysisRunId()).isEqualTo("RULE-v1");
            assertThat(f.getRuleVersion()).isEqualTo("v1");
            assertThat(f.getRunContext()).isEqualTo("task11");
            assertThat(f.getBooking()).isNotNull();
            assertThat(f.getAlertDescription()).isNotEmpty();
        }

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditTrailWriter, times(3)).record(
                eq("FINDING"),
                anyLong(),
                eq("FINDING_CREATED"),
                eq("SYSTEM_ANALYTICS"),
                anyString(),
                isNull(),
                payloadCaptor.capture()
        );

        List<String> payloads = payloadCaptor.getAllValues();
        assertThat(payloads).allMatch(p -> p.contains("bookingId=")
                && p.contains(";ruleName=")
                && p.contains(";riskLevel=")
                && p.contains(";status=NEW")
                && p.contains(";analysisRunId=RULE-v1")
                && p.contains(";ruleVersion=v1")
                && p.contains(";runContext=task11"));
    }

    @Test
    void shouldHandleRuleMatchesWithNullBooking() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        FindingRepository findingRepository = mock(FindingRepository.class);
        AuditTrailWriter auditTrailWriter = mock(AuditTrailWriter.class);

        AnalyticsRule customRule = bookings -> List.of(
                new AnalyticsRuleMatch(null, "CUSTOM_RULE", "Custom Alert", "LOW")
        );

        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());
        when(findingRepository.save(any(Finding.class))).thenAnswer(invocation -> {
            Finding f = invocation.getArgument(0);
            f.setId(99L);
            return f;
        });

        AnalyticsRuleService service = new AnalyticsRuleService(
                bookingRepository,
                findingRepository,
                auditTrailWriter,
                List.of(customRule)
        );

        List<Finding> findings = service.run("v2", "null-booking-ctx");
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getBooking()).isNull();
        assertThat(findings.get(0).getRiskLevel()).isEqualTo("LOW");
        assertThat(findings.get(0).getRuleName()).isEqualTo("CUSTOM_RULE");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditTrailWriter).record(
                eq("FINDING"),
                eq(99L),
                eq("FINDING_CREATED"),
                eq("SYSTEM_ANALYTICS"),
                eq("Finding created by analytics rule CUSTOM_RULE"),
                isNull(),
                payloadCaptor.capture()
        );

        assertThat(payloadCaptor.getValue()).isEqualTo(
                "bookingId=null;ruleName=CUSTOM_RULE;riskLevel=LOW;status=NEW;analysisRunId=RULE-v2;ruleVersion=v2;runContext=null-booking-ctx"
        );
    }

    private Booking booking(Long id,
                            String description,
                            BigDecimal amount,
                            String currency,
                            LocalDateTime timestamp,
                            String sourceAccount,
                            String destinationAccount) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setDescription(description);
        booking.setAmount(amount);
        booking.setCurrency(currency);
        booking.setTransactionTimestamp(timestamp);
        booking.setSourceAccount(sourceAccount);
        booking.setDestinationAccount(destinationAccount);
        return booking;
    }
}
