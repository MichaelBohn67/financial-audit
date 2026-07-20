package de.bohnottensen.financialaudit.infrastructure.adapter;

import de.bohnottensen.financialaudit.application.ports.TransactionSourcePort;
import de.bohnottensen.financialaudit.domain.model.Booking;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * REQ-INT-001: CSV Implementation of TransactionSourcePort.
 */
public class CsvImportAdapter implements TransactionSourcePort {

    @Override
    public List<Booking> importTransactions(Object source) {
        if (!(source instanceof InputStream)) {
            throw new IllegalArgumentException("Source must be an InputStream");
        }

        List<Booking> bookings = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream) source))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length >= 6) {
                    Booking booking = new Booking();
                    booking.setDescription(parts[0]);
                    booking.setAmount(new BigDecimal(parts[1]));
                    booking.setCurrency(parts[2]);
                    booking.setTransactionTimestamp(LocalDateTime.parse(parts[3]));
                    booking.setSourceAccount(parts[4]);
                    booking.setDestinationAccount(parts[5]);
                    bookings.add(booking);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to import CSV", e);
        }
        return bookings;
    }
}
