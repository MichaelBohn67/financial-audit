package de.bohnottensen.financialaudit.domain.model;

import java.util.Optional;

public interface AmlRule {
    Optional<String> evaluate(Booking booking);
}
