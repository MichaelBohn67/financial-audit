package de.bohnottensen.financialaudit.application.usecase.materiality;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.domain.model.MaterialityConfig;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.MaterialityConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MaterialityServiceTest {

    private MaterialityConfigRepository configs;
    private BookingRepository bookings;
    private FindingRepository findings;
    private AuditTrailWriter audit;
    private MaterialityService service;

    @BeforeEach
    void setUp() {
        configs = mock(MaterialityConfigRepository.class);
        bookings = mock(BookingRepository.class);
        findings = mock(FindingRepository.class);
        audit = mock(AuditTrailWriter.class);
        service = new MaterialityService(configs, bookings, findings, audit);
    }

    @Test
    void shouldValidateThresholdBoundariesOnSave() {
        String expectedMsg = "Materiality thresholds must satisfy 0 <= de minimis <= performance <= overall";

        // Blank name
        assertThatThrownBy(() -> service.save("  ", new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("10")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMsg);

        // Null thresholds
        assertThatThrownBy(() -> service.save("T1", null, new BigDecimal("50"), new BigDecimal("10")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMsg);

        // Zero or negative planning materiality
        assertThatThrownBy(() -> service.save("T1", BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("50")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMsg);

        assertThatThrownBy(() -> service.save("T1", new BigDecimal("-100"), new BigDecimal("50"), new BigDecimal("10")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMsg);

        // Zero or negative performance materiality
        assertThatThrownBy(() -> service.save("T1", new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("50")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMsg);

        // Negative de minimis threshold
        assertThatThrownBy(() -> service.save("T1", new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMsg);

        // Performance materiality > planning materiality
        assertThatThrownBy(() -> service.save("T1", new BigDecimal("1000"), new BigDecimal("1000.01"), new BigDecimal("50")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMsg);

        // De minimis > performance materiality
        assertThatThrownBy(() -> service.save("T1", new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("500.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMsg);

        // Zero de minimis is valid (boundary)
        when(configs.save(any(MaterialityConfig.class))).thenAnswer(inv -> inv.getArgument(0));
        MaterialityConfig zeroDeMinimis = service.save("T-Zero", new BigDecimal("1000"), new BigDecimal("500"), BigDecimal.ZERO);
        assertThat(zeroDeMinimis).isNotNull();
        assertThat(zeroDeMinimis.isActive()).isTrue();

        // Equal thresholds are valid (boundary)
        MaterialityConfig equalConfig = service.save("T-Equal", new BigDecimal("1000"), new BigDecimal("1000"), new BigDecimal("1000"));
        assertThat(equalConfig).isNotNull();
        assertThat(equalConfig.isActive()).isTrue();
    }

    @Test
    void shouldClassifyBookingAtExactBoundaries() {
        MaterialityConfig config = config(10L); // planning: 1000, performance: 500, deMinimis: 100
        when(configs.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(config));

        // Exactly below de minimis (99.99)
        assertThat(service.evaluate(booking(1L, "99.99")).classification()).isEqualTo("BELOW_THRESHOLD");
        assertThat(service.evaluate(booking(1L, "99.99")).material()).isFalse();

        // Exactly at de minimis (100.00)
        assertThat(service.evaluate(booking(2L, "100.00")).classification()).isEqualTo("DE_MINIMIS");
        assertThat(service.evaluate(booking(2L, "100.00")).material()).isTrue();

        // Between de minimis and performance (499.99)
        assertThat(service.evaluate(booking(3L, "499.99")).classification()).isEqualTo("DE_MINIMIS");

        // Exactly at performance (500.00)
        assertThat(service.evaluate(booking(4L, "500.00")).classification()).isEqualTo("PERFORMANCE");
        assertThat(service.evaluate(booking(4L, "500.00")).material()).isTrue();

        // Between performance and planning (999.99)
        assertThat(service.evaluate(booking(5L, "999.99")).classification()).isEqualTo("PERFORMANCE");

        // Exactly at planning (1000.00)
        assertThat(service.evaluate(booking(6L, "1000.00")).classification()).isEqualTo("OVERALL");
        assertThat(service.evaluate(booking(6L, "1000.00")).material()).isTrue();
    }

    @Test
    void shouldThrowWhenEvaluatingNullBookingOrNullAmount() {
        MaterialityConfig config = config(10L);
        when(configs.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(config));

        assertThatThrownBy(() -> service.evaluate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Booking and booking amount are required");

        Booking b = new Booking();
        b.setAmount(null);
        assertThatThrownBy(() -> service.evaluate(b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Booking and booking amount are required");
    }

    @Test
    void shouldThrowWhenNoActiveConfig() {
        when(configs.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.active())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active materiality configuration");
    }

    @Test
    void shouldSaveAndActivateNewConfigAndDeactivateOldOnes() {
        MaterialityConfig old1 = config(1L);
        old1.setActive(true);
        MaterialityConfig old2 = config(2L);
        old2.setActive(true);

        when(configs.findAll()).thenReturn(List.of(old1, old2));
        when(configs.save(any(MaterialityConfig.class))).thenAnswer(invocation -> {
            MaterialityConfig c = invocation.getArgument(0);
            if (c.getId() == null) {
                c.setId(99L);
            }
            return c;
        });

        MaterialityConfig saved = service.save("FY2026", new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("100"));

        assertThat(saved.getId()).isEqualTo(99L);
        assertThat(saved.getName()).isEqualTo("FY2026");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getPlanningMateriality()).isEqualByComparingTo("1000");
        assertThat(saved.getPerformanceMateriality()).isEqualByComparingTo("500");
        assertThat(saved.getDeMinimisThreshold()).isEqualByComparingTo("100");

        assertThat(old1.isActive()).isFalse();
        assertThat(old2.isActive()).isFalse();

        verify(audit).record("MaterialityConfig", 99L, "CREATE", "system",
                "Materiality configured", null, "FY2026");
    }

    @Test
    void shouldRejectInvalidThresholdOrderingAndInvalidParameters() {
        assertThatThrownBy(() -> service.save(null, new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save("  ", new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save("test", null, new BigDecimal("500"), new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save("test", new BigDecimal("1000"), null, new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save("test", new BigDecimal("1000"), new BigDecimal("500"), null))
                .isInstanceOf(IllegalArgumentException.class);

        // overall <= 0
        assertThatThrownBy(() -> service.save("test", BigDecimal.ZERO, new BigDecimal("500"), new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save("test", new BigDecimal("-10"), new BigDecimal("500"), new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);

        // performance <= 0
        assertThatThrownBy(() -> service.save("test", new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);

        // deMinimis < 0
        assertThatThrownBy(() -> service.save("test", new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);

        // performance > overall
        assertThatThrownBy(() -> service.save("test", new BigDecimal("100"), new BigDecimal("200"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        // deMinimis > performance
        assertThatThrownBy(() -> service.save("test", new BigDecimal("1000"), new BigDecimal("100"), new BigDecimal("200")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldEvaluateAllBookingsAndPersistFindingsWhenNotPresent() {
        MaterialityConfig config = config(10L);
        when(configs.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(config));

        Booking b1 = booking(1L, "50.00"); // BELOW_THRESHOLD
        Booking b2 = booking(2L, "150.00"); // DE_MINIMIS
        Booking b3 = booking(3L, "600.00"); // PERFORMANCE
        Booking b4 = booking(4L, "2000.00"); // OVERALL

        when(bookings.findBySourceAccountOrDestinationAccount("ACC1", "ACC1"))
                .thenReturn(List.of(b1, b2, b3, b4));

        when(findings.findFirstByBookingIdAndMaterialityConfigIdAndRuleName(anyLong(), eq(10L), eq("MATERIALITY_THRESHOLD")))
                .thenReturn(Optional.empty());

        when(findings.save(any(Finding.class))).thenAnswer(invocation -> {
            Finding f = invocation.getArgument(0);
            f.setId(1000L + f.getBooking().getId());
            return f;
        });

        List<MaterialityService.MaterialityResult> results = service.evaluateAll("ACC1");

        assertThat(results).hasSize(4);
        assertThat(results.get(0).classification()).isEqualTo("BELOW_THRESHOLD");
        assertThat(results.get(0).findingId()).isNull();

        assertThat(results.get(1).classification()).isEqualTo("DE_MINIMIS");
        assertThat(results.get(1).findingId()).isEqualTo(1002L);

        assertThat(results.get(2).classification()).isEqualTo("PERFORMANCE");
        assertThat(results.get(2).findingId()).isEqualTo(1003L);

        assertThat(results.get(3).classification()).isEqualTo("OVERALL");
        assertThat(results.get(3).findingId()).isEqualTo(1004L);

        ArgumentCaptor<Finding> findingCaptor = ArgumentCaptor.forClass(Finding.class);
        verify(findings, times(3)).save(findingCaptor.capture());

        Finding deMinimisFinding = findingCaptor.getAllValues().get(0);
        assertThat(deMinimisFinding.getRiskLevel()).isEqualTo("LOW");
        assertThat(deMinimisFinding.getMaterialityClassification()).isEqualTo("DE_MINIMIS");
        assertThat(deMinimisFinding.getRuleName()).isEqualTo("MATERIALITY_THRESHOLD");
        assertThat(deMinimisFinding.getAlertDescription()).isEqualTo("Booking amount meets the DE_MINIMIS materiality threshold");
        assertThat(deMinimisFinding.getStatus()).isEqualTo("NEW");
        assertThat(deMinimisFinding.getRuleVersion()).isEqualTo("1");
        assertThat(deMinimisFinding.getRunContext()).isEqualTo("ACC1");
        assertThat(deMinimisFinding.getAnalysisRunId()).isEqualTo("MAT-10");

        Finding perfFinding = findingCaptor.getAllValues().get(1);
        assertThat(perfFinding.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(perfFinding.getMaterialityClassification()).isEqualTo("PERFORMANCE");

        Finding overallFinding = findingCaptor.getAllValues().get(2);
        assertThat(overallFinding.getRiskLevel()).isEqualTo("HIGH");
        assertThat(overallFinding.getMaterialityClassification()).isEqualTo("OVERALL");

        ArgumentCaptor<String> snapshotCaptor = ArgumentCaptor.forClass(String.class);
        verify(audit, times(3)).record(eq("FINDING"), anyLong(), eq("FINDING_CREATED"), eq("SYSTEM_MATERIALITY"),
                eq("Finding created by materiality evaluation"), isNull(), snapshotCaptor.capture());
        for (String snapshot : snapshotCaptor.getAllValues()) {
            assertThat(snapshot).contains("bookingId=").contains(";materialityConfigId=10").contains(";classification=").contains(";riskLevel=").contains(";status=NEW");
        }
    }

    @Test
    void shouldEvaluateAllBookingsAndUseExistingFindingIfExists() {
        MaterialityConfig config = config(10L);
        when(configs.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(config));

        Booking b = booking(2L, "2000.00");
        when(bookings.findBySourceAccountOrDestinationAccount("ACC1", "ACC1")).thenReturn(List.of(b));

        Finding existing = new Finding();
        existing.setId(777L);
        when(findings.findFirstByBookingIdAndMaterialityConfigIdAndRuleName(2L, 10L, "MATERIALITY_THRESHOLD"))
                .thenReturn(Optional.of(existing));

        List<MaterialityService.MaterialityResult> results = service.evaluateAll("ACC1");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).findingId()).isEqualTo(777L);

        verify(findings, never()).save(any());
        verify(audit, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    private MaterialityConfig config(Long id) {
        MaterialityConfig config = new MaterialityConfig();
        config.setId(id);
        config.setName("Default");
        config.setPlanningMateriality(new BigDecimal("1000"));
        config.setPerformanceMateriality(new BigDecimal("500"));
        config.setDeMinimisThreshold(new BigDecimal("100"));
        config.setActive(true);
        return config;
    }

    private Booking booking(Long id, String amount) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setDescription("Materiality test booking");
        booking.setAmount(new BigDecimal(amount));
        booking.setCurrency("EUR");
        booking.setTransactionTimestamp(LocalDateTime.of(2026, 8, 17, 10, 0));
        booking.setSourceAccount("ACC1");
        booking.setDestinationAccount("ACC2");
        return booking;
    }
}
