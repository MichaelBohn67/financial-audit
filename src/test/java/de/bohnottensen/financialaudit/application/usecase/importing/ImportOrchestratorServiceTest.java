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

    @Test
    void shouldApplyCsvSpecificCompletenessDuplicateAndGapChecks() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        ImportJobRepository importJobRepository = mock(ImportJobRepository.class);
        ImportJobProtocolEntryRepository protocolEntryRepository = mock(ImportJobProtocolEntryRepository.class);
        when(importJobRepository.save(any())).thenAnswer(invocation -> {
            de.bohnottensen.financialaudit.domain.model.ImportJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(2L);
            }
            return job;
        });
        TransactionSourcePort csvSourcePort = new TransactionSourcePort() {
            @Override
            public boolean supports(Object source) {
                return true;
            }

            @Override
            public String sourceType() {
                return "CSV";
            }

            @Override
            public List<Booking> importTransactions(Object source) {
                Booking duplicate1 = createBooking(100L, "A", new BigDecimal("10.00"));
                Booking duplicate2 = createBooking(100L, "B", new BigDecimal("20.00"));
                Booking gap = createBooking(102L, "C", new BigDecimal("30.00"));
                Booking missingDocumentNumber = createBooking(null, "D", new BigDecimal("40.00"));
                return List.of(duplicate1, duplicate2, gap, missingDocumentNumber);
            }
        };

        ImportOrchestratorService service = new ImportOrchestratorService(
                List.of(csvSourcePort),
                bookingRepository,
                importJobRepository,
                protocolEntryRepository
        );

        ImportJobResult result = service.importFrom("csv");

        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.invalidCount()).isGreaterThanOrEqualTo(4);
        assertThat(result.validationErrors().stream().map(ImportValidationError::error))
                .anyMatch(error -> error.contains("Document number"))
                .anyMatch(error -> error.contains("Duplicate foreign transaction id"))
                .anyMatch(error -> error.contains("Gap in foreign transaction ids"));
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
