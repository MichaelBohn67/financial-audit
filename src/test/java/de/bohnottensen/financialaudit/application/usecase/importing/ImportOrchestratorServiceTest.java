package de.bohnottensen.financialaudit.application.usecase.importing;

import de.bohnottensen.financialaudit.application.ports.TransactionSourcePort;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.ImportJob;
import de.bohnottensen.financialaudit.domain.model.ImportJobProtocolEntry;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ImportJobProtocolEntryRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ImportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ImportOrchestratorServiceTest {

    private BookingRepository bookingRepository;
    private ImportJobRepository importJobRepository;
    private ImportJobProtocolEntryRepository protocolEntryRepository;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        importJobRepository = mock(ImportJobRepository.class);
        protocolEntryRepository = mock(ImportJobProtocolEntryRepository.class);

        when(importJobRepository.save(any(ImportJob.class))).thenAnswer(invocation -> {
            ImportJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(10L);
            }
            return job;
        });
    }

    @Test
    void shouldValidateAndPersistOnlyAcceptedBookings() {
        TransactionSourcePort sourcePort = new TransactionSourcePort() {
            @Override
            public boolean supports(Object source) {
                return true;
            }

            @Override
            public String sourceType() {
                return "GENERIC";
            }

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

        ImportRunContext runContext = new ImportRunContext("T1", "P1", "D1", "custom-context");
        ImportJobResult result = service.importFrom(runContext, "in-memory");

        assertThat(result.jobId()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.sourceType()).isEqualTo("GENERIC");
        assertThat(result.runContext()).isEqualTo("tenantId=T1;projectId=P1;documentId=D1;label=custom-context");
        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.invalidCount()).isEqualTo(2);
        assertThat(result.checksum()).isNotEmpty();
        assertThat(result.startedAt()).isNotNull();
        assertThat(result.finishedAt()).isNotNull();

        verify(bookingRepository).saveAll(anyList());
        verify(protocolEntryRepository, atLeastOnce()).save(any(ImportJobProtocolEntry.class));
    }

    @Test
    void shouldApplyCsvSpecificCompletenessDuplicateAndGapChecks() {
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

    @Test
    void shouldApplyOpenBankingCompletenessConsistencyAndDataQualityChecks() {
        TransactionSourcePort openBankingSourcePort = new TransactionSourcePort() {
            @Override
            public boolean supports(Object source) {
                return true;
            }

            @Override
            public String sourceType() {
                return "OPEN_BANKING";
            }

            @Override
            public List<Booking> importTransactions(Object source) {
                Booking valid = createBooking(200L, "Salary", new BigDecimal("500.00"));
                valid.setSourceAccount("DE111");
                valid.setDestinationAccount("DE222");

                Booking missingTransactionId = createBooking(null, "NoTxId", new BigDecimal("10.00"));
                missingTransactionId.setSourceAccount("DE333");
                missingTransactionId.setDestinationAccount("DE444");

                Booking nonPositiveTransactionId = createBooking(0L, "NonPos", new BigDecimal("10.00"));
                nonPositiveTransactionId.setSourceAccount("DE333");
                nonPositiveTransactionId.setDestinationAccount("DE444");

                Booking futureTimestamp = createBooking(201L, "Future", new BigDecimal("20.00"));
                futureTimestamp.setTransactionTimestamp(LocalDateTime.now().plusHours(1));
                futureTimestamp.setSourceAccount("DE555");
                futureTimestamp.setDestinationAccount("DE666");

                Booking invalidSourceAccount = createBooking(202L, "BadSrc", new BigDecimal("30.00"));
                invalidSourceAccount.setSourceAccount("INVALID_SRC!");
                invalidSourceAccount.setDestinationAccount("DE666");

                Booking invalidDestAccount = createBooking(203L, "BadDst", new BigDecimal("30.00"));
                invalidDestAccount.setSourceAccount("DE555");
                invalidDestAccount.setDestinationAccount("INVALID_DST!");

                Booking duplicatePayload = createBooking(204L, "Salary", new BigDecimal("500.00"));
                duplicatePayload.setTransactionTimestamp(valid.getTransactionTimestamp());
                duplicatePayload.setSourceAccount(valid.getSourceAccount());
                duplicatePayload.setDestinationAccount(valid.getDestinationAccount());

                return List.of(valid, missingTransactionId, nonPositiveTransactionId, futureTimestamp,
                        invalidSourceAccount, invalidDestAccount, duplicatePayload);
            }
        };

        ImportOrchestratorService service = new ImportOrchestratorService(
                List.of(openBankingSourcePort),
                bookingRepository,
                importJobRepository,
                protocolEntryRepository
        );

        ImportJobResult result = service.importFrom("open-banking");

        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.invalidCount()).isGreaterThanOrEqualTo(6);
        assertThat(result.validationErrors().stream().map(ImportValidationError::error))
                .anyMatch(error -> error.contains("OpenBanking transaction id is required and must be positive"))
                .anyMatch(error -> error.contains("timestamp is in the future"))
                .anyMatch(error -> error.contains("OpenBanking source account id format is invalid"))
                .anyMatch(error -> error.contains("OpenBanking destination account id format is invalid"))
                .anyMatch(error -> error.contains("Duplicate OpenBanking transaction payload"));
    }

    @Test
    void shouldHandleFailedImportWhenNoAdapterMatches() {
        ImportOrchestratorService service = new ImportOrchestratorService(
                List.of(),
                bookingRepository,
                importJobRepository,
                protocolEntryRepository
        );

        assertThatThrownBy(() -> service.importFrom("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No import adapter supports source");

        ArgumentCaptor<ImportJob> jobCaptor = ArgumentCaptor.forClass(ImportJob.class);
        verify(importJobRepository, atLeastOnce()).save(jobCaptor.capture());
        ImportJob failedJob = jobCaptor.getAllValues().get(jobCaptor.getAllValues().size() - 1);
        assertThat(failedJob.getStatus()).isEqualTo("FAILED");
        assertThat(failedJob.getErrorMessage()).contains("No import adapter supports source");

        verify(protocolEntryRepository).save(any(ImportJobProtocolEntry.class));
    }

    @Test
    void shouldHandleFailedImportWhenExceptionDuringImport() {
        TransactionSourcePort failingPort = new TransactionSourcePort() {
            @Override
            public boolean supports(Object source) {
                return true;
            }

            @Override
            public List<Booking> importTransactions(Object source) {
                throw new IllegalStateException("Simulated network failure");
            }
        };

        ImportOrchestratorService service = new ImportOrchestratorService(
                List.of(failingPort),
                bookingRepository,
                importJobRepository,
                protocolEntryRepository
        );

        assertThatThrownBy(() -> service.importFrom("network-source"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Simulated network failure");

        ArgumentCaptor<ImportJob> jobCaptor = ArgumentCaptor.forClass(ImportJob.class);
        verify(importJobRepository, atLeastOnce()).save(jobCaptor.capture());
        ImportJob failedJob = jobCaptor.getAllValues().get(jobCaptor.getAllValues().size() - 1);
        assertThat(failedJob.getStatus()).isEqualTo("FAILED");
        assertThat(failedJob.getErrorMessage()).isEqualTo("Simulated network failure");
    }

    private Booking createBooking(Long foreignId, String description, BigDecimal amount) {
        Booking booking = new Booking();
        booking.setForeignTransactionId(foreignId);
        booking.setDescription(description);
        booking.setAmount(amount);
        booking.setCurrency("EUR");
        booking.setTransactionTimestamp(LocalDateTime.now());
        booking.setSourceAccount("DE100");
        booking.setDestinationAccount("DE200");
        return booking;
    }
}
