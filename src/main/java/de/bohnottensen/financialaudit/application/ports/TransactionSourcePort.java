package de.bohnottensen.financialaudit.application.ports;

import de.bohnottensen.financialaudit.domain.model.Booking;
import java.util.List;

/**
 * REQ-INT-001: Standardized data import via source port.
 * Port for importing transaction data from various sources.
 */
public interface TransactionSourcePort {
    default boolean supports(Object source) {
        return true;
    }

    default String sourceType() {
        return getClass().getSimpleName();
    }

    List<Booking> importTransactions(Object source);
}
