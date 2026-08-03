package de.bohnottensen.financialaudit.application.usecase.importing;

import de.bohnottensen.financialaudit.application.ports.TransactionSourcePort;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ImportJobProtocolEntryRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ImportJobRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class ImportOrchestratorServiceTest {

    @Test
    void shouldValidateAndPersistOnlyAcceptedBookings() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        ImportJobRepository importJobRepository = mock(ImportJobRepository.class);
        ImportJobProtocolEntryRepository protocolEntryRepository = mock(ImportJobProtocolEntryRepository.class);
        when(importJobRepository.save(any())).thenAnswer(invocation -> {
            de.bohnottensen.financialaudit.domain.model.ImportJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(1L);
            }
            return job;
        });
        TransactionSourcePort sourcePort = new TransactionSourcePort() {
            @Override
            public List<Booking> importTransactions(Object source) {
                Booking valid = createBooking(1L, "Valid", new BigDecimal("100.00"));
                Booking invalid = createBooking(2L, "", new BigDecimal("-1"));
                return List.of(valid, invalid);
            }
        };

        ImportOrchestratorService service = new ImportOrchestratorService(
                List.of(sourcePort),
                bookingRepository,
                importJobRepository,
                protocolEntryRepository
        );
        ImportJobResult result = service.importFrom("in-memory");

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.invalidCount()).isGreaterThan(0);
        verify(bookingRepository).saveAll(anyList());
    }

    private Booking createBooking(Long foreignId, String description, BigDecimal amount) {
        Booking booking = new Booking();
        booking.setForeignTransactionId(foreignId);
        booking.setDescription(description);
        booking.setAmount(amount);
        booking.setCurrency("EUR");
        booking.setTransactionTimestamp(LocalDateTime.now());
        booking.setSourceAccount("SRC");
        booking.setDestinationAccount("DST");
        return booking;
    }
}
