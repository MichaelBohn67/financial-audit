package de.bohnottensen.financialaudit.domain.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AmlEngine {
    private final List<AmlRule> rules;

    public AmlEngine(List<AmlRule> rules) {
        this.rules = rules;
    }

    public List<String> check(Booking booking) {
        return rules.stream()
                .map(rule -> rule.evaluate(booking))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
