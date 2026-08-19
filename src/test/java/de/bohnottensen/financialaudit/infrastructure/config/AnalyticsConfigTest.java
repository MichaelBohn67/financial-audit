package de.bohnottensen.financialaudit.infrastructure.config;

import de.bohnottensen.financialaudit.domain.model.AmlEngine;
import de.bohnottensen.financialaudit.domain.model.Booking;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsConfigTest {

    @Test
    void shouldCreateAmlEngineWithDefaultThreshold() {
        AnalyticsConfig config = new AnalyticsConfig();
        AmlEngine engine = config.amlEngine();

        assertThat(engine).isNotNull();

        Booking lowBooking = new Booking();
        lowBooking.setAmount(new BigDecimal("5000.00"));
        assertThat(engine.check(lowBooking)).isEmpty();

        Booking highBooking = new Booking();
        highBooking.setAmount(new BigDecimal("15000.00"));
        highBooking.setCurrency("EUR");
        List<String> alerts = engine.check(highBooking);
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0)).contains("15000.00").contains("EUR");
    }
}
