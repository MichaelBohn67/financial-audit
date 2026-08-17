package de.bohnottensen.financialaudit.application.usecase.materiality;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.MaterialityConfig;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.MaterialityConfigRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaterialityServiceTest {

    @Test
    void shouldClassifyBookingAtEachMaterialityLevel() {
        MaterialityConfig config = config(10_000L);
        MaterialityConfigRepository configs = mock(MaterialityConfigRepository.class);
        when(configs.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(config));

        MaterialityService service = new MaterialityService(configs, mock(BookingRepository.class),
                mock(FindingRepository.class), mock(AuditTrailWriter.class));

        assertThat(service.evaluate(booking("50")).classification()).isEqualTo("BELOW_THRESHOLD");
        assertThat(service.evaluate(booking("100")).classification()).isEqualTo("DE_MINIMIS");
        assertThat(service.evaluate(booking("500")).classification()).isEqualTo("PERFORMANCE");
        assertThat(service.evaluate(booking("1000")).classification()).isEqualTo("OVERALL");
        assertThat(service.evaluate(booking("-1000")).amount()).isEqualByComparingTo("1000");
    }

    @Test
    void shouldRejectInvalidThresholdOrdering() {
        MaterialityConfigRepository configs = mock(MaterialityConfigRepository.class);
        MaterialityService service = new MaterialityService(configs, mock(BookingRepository.class),
                mock(FindingRepository.class), mock(AuditTrailWriter.class));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.save("invalid", new BigDecimal("100"), new BigDecimal("200"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private MaterialityConfig config(Long id) {
        MaterialityConfig config = new MaterialityConfig();
        config.setId(id);
        config.setPlanningMateriality(new BigDecimal("1000"));
        config.setPerformanceMateriality(new BigDecimal("500"));
        config.setDeMinimisThreshold(new BigDecimal("100"));
        config.setActive(true);
        return config;
    }

    private Booking booking(String amount) {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setDescription("Materiality test booking");
        booking.setAmount(new BigDecimal(amount));
        booking.setCurrency("EUR");
        booking.setTransactionTimestamp(LocalDateTime.of(2026, 8, 17, 10, 0));
        booking.setSourceAccount("SOURCE");
        booking.setDestinationAccount("DESTINATION");
        return booking;
    }
}
