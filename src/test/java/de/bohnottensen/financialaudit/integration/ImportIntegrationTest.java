package de.bohnottensen.financialaudit.integration;

import de.bohnottensen.financialaudit.application.usecase.importing.ImportJobResult;
import de.bohnottensen.financialaudit.application.usecase.importing.ImportOrchestratorService;
import de.bohnottensen.financialaudit.application.usecase.importing.ImportRunContext;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ImportJobRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ImportJobProtocolEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: CSV import → BookingRepository + ImportJobRepository,
 * verifying that the full import pipeline works end-to-end with a real H2 database.
 */
@SpringBootTest
@Transactional
class ImportIntegrationTest {

    @Autowired
    private ImportOrchestratorService importOrchestratorService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ImportJobRepository importJobRepository;

    @Autowired
    private ImportJobProtocolEntryRepository protocolEntryRepository;

    private static final String CSV_HEADER =
            "foreignTransactionId,description,amount,currency,transactionTimestamp,sourceAccount,destinationAccount\n";

    @Test
    void shouldImportValidCsvBookingsAndPersistImportJob() {
        String csv = CSV_HEADER
                + "1001,Invoice A,5000.00,EUR,2026-01-10T10:00:00,DE11000000001234567890,DE22000000009876543210\n"
                + "1002,Invoice B,2500.00,USD,2026-01-11T11:00:00,DE11000000001234567890,DE33000000005555555555\n";

        long bookingsBefore = bookingRepository.count();
        long jobsBefore = importJobRepository.count();

        ImportRunContext ctx = new ImportRunContext("T1", "P1", "D1", "csv-test");
        ImportJobResult result = importOrchestratorService.importFrom(ctx,
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.invalidCount()).isZero();
        assertThat(result.sourceType()).isEqualTo("CSV");
        assertThat(result.checksum()).isNotBlank();

        assertThat(bookingRepository.count()).isEqualTo(bookingsBefore + 2);
        assertThat(importJobRepository.count()).isEqualTo(jobsBefore + 1);
    }

    @Test
    void shouldRejectInvalidBookingsAndWriteValidationProtocol() {
        // Row 1: missing amount → validation error
        // Row 2: missing foreignTransactionId → CSV-specific error
        String csv = CSV_HEADER
                + "2001,,EUR,2026-01-10T10:00:00,DE11000000001234567890,DE22000000009876543210\n"
                + ",Invoice C,1000.00,EUR,2026-01-10T10:00:00,DE11000000001234567890,DE33000000005555555555\n";

        long bookingsBefore = bookingRepository.count();

        ImportRunContext ctx = new ImportRunContext("T1", "P1", "D1", "csv-test");
        ImportJobResult result = importOrchestratorService.importFrom(ctx,
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.importedCount()).isZero();
        assertThat(result.invalidCount()).isGreaterThan(0);
        // No bookings were persisted
        assertThat(bookingRepository.count()).isEqualTo(bookingsBefore);
        // Protocol entries written for errors
        assertThat(protocolEntryRepository.count()).isGreaterThan(0);
    }

    @Test
    void shouldMarkJobAsCompletedWithCorrectCounts() {
        String csv = CSV_HEADER
                + "3001,Invoice D,9000.00,EUR,2026-01-15T09:00:00,DE11000000001234567890,DE44000000001111111111\n";

        ImportRunContext ctx = new ImportRunContext("T2", "P2", "D2", "csv-test");
        ImportJobResult result = importOrchestratorService.importFrom(ctx,
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.invalidCount()).isZero();
        assertThat(importJobRepository.findById(result.jobId())).isPresent();
    }
}
