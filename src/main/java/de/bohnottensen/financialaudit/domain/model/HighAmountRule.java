package de.bohnottensen.financialaudit.domain.model;

import java.math.BigDecimal;
import java.util.Optional;

public class HighAmountRule implements AmlRule {
    private final BigDecimal threshold;

    public HighAmountRule(BigDecimal threshold) {
        this.threshold = threshold;
    }

    @Override
    public Optional<String> evaluate(Booking booking) {
        if (booking.getAmount() != null && booking.getAmount().compareTo(threshold) > 0) {
            return Optional.of(String.format("High amount detected: %s %s", booking.getAmount(), booking.getCurrency()));
        }
        return Optional.empty();
    }
}
