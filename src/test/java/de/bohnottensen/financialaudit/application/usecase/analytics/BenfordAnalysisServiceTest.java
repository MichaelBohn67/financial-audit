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
        when(runRepository.save(any(BenfordAnalysisRun.class))).thenAnswer(invocation -> {
            BenfordAnalysisRun run = invocation.getArgument(0);
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
        }

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
