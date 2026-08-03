package de.bohnottensen.financialaudit.infrastructure.adapter;

import de.bohnottensen.financialaudit.application.ports.TransactionSourcePort;
import de.bohnottensen.financialaudit.domain.model.Booking;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * REQ-INT-001: CSV Implementation of TransactionSourcePort.
 */
@Component
public class CsvImportAdapter implements TransactionSourcePort {

    @Override
    public boolean supports(Object source) {
        return source instanceof InputStream;
    }

    @Override
    public String sourceType() {
        return "CSV";
    }

    @Override
    public List<Booking> importTransactions(Object source) {
        if (!(source instanceof InputStream)) {
            throw new IllegalArgumentException("Source must be an InputStream");
        }

        List<Booking> bookings = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream) source))) {
            String line = reader.readLine();
            if (line == null) {
                return bookings;
            }
            Map<String, Integer> headerIndex = parseHeader(line);
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                Booking booking = new Booking();
                booking.setForeignTransactionId(parseLong(readField(parts, headerIndex, "foreigntransactionid", "belegnummer", "documentnumber")));
                booking.setDescription(readField(parts, headerIndex, "description", "beschreibung"));
                booking.setAmount(parseAmount(readField(parts, headerIndex, "amount", "betrag")));
                booking.setCurrency(normalizeCurrency(readField(parts, headerIndex, "currency", "waehrung", "währung")));
                booking.setTransactionTimestamp(parseTimestamp(readField(parts, headerIndex, "transactiontimestamp", "bookedat", "buchungszeitpunkt")));
                booking.setSourceAccount(readField(parts, headerIndex, "sourceaccount", "sourceaccountid", "quellkonto"));
                booking.setDestinationAccount(readField(parts, headerIndex, "destinationaccount", "destinationaccountid", "zielkonto"));
                bookings.add(booking);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to import CSV", e);
        }
        return bookings;
    }

    private Map<String, Integer> parseHeader(String headerLine) {
        String[] headers = headerLine.split(",");
        Map<String, Integer> indexByHeader = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            indexByHeader.put(normalizeHeader(headers[i]), i);
        }
        return indexByHeader;
    }

    private String readField(String[] parts, Map<String, Integer> headerIndex, String... candidates) {
        for (String candidate : candidates) {
            Integer index = headerIndex.get(normalizeHeader(candidate));
            if (index != null && index >= 0 && index < parts.length) {
                String value = parts[index];
                return value == null ? null : value.trim();
            }
        }
        return null;
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeCurrency(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
