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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BenfordAnalysisServiceTest {

    @Test
    void shouldPersistBenfordRunAndDigitStats() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        FindingRepository findingRepository = mock(FindingRepository.class);
        BenfordAnalysisRunRepository runRepository = mock(BenfordAnalysisRunRepository.class);
        BenfordDigitStatRepository statRepository = mock(BenfordDigitStatRepository.class);
        AuditTrailWriter auditTrailWriter = mock(AuditTrailWriter.class);

        List<Booking> bookings = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            bookings.add(booking(900 + i, "B" + i, new BigDecimal("9" + i + ".00")));
        }
        when(bookingRepository.findAll()).thenReturn(bookings);

        AtomicLong runIds = new AtomicLong(1L);
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

        assertThat(result.bookingCount()).isEqualTo(10);
        assertThat(result.digitResults()).hasSize(9);
        verify(statRepository, times(9)).save(any(BenfordDigitStat.class));
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
