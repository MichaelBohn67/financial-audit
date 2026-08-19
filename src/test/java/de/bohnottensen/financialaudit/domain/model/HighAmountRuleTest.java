package de.bohnottensen.financialaudit.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HighAmountRuleTest {

    @Test
    void shouldEvaluateAmountsAgainstThreshold() {
        HighAmountRule rule = new HighAmountRule(new BigDecimal("10000.00"));

        Booking nullAmount = new Booking();
        assertThat(rule.evaluate(nullAmount)).isEmpty();

        Booking belowThreshold = new Booking();
        belowThreshold.setAmount(new BigDecimal("9999.99"));
        assertThat(rule.evaluate(belowThreshold)).isEmpty();

        Booking exactThreshold = new Booking();
        exactThreshold.setAmount(new BigDecimal("10000.00"));
        assertThat(rule.evaluate(exactThreshold)).isEmpty();

        Booking aboveThreshold = new Booking();
        aboveThreshold.setAmount(new BigDecimal("10000.01"));
        aboveThreshold.setCurrency("EUR");
        assertThat(rule.evaluate(aboveThreshold)).contains("High amount detected: 10000.01 EUR");
    }
}
