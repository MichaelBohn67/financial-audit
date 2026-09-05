package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.BenfordAnalysisRun;
import de.bohnottensen.financialaudit.domain.model.BenfordDigitStat;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.infrastructure.persistence.BenfordAnalysisRunRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.BenfordDigitStatRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BenfordAnalysisServiceTest {

    @Test
    void shouldApplyBenfordEligibilityAndDigitRulesAtBoundaries() {
        BenfordAnalysisService service = new BenfordAnalysisService(
                mock(BookingRepository.class), mock(FindingRepository.class),
                mock(BenfordAnalysisRunRepository.class), mock(BenfordDigitStatRepository.class),
                mock(AuditTrailWriter.class));

        Booking zero = booking(1, "zero", BigDecimal.ZERO);
        Booking one = booking(2, "one", new BigDecimal("100.00"));
        Booking fractional = booking(3, "fractional", new BigDecimal("0.00070"));
        Booking onlyZeros = booking(4, "only zeros", new BigDecimal("0.0000"));

        assertThat(service.isEligibleForBenford(zero)).isFalse();
        assertThat(service.isEligibleForBenford(onlyZeros)).isFalse();
        assertThat(service.isEligibleForBenford(one)).isTrue();
        assertThat(service.isEligibleForBenford(fractional)).isTrue();
        assertThat(service.leadingDigit(one)).isEqualTo(1);
        assertThat(service.leadingDigit(fractional)).isEqualTo(7);
        assertThat(service.leadingDigit(onlyZeros)).isEqualTo(-1);
        assertThat(service.benfordExpectedRatio(1)).isEqualByComparingTo("0.301030");
        assertThat(service.benfordExpectedRatio(9)).isEqualByComparingTo("0.045757");
    }

    @Test
    void shouldPersistBenfordRunAndDigitStats() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        FindingRepository findingRepository = mock(FindingRepository.class);
        BenfordAnalysisRunRepository runRepository = mock(BenfordAnalysisRunRepository.class);
        BenfordDigitStatRepository statRepository = mock(BenfordDigitStatRepository.class);
        AuditTrailWriter auditTrailWriter = mock(AuditTrailWriter.class);

        List<Booking> bookings = new ArrayList<>();
        // Add 10 bookings with leading digit 9, 1 booking with leading digit 1 (0.123), 1 booking with 0.005 (leading digit 5)
        for (int i = 0; i < 10; i++) {
            Booking b = booking(900 + i, "B" + i, new BigDecimal("9" + i + ".00"));
            b.setId((long) (900 + i));
            bookings.add(b);
        }
        Booking bDec1 = booking(950, "B_Dec1", new BigDecimal("0.1234"));
        bDec1.setId(950L);
        Booking bDec5 = booking(951, "B_Dec5", new BigDecimal("0.0056"));
        bDec5.setId(951L);
        bookings.add(bDec1);
        bookings.add(bDec5);

        when(bookingRepository.findAll()).thenReturn(bookings);

        AtomicLong runIds = new AtomicLong(42L);
        List<Integer> persistedSuspiciousCounts = new ArrayList<>();
        when(runRepository.save(any(BenfordAnalysisRun.class))).thenAnswer(invocation -> {
            BenfordAnalysisRun run = invocation.getArgument(0);
            persistedSuspiciousCounts.add(run.getSuspiciousDigitCount());
            if (run.getId() == null) {
                run.setId(runIds.getAndIncrement());
            }
            return run;
        });

        when(statRepository.save(any(BenfordDigitStat.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(findingRepository.save(any(Finding.class))).thenAnswer(invocation -> {
            Finding finding = invocation.getArgument(0);
            finding.setId(99L);
            return finding;
        });

        BenfordAnalysisService service = new BenfordAnalysisService(
                bookingRepository,
                findingRepository,
                runRepository,
                statRepository,
                auditTrailWriter
        );

        BenfordAnalysisResult result = service.run("v1", "task12");

        assertThat(result.bookingCount()).isEqualTo(12);
        assertThat(result.ruleVersion()).isEqualTo("v1");
        assertThat(result.runContext()).isEqualTo("task12");
        assertThat(result.runId()).startsWith("BENFORD-");
        assertThat(result.runDbId()).isEqualTo(42L);
        assertThat(result.digitResults()).hasSize(9);
        assertThat(result.suspiciousDigitCount()).isGreaterThan(0);

        ArgumentCaptor<BenfordDigitStat> statCaptor = ArgumentCaptor.forClass(BenfordDigitStat.class);
        verify(statRepository, times(9)).save(statCaptor.capture());
        List<BenfordDigitStat> savedStats = statCaptor.getAllValues();
        assertThat(savedStats).hasSize(9);
        for (int i = 0; i < 9; i++) {
            BenfordDigitStat s = savedStats.get(i);
            assertThat(s.getBenfordRunId()).isEqualTo(42L);
            assertThat(s.getLeadingDigit()).isEqualTo(i + 1);
            assertThat(s.getSampleSize()).isEqualTo(12);
            assertThat(s.getExpectedRatio()).isNotNull();
            assertThat(s.getObservedRatio()).isNotNull();
            assertThat(s.getAbsoluteDeviation()).isNotNull();
        }

        ArgumentCaptor<Finding> findingCaptor = ArgumentCaptor.forClass(Finding.class);
        verify(findingRepository, times(result.suspiciousDigitCount())).save(findingCaptor.capture());
        for (Finding f : findingCaptor.getAllValues()) {
            assertThat(f.getRiskLevel()).isEqualTo("MEDIUM");
            assertThat(f.getStatus()).isEqualTo("NEW");
            assertThat(f.getRuleVersion()).isEqualTo("v1");
            assertThat(f.getRunContext()).isEqualTo("task12");
            assertThat(f.getAnalysisRunId()).isEqualTo(result.runId());
            assertThat(f.getRuleName()).startsWith("BENFORD_DIGIT_");
            assertThat(f.getBooking()).isNotNull();
            assertThat(f.getAlertDescription()).contains("Benford deviation for leading digit");
        }

        ArgumentCaptor<BenfordAnalysisRun> runCaptor = ArgumentCaptor.forClass(BenfordAnalysisRun.class);
        verify(runRepository, times(2)).save(runCaptor.capture());
        assertThat(persistedSuspiciousCounts).containsExactly(0, result.suspiciousDigitCount());
        assertThat(runCaptor.getAllValues().get(1).getSuspiciousDigitCount()).isEqualTo(result.suspiciousDigitCount());

        ArgumentCaptor<String> auditPayloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditTrailWriter, times(result.suspiciousDigitCount())).record(
                eq("FINDING"),
                eq(99L),
                eq("FINDING_CREATED"),
                eq("SYSTEM_BENFORD"),
                eq("Finding created by Benford analysis"),
                isNull(),
                auditPayloadCaptor.capture()
        );
        for (String payload : auditPayloadCaptor.getAllValues()) {
            assertThat(payload).contains("bookingId=900;ruleName=BENFORD_DIGIT_")
                    .contains(";riskLevel=MEDIUM")
                    .contains(";status=NEW")
                    .contains(";analysisRunId=" + result.runId())
                    .contains(";ruleVersion=v1")
                    .contains(";runContext=task12");
        }
    }

    @Test
    void shouldHandleEmptyAndIneligibleBookings() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        FindingRepository findingRepository = mock(FindingRepository.class);
        BenfordAnalysisRunRepository runRepository = mock(BenfordAnalysisRunRepository.class);
        BenfordDigitStatRepository statRepository = mock(BenfordDigitStatRepository.class);
        AuditTrailWriter auditTrailWriter = mock(AuditTrailWriter.class);

        Booking zeroAmount = booking(1, "Zero", BigDecimal.ZERO);
        Booking negativeAmount = booking(2, "Neg", new BigDecimal("-50.00"));
        Booking nullAmount = booking(3, "Null", null);
        Booking onlyZeros = booking(4, "ZerosOnly", new BigDecimal("0.0000"));

        when(bookingRepository.findAll()).thenReturn(List.of(zeroAmount, negativeAmount, nullAmount, onlyZeros));
        when(runRepository.save(any(BenfordAnalysisRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statRepository.save(any(BenfordDigitStat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BenfordAnalysisService service = new BenfordAnalysisService(
                bookingRepository,
                findingRepository,
                runRepository,
                statRepository,
                auditTrailWriter
        );

        BenfordAnalysisResult result = service.run("v1", "empty-test");

        assertThat(result.bookingCount()).isEqualTo(0);
        assertThat(result.suspiciousDigitCount()).isEqualTo(0);
        assertThat(result.digitResults()).hasSize(9);
        for (BenfordAnalysisResult.DigitResult digit : result.digitResults()) {
            assertThat(digit.observedRatio()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(digit.sampleSize()).isEqualTo(0);
        }
        verify(findingRepository, times(0)).save(any());
        verify(auditTrailWriter, times(0)).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldCountEveryLeadingDigitIncludingFractionalAmounts() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        FindingRepository findingRepository = mock(FindingRepository.class);
        BenfordAnalysisRunRepository runRepository = mock(BenfordAnalysisRunRepository.class);
        BenfordDigitStatRepository statRepository = mock(BenfordDigitStatRepository.class);
        AuditTrailWriter auditTrailWriter = mock(AuditTrailWriter.class);

        // The fractional values exercise the scan past the decimal point. The
        // negative value verifies that eligibility and digit extraction remain
        // separate concerns for callers that use signed ledger values.
        List<Booking> bookings = new ArrayList<>();
        bookings.add(booking(1, "one", new BigDecimal("100")));
        bookings.add(booking(2, "two", new BigDecimal("20")));
        bookings.add(booking(3, "three", new BigDecimal("3")));
        bookings.add(booking(4, "four", new BigDecimal("0.4")));
        bookings.add(booking(5, "five", new BigDecimal("0.05")));
        bookings.add(booking(6, "six", new BigDecimal("0.006")));
        bookings.add(booking(7, "seven", new BigDecimal("0.0007")));
        bookings.add(booking(8, "eight", new BigDecimal("0.00008")));
        bookings.add(booking(9, "nine", new BigDecimal("0.000009")));
        bookings.add(booking(10, "negative", new BigDecimal("-80")));
        when(bookingRepository.findAll()).thenReturn(bookings);
        when(runRepository.save(any(BenfordAnalysisRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statRepository.save(any(BenfordDigitStat.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(findingRepository.save(any(Finding.class))).thenAnswer(invocation -> {
            Finding finding = invocation.getArgument(0);
            finding.setId(1L);
            return finding;
        });

        BenfordAnalysisResult result = new BenfordAnalysisService(
                bookingRepository, findingRepository, runRepository, statRepository, auditTrailWriter
        ).run("digits", "boundary");

        assertThat(result.bookingCount()).isEqualTo(9);
        assertThat(result.digitResults()).extracting(BenfordAnalysisResult.DigitResult::observedRatio)
                .containsExactly(
                        new BigDecimal("0.111111"), new BigDecimal("0.111111"), new BigDecimal("0.111111"),
                        new BigDecimal("0.111111"), new BigDecimal("0.111111"), new BigDecimal("0.111111"),
                        new BigDecimal("0.111111"), new BigDecimal("0.111111"), new BigDecimal("0.111111")
                );
        verify(statRepository, times(9)).save(any(BenfordDigitStat.class));
    }

    @Test
    void shouldEvaluateSuspiciousThresholdBoundariesAroundPointZeroEight() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        FindingRepository findingRepository = mock(FindingRepository.class);
        BenfordAnalysisRunRepository runRepository = mock(BenfordAnalysisRunRepository.class);
        BenfordDigitStatRepository statRepository = mock(BenfordDigitStatRepository.class);
        AuditTrailWriter auditTrailWriter = mock(AuditTrailWriter.class);

        when(runRepository.save(any(BenfordAnalysisRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statRepository.save(any(BenfordDigitStat.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(findingRepository.save(any(Finding.class))).thenAnswer(invocation -> {
            Finding f = invocation.getArgument(0);
            f.setId(100L);
            return f;
        });

        // Test with a synthetic service or controlled distribution
        BenfordAnalysisService service = new BenfordAnalysisService(
                bookingRepository, findingRepository, runRepository, statRepository, auditTrailWriter
        );

        // Leading digit 1 expected ratio is 0.301030.
        // If sampleSize = 100:
        // observedCount = 38 -> observedRatio = 0.380000 -> deviation = 0.078970 (< 0.08, not suspicious)
        // observedCount = 39 -> observedRatio = 0.390000 -> deviation = 0.088970 (> 0.08, suspicious)
        List<Booking> bookings = new ArrayList<>();
        for (int i = 0; i < 38; i++) {
            Booking b = booking(i + 1, "B1", new BigDecimal("100.00"));
            b.setId((long) (i + 1));
            bookings.add(b);
        }
        for (int i = 0; i < 62; i++) {
            Booking b = booking(100 + i, "B2", new BigDecimal("200.00"));
            b.setId((long) (100 + i));
            bookings.add(b);
        }
        when(bookingRepository.findAll()).thenReturn(bookings);

        BenfordAnalysisResult res = service.run("v1", "ctx");
        // Check digit 1 deviation is 0.078970 (below 0.08)
        BenfordAnalysisResult.DigitResult d1 = res.digitResults().stream().filter(d -> d.digit() == 1).findFirst().orElseThrow();
        assertThat(d1.observedRatio()).isEqualByComparingTo("0.380000");
        assertThat(d1.absoluteDeviation()).isEqualByComparingTo("0.078970");

        // Verify leadingDigit returns -1 for unparseable or zero amounts
        Booking zeroDigits = booking(999, "zeros", new BigDecimal("0.000"));
        assertThat(service.leadingDigit(zeroDigits)).isEqualTo(-1);
    }

    private Booking booking(long foreignId, String description, BigDecimal amount) {
        Booking booking = new Booking();
        booking.setForeignTransactionId(foreignId);
        booking.setDescription(description);
        booking.setAmount(amount);
        booking.setCurrency("EUR");
        booking.setTransactionTimestamp(LocalDateTime.parse("2026-08-01T10:00:00"));
        booking.setSourceAccount("DE111");
        booking.setDestinationAccount("DE222");
        return booking;
    }
}
