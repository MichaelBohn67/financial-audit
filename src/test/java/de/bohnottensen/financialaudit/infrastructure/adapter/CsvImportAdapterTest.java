package de.bohnottensen.financialaudit.infrastructure.adapter;

import de.bohnottensen.financialaudit.domain.model.Booking;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvImportAdapterTest {

    @Test
    void shouldImportValidCsv() {
        String csvContent = "description,amount,currency,transactionTimestamp,sourceAccount,destinationAccount\n" +
                "Rent,1200.00,EUR,2026-07-20T10:00:00,DE123,DE456";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        CsvImportAdapter adapter = new CsvImportAdapter();

        List<Booking> bookings = adapter.importTransactions(inputStream);

        assertThat(bookings).hasSize(1);
        Booking booking = bookings.get(0);
        assertThat(booking.getDescription()).isEqualTo("Rent");
        assertThat(booking.getAmount()).isEqualByComparingTo("1200.00");
        assertThat(booking.getCurrency()).isEqualTo("EUR");
        assertThat(booking.getSourceAccount()).isEqualTo("DE123");
        assertThat(booking.getDestinationAccount()).isEqualTo("DE456");
    }
}
