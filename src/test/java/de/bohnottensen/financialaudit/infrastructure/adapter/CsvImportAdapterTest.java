package de.bohnottensen.financialaudit.infrastructure.adapter;

import de.bohnottensen.financialaudit.domain.model.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvImportAdapterTest {

    private CsvImportAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CsvImportAdapter();
    }

    @Test
    void shouldCheckSupportsAndSourceType() {
        assertThat(adapter.supports(new ByteArrayInputStream(new byte[0]))).isTrue();
        assertThat(adapter.supports("string-source")).isFalse();
        assertThat(adapter.supports(null)).isFalse();
        assertThat(adapter.supports(123)).isFalse();
        assertThat(adapter.sourceType()).isEqualTo("CSV");
    }

    @Test
    void shouldThrowWhenSourceIsNotInputStream() {
        assertThatThrownBy(() -> adapter.importTransactions("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source must be an InputStream");

        assertThatThrownBy(() -> adapter.importTransactions(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source must be an InputStream");
    }

    @Test
    void shouldReturnEmptyListForEmptyStream() {
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        List<Booking> bookings = adapter.importTransactions(emptyStream);
        assertThat(bookings).isNotNull().isEmpty();
        bookings.add(new Booking());
        assertThat(bookings).hasSize(1);
    }

    @Test
    void shouldImportValidCsvWithStandardHeaders() {
        String csvContent = "foreignTransactionId,description,amount,currency,transactionTimestamp,sourceAccount,destinationAccount\n" +
                "101,Rent payment,1200.50,eur,2026-07-20T10:00:00,de123,de456";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        List<Booking> bookings = adapter.importTransactions(inputStream);

        assertThat(bookings).hasSize(1);
        Booking booking = bookings.get(0);
        assertThat(booking.getForeignTransactionId()).isEqualTo(101L);
        assertThat(booking.getDescription()).isEqualTo("Rent payment");
        assertThat(booking.getAmount()).isEqualByComparingTo("1200.50");
        assertThat(booking.getCurrency()).isEqualTo("EUR");
        assertThat(booking.getTransactionTimestamp()).isEqualTo(LocalDateTime.of(2026, 7, 20, 10, 0, 0));
        assertThat(booking.getSourceAccount()).isEqualTo("de123");
        assertThat(booking.getDestinationAccount()).isEqualTo("de456");
    }

    @Test
    void shouldImportWithAlternativeGermanAndFormattedHeaders() {
        String csvContent = "Beleg_Nummer,Beschreibung,Betrag,Waehrung,Buchungs-Zeitpunkt,Quell_Konto,Ziel_Konto\n" +
                "202,Gehalt,3500.00,chf,2026-08-01T12:30:00,CH999,CH888";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        List<Booking> bookings = adapter.importTransactions(inputStream);

        assertThat(bookings).hasSize(1);
        Booking booking = bookings.get(0);
        assertThat(booking.getForeignTransactionId()).isEqualTo(202L);
        assertThat(booking.getDescription()).isEqualTo("Gehalt");
        assertThat(booking.getAmount()).isEqualByComparingTo("3500.00");
        assertThat(booking.getCurrency()).isEqualTo("CHF");
        assertThat(booking.getTransactionTimestamp()).isEqualTo(LocalDateTime.of(2026, 8, 1, 12, 30, 0));
        assertThat(booking.getSourceAccount()).isEqualTo("CH999");
        assertThat(booking.getDestinationAccount()).isEqualTo("CH888");
    }

    @Test
    void shouldImportWithDocumentNumberAndWaehrungUmlautAndBookedAt() {
        String csvContent = "Document-Number,Description,Amount,Währung,Booked_At,Source_Account_Id,Destination_Account_Id\n" +
                "303,Office supplies,45.99,usd,2026-08-02T14:15:00,US111,US222";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        List<Booking> bookings = adapter.importTransactions(inputStream);

        assertThat(bookings).hasSize(1);
        Booking booking = bookings.get(0);
        assertThat(booking.getForeignTransactionId()).isEqualTo(303L);
        assertThat(booking.getDescription()).isEqualTo("Office supplies");
        assertThat(booking.getAmount()).isEqualByComparingTo("45.99");
        assertThat(booking.getCurrency()).isEqualTo("USD");
        assertThat(booking.getTransactionTimestamp()).isEqualTo(LocalDateTime.of(2026, 8, 2, 14, 15, 0));
        assertThat(booking.getSourceAccount()).isEqualTo("US111");
        assertThat(booking.getDestinationAccount()).isEqualTo("US222");
    }

    @Test
    void shouldHandleMissingBlankAndInvalidValuesGracefully() {
        String csvContent = "belegnummer,description,amount,currency,transactiontimestamp,sourceaccount,destinationaccount\n" +
                "not-a-long,   ,invalid-amount,,invalid-date,END,\n" +
                ",,,,,\n" +
                "505";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        List<Booking> bookings = adapter.importTransactions(inputStream);

        assertThat(bookings).hasSize(3);

        Booking b1 = bookings.get(0);
        assertThat(b1.getForeignTransactionId()).isNull();
        assertThat(b1.getDescription()).isEqualTo("");
        assertThat(b1.getAmount()).isNull();
        assertThat(b1.getCurrency()).isEqualTo("");
        assertThat(b1.getTransactionTimestamp()).isNull();
        assertThat(b1.getSourceAccount()).isEqualTo("END");
        assertThat(b1.getDestinationAccount()).isNull(); // trailing comma dropped by split

        Booking b2 = bookings.get(1);
        assertThat(b2.getForeignTransactionId()).isNull();
        assertThat(b2.getDescription()).isNull();
        assertThat(b2.getAmount()).isNull();
        assertThat(b2.getCurrency()).isNull();
        assertThat(b2.getTransactionTimestamp()).isNull();
        assertThat(b2.getSourceAccount()).isNull();
        assertThat(b2.getDestinationAccount()).isNull();

        Booking b3 = bookings.get(2);
        assertThat(b3.getForeignTransactionId()).isEqualTo(505L);
        assertThat(b3.getDescription()).isNull();
        assertThat(b3.getAmount()).isNull();
        assertThat(b3.getCurrency()).isNull();
        assertThat(b3.getTransactionTimestamp()).isNull();
        assertThat(b3.getSourceAccount()).isNull();
        assertThat(b3.getDestinationAccount()).isNull();
    }

    @Test
    void shouldNormalizeHeadersCorrectly() {
        assertThat(adapter.normalizeHeader(null)).isEqualTo("");
        assertThat(adapter.normalizeHeader("   ")).isEqualTo("");
        assertThat(adapter.normalizeHeader("  Transaction_Timestamp-Id ")).isEqualTo("transactiontimestampid");
    }

    @Test
    void shouldWrapIOExceptionInRuntimeException() {
        InputStream faultyStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated read error");
            }
        };

        assertThatThrownBy(() -> adapter.importTransactions(faultyStream))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to import CSV")
                .hasCauseInstanceOf(IOException.class);
    }
}
