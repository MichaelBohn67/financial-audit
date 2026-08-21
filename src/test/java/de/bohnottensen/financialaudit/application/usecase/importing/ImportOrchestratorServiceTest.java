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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ImportOrchestratorServiceTest {

    @Test
    void shouldRejectBlankImportContextFieldsAndDefaultBlankLabel() {
        assertThatThrownBy(() -> new ImportRunContext(null, "P1", "D1", "label"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("tenantId must not be blank");
        assertThatThrownBy(() -> new ImportRunContext("T1", " ", "D1", "label"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("projectId must not be blank");
        assertThatThrownBy(() -> new ImportRunContext("T1", "P1", "", "label"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("documentId must not be blank");

        assertThat(new ImportRunContext("T1", "P1", "D1", " ").asProtocolContext())
                .isEqualTo("tenantId=T1;projectId=P1;documentId=D1;label=default");
    }

    @Test
    void shouldRejectBlankOpenBankingSourceContextFields() {
        assertThatThrownBy(() -> new OpenBankingImportSource(null, "P1", "A1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("tenantId must not be blank");
        assertThatThrownBy(() -> new OpenBankingImportSource("T1", " ", "A1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("projectId must not be blank");
    }

    @Test
    void shouldAcceptOpenBankingAccountIdLengthBoundaries() {
        TransactionSourcePort source = new TransactionSourcePort() {
            @Override
            public boolean supports(Object value) { return true; }

            @Override
            public String sourceType() { return "OPEN_BANKING"; }

            @Override
            public List<Booking> importTransactions(Object value) {
                Booking minimum = createBooking(1L, "minimum", new BigDecimal("10.00"));
                minimum.setSourceAccount("DE123");
                minimum.setDestinationAccount("AT123");

                Booking maximum = createBooking(2L, "maximum", new BigDecimal("20.00"));
                maximum.setSourceAccount("DE" + "A".repeat(32));
                maximum.setDestinationAccount("AT" + "B".repeat(32));

                Booking tooShort = createBooking(3L, "short", new BigDecimal("30.00"));
                tooShort.setSourceAccount("DE12");
                tooShort.setDestinationAccount("AT12");

                Booking tooLong = createBooking(4L, "long", new BigDecimal("40.00"));
                tooLong.setSourceAccount("DE" + "A".repeat(33));
                tooLong.setDestinationAccount("AT" + "B".repeat(33));
                return List.of(minimum, maximum, tooShort, tooLong);
            }
        };

        ImportJobResult result = new ImportOrchestratorService(
                List.of(source), bookingRepository, importJobRepository, protocolEntryRepository
        ).importFrom("accounts");

        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.invalidCount()).isEqualTo(4);
        assertThat(result.validationErrors().stream().map(ImportValidationError::error))
                .allMatch(error -> error.contains("account id format is invalid"));
    }

    private BookingRepository bookingRepository;
    private ImportJobRepository importJobRepository;
    private ImportJobProtocolEntryRepository protocolEntryRepository;
    private List<ImportJob> jobSnapshots;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        importJobRepository = mock(ImportJobRepository.class);
        protocolEntryRepository = mock(ImportJobProtocolEntryRepository.class);
        jobSnapshots = new ArrayList<>();

        when(importJobRepository.save(any(ImportJob.class))).thenAnswer(invocation -> {
            ImportJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(10L);
            }
            ImportJob copy = new ImportJob();
            copy.setId(job.getId());
            copy.setTenantId(job.getTenantId());
            copy.setProjectId(job.getProjectId());
            copy.setDocumentId(job.getDocumentId());
            copy.setSourceType(job.getSourceType());
            copy.setStatus(job.getStatus());
            copy.setStartedAt(job.getStartedAt());
            copy.setFinishedAt(job.getFinishedAt());
            copy.setRecordCount(job.getRecordCount());
            copy.setImportedCount(job.getImportedCount());
            copy.setInvalidCount(job.getInvalidCount());
            copy.setChecksum(job.getChecksum());
            jobSnapshots.add(copy);
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

        ArgumentCaptor<ImportJob> jobCaptor = ArgumentCaptor.forClass(ImportJob.class);
        verify(importJobRepository, atLeast(2)).save(jobCaptor.capture());
        List<ImportJob> savedJobs = jobCaptor.getAllValues();

        ImportJob completedJob = savedJobs.get(savedJobs.size() - 1);
        assertThat(completedJob.getTenantId()).isEqualTo("T1");
        assertThat(completedJob.getProjectId()).isEqualTo("P1");
        assertThat(completedJob.getDocumentId()).isEqualTo("D1");
        assertThat(completedJob.getSourceType()).isEqualTo("GENERIC");
        assertThat(completedJob.getStatus()).isEqualTo("COMPLETED");
        assertThat(completedJob.getStartedAt()).isNotNull();
        assertThat(completedJob.getFinishedAt()).isNotNull();
        assertThat(completedJob.getRecordCount()).isEqualTo(2);
        assertThat(completedJob.getImportedCount()).isEqualTo(1);
        assertThat(completedJob.getInvalidCount()).isEqualTo(2);
        assertThat(completedJob.getChecksum()).isEqualTo(result.checksum());

        ImportJob initialJob = jobSnapshots.get(0);
        assertThat(initialJob.getStatus()).isEqualTo("RUNNING");
        assertThat(initialJob.getSourceType()).isEqualTo("UNKNOWN");
        assertThat(initialJob.getStartedAt()).isNotNull();
        assertThat(initialJob.getRecordCount()).isEqualTo(0);
        assertThat(initialJob.getImportedCount()).isEqualTo(0);
        assertThat(initialJob.getInvalidCount()).isEqualTo(0);

        ImportJob secondJob = jobSnapshots.get(1);
        assertThat(secondJob.getSourceType()).isEqualTo("GENERIC");

        ArgumentCaptor<ImportJobProtocolEntry> protocolCaptor = ArgumentCaptor.forClass(ImportJobProtocolEntry.class);
        verify(protocolEntryRepository, atLeastOnce()).save(protocolCaptor.capture());
        List<ImportJobProtocolEntry> protocolEntries = protocolCaptor.getAllValues();
        assertThat(protocolEntries).isNotEmpty();
        for (int i = 0; i < protocolEntries.size(); i++) {
            ImportJobProtocolEntry entry = protocolEntries.get(i);
            assertThat(entry.getImportJobId()).isEqualTo(10L);
            assertThat(entry.getEntryIndex()).isEqualTo(i);
            assertThat(entry.getLevel()).isEqualTo("VALIDATION");
            assertThat(entry.getMessage()).isNotEmpty();
        }
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

                Booking duplicateForeignId = createBooking(200L, "DuplicateForeign", new BigDecimal("60.00"));
                duplicateForeignId.setSourceAccount("DE777");
                duplicateForeignId.setDestinationAccount("DE888");

                return List.of(valid, missingTransactionId, nonPositiveTransactionId, futureTimestamp,
                        invalidSourceAccount, invalidDestAccount, duplicatePayload, duplicateForeignId);
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
        assertThat(result.invalidCount()).isGreaterThanOrEqualTo(7);
        assertThat(result.validationErrors().stream().map(ImportValidationError::error))
                .anyMatch(error -> error.contains("OpenBanking transaction id is required and must be positive"))
                .anyMatch(error -> error.contains("timestamp is in the future"))
                .anyMatch(error -> error.contains("OpenBanking source account id format is invalid"))
                .anyMatch(error -> error.contains("OpenBanking destination account id format is invalid"))
                .anyMatch(error -> error.contains("Duplicate OpenBanking transaction payload"))
                .anyMatch(error -> error.contains("Duplicate foreign transaction id"));
    }

    @Test
    void shouldSelectMatchingAdapterWhenMultipleAdaptersConfigured() {
        TransactionSourcePort ignoredPort = new TransactionSourcePort() {
            @Override
            public boolean supports(Object source) {
                return false;
            }

            @Override
            public String sourceType() {
                return "IGNORED";
            }

            @Override
            public List<Booking> importTransactions(Object source) {
                return List.of();
            }
        };

        TransactionSourcePort matchingPort = new TransactionSourcePort() {
            @Override
            public boolean supports(Object source) {
                return "matching".equals(source);
            }

            @Override
            public String sourceType() {
                return "MATCHING_TYPE";
            }

            @Override
            public List<Booking> importTransactions(Object source) {
                return List.of(createBooking(1L, "Test", new BigDecimal("100.00")));
            }
        };

        ImportOrchestratorService service = new ImportOrchestratorService(
                List.of(ignoredPort, matchingPort),
                bookingRepository,
                importJobRepository,
                protocolEntryRepository
        );

        ImportJobResult result = service.importFrom("matching");
        assertThat(result.sourceType()).isEqualTo("MATCHING_TYPE");
        assertThat(result.importedCount()).isEqualTo(1);
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
        assertThat(failedJob.getFinishedAt()).isNotNull();
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
            public String sourceType() {
                return "FAILING";
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
        assertThat(failedJob.getFinishedAt()).isNotNull();
        assertThat(failedJob.getErrorMessage()).isEqualTo("Simulated network failure");

        ArgumentCaptor<ImportJobProtocolEntry> protocolCaptor = ArgumentCaptor.forClass(ImportJobProtocolEntry.class);
        verify(protocolEntryRepository).save(protocolCaptor.capture());
        ImportJobProtocolEntry protocolEntry = protocolCaptor.getValue();
        assertThat(protocolEntry.getImportJobId()).isEqualTo(10L);
        assertThat(protocolEntry.getEntryIndex()).isEqualTo(0);
        assertThat(protocolEntry.getLevel()).isEqualTo("ERROR");
        assertThat(protocolEntry.getMessage()).isEqualTo("Simulated network failure");
    }

    @Test
    void shouldComputeDifferentChecksumForDifferentBookingData() {
        Booking b1 = createBooking(1L, "Payment 1", new BigDecimal("100.00"));
        b1.setSourceAccount("ACC1");
        b1.setDestinationAccount("ACC2");
        b1.setCurrency("EUR");
        b1.setTransactionTimestamp(LocalDateTime.of(2026, 8, 1, 10, 0));

        Booking b2 = createBooking(1L, "Payment 2", new BigDecimal("100.00"));
        b2.setSourceAccount("ACC1");
        b2.setDestinationAccount("ACC2");
        b2.setCurrency("EUR");
        b2.setTransactionTimestamp(LocalDateTime.of(2026, 8, 1, 10, 0));

        TransactionSourcePort port1 = new TransactionSourcePort() {
            @Override public boolean supports(Object source) { return "s1".equals(source); }
            @Override public String sourceType() { return "CSV"; }
            @Override public List<Booking> importTransactions(Object source) { return List.of(b1); }
        };
        TransactionSourcePort port2 = new TransactionSourcePort() {
            @Override public boolean supports(Object source) { return "s2".equals(source); }
            @Override public String sourceType() { return "CSV"; }
            @Override public List<Booking> importTransactions(Object source) { return List.of(b2); }
        };

        ImportOrchestratorService service = new ImportOrchestratorService(
                List.of(port1, port2), bookingRepository, importJobRepository, protocolEntryRepository
        );

        ImportJobResult res1 = service.importFrom("s1");
        ImportJobResult res2 = service.importFrom("s2");

        assertThat(res1.checksum()).isNotNull().hasSize(64);
        assertThat(res2.checksum()).isNotNull().hasSize(64);
        assertThat(res1.checksum()).isNotEqualTo(res2.checksum());
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
