package de.bohnottensen.financialaudit.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BookingValidator {

    public List<String> validate(Booking booking) {
        List<String> errors = new ArrayList<>();

        if (booking.getDescription() == null || booking.getDescription().isBlank()) {
            errors.add("Description is required");
        }

        if (booking.getAmount() == null) {
            errors.add("Amount is required");
        } else if (booking.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Amount must be positive");
        }

        if (booking.getCurrency() == null || booking.getCurrency().isBlank()) {
            errors.add("Currency is required");
        } else if (!booking.getCurrency().matches("[A-Z]{3}")) {
            errors.add("Currency must be a 3-letter ISO code");
        }

        if (booking.getTransactionTimestamp() == null) {
            errors.add("Transaction Timestamp is required");
        }

        if (booking.getSourceAccount() == null || booking.getSourceAccount().isBlank()) {
            errors.add("Source Account is required");
        }

        if (booking.getDestinationAccount() == null || booking.getDestinationAccount().isBlank()) {
            errors.add("Destination Account is required");
        } else if (booking.getSourceAccount() != null && booking.getSourceAccount().equals(booking.getDestinationAccount())) {
            errors.add("Source and Destination Account must differ");
        }

        return errors;
    }
}
