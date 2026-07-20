package de.bohnottensen.financialaudit.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AmlEngineTest {

    @Test
    void shouldFlagHighAmountTransaction() {
        Booking booking = new Booking();
        booking.setAmount(new BigDecimal("15000.00"));
        booking.setCurrency("EUR");

        AmlEngine engine = new AmlEngine(List.of(new HighAmountRule(new BigDecimal("10000.00"))));
        List<String> alerts = engine.check(booking);

        assertThat(alerts).contains("High amount detected: 15000.00 EUR");
    }

    @Test
    void shouldNotFlagNormalAmountTransaction() {
        Booking booking = new Booking();
        booking.setAmount(new BigDecimal("500.00"));
        booking.setCurrency("EUR");

        AmlEngine engine = new AmlEngine(List.of(new HighAmountRule(new BigDecimal("10000.00"))));
        List<String> alerts = engine.check(booking);

        assertThat(alerts).isEmpty();
    }
}
