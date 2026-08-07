package de.bohnottensen.financialaudit.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record ReconciliationResult(
        String iban,
        BigDecimal internalSum,
        BigDecimal externalBalance,
        BigDecimal discrepancy,
        boolean isBalanced,
        List<Long> bookingIds
) {
}
