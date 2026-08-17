package de.bohnottensen.financialaudit.integration;

import de.bohnottensen.financialaudit.application.usecase.materiality.MaterialityService;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MaterialityIntegrationTest {

    @Autowired
    private MaterialityService materialityService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void shouldPersistMaterialityFindingAndAvoidDuplicatesOnRepeatedEvaluation() {
        materialityService.save("Q3 2026", new BigDecimal("1000"),
                new BigDecimal("500"), new BigDecimal("100"));

        Booking booking = new Booking();
        booking.setDescription("Materiality integration booking");
        booking.setAmount(new BigDecimal("750"));
        booking.setCurrency("EUR");
        booking.setTransactionTimestamp(LocalDateTime.of(2026, 8, 17, 10, 0));
        booking.setSourceAccount("SOURCE");
        booking.setDestinationAccount("DESTINATION");
        bookingRepository.save(booking);

        List<MaterialityService.MaterialityResult> first = materialityService.evaluateAll("SOURCE");
        List<MaterialityService.MaterialityResult> second = materialityService.evaluateAll("SOURCE");

        assertThat(first).singleElement().satisfies(result -> {
            assertThat(result.classification()).isEqualTo("PERFORMANCE");
            assertThat(result.material()).isTrue();
            assertThat(result.findingId()).isNotNull();
        });
        assertThat(second).singleElement().satisfies(result ->
                assertThat(result.findingId()).isEqualTo(first.get(0).findingId()));

        List<Finding> persisted = findingRepository.findAll().stream()
                .filter(finding -> "MATERIALITY_THRESHOLD".equals(finding.getRuleName()))
                .toList();
        assertThat(persisted).singleElement().satisfies(finding -> {
            assertThat(finding.getBooking().getId()).isEqualTo(booking.getId());
            assertThat(finding.getMaterialityConfig()).isNotNull();
            assertThat(finding.getMaterialityClassification()).isEqualTo("PERFORMANCE");
            assertThat(finding.getRiskLevel()).isEqualTo("MEDIUM");
        });
        assertThat(auditEventRepository.findAll()).anyMatch(event ->
                "SYSTEM_MATERIALITY".equals(event.getActor())
                        && "FINDING_CREATED".equals(event.getEventType()));
    }
}
