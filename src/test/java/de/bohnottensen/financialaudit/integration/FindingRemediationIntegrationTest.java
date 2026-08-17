package de.bohnottensen.financialaudit.integration;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.application.usecase.finding.FindingManagementService;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.domain.model.Workpaper;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.WorkpaperRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FindingRemediationIntegrationTest {
    @Autowired private FindingManagementService service;
    @Autowired private BookingRepository bookings;
    @Autowired private FindingRepository findings;
    @Autowired private WorkpaperRepository workpapers;
    @Autowired private AuditEventRepository auditEvents;

    @Test
    void shouldPersistLifecycleAndAuditSnapshots() {
        Booking booking = new Booking();
        booking.setDescription("Remediation booking");
        booking.setAmount(new BigDecimal("100"));
        booking.setCurrency("EUR");
        booking.setTransactionTimestamp(LocalDateTime.now());
        booking.setSourceAccount("SOURCE");
        booking.setDestinationAccount("DESTINATION");
        bookings.save(booking);

        Finding finding = new Finding();
        finding.setBooking(booking);
        finding.setRuleName("TEST_RULE");
        finding.setAlertDescription("Test finding");
        finding.setRiskLevel("MEDIUM");
        finding = findings.save(finding);

        Workpaper workpaper = new Workpaper();
        workpaper.setTitle("Remediation workpaper");
        workpaper.setStatus("DRAFT");
        workpaper = workpapers.save(workpaper);

        service.linkWorkpaper(finding.getId(), workpaper.getId(), "auditor");
        service.assign(finding.getId(), "owner", LocalDate.of(2026, 9, 1), "auditor");
        service.updatePlan(finding.getId(), "Implement compensating control", "auditor");
        service.transition(finding.getId(), "IN_PROGRESS", null, "auditor");
        service.transition(finding.getId(), "READY_FOR_REVIEW", null, "auditor");
        service.transition(finding.getId(), "RESOLVED", "Evidence attached", "reviewer");
        service.transition(finding.getId(), "CLOSED", null, "reviewer");

        Finding persisted = findings.findById(finding.getId()).orElseThrow();
        assertThat(persisted.getRemediationStatus()).isEqualTo("CLOSED");
        assertThat(persisted.getWorkpaper().getId()).isEqualTo(workpaper.getId());
        assertThat(persisted.getResolvedBy()).isEqualTo("reviewer");
        assertThat(auditEvents.findAll()).anyMatch(event ->
                "FINDING".equals(event.getEntityType())
                        && "REMEDIATION_RESOLVED".equals(event.getEventType())
                        && event.getPreviousValue() != null
                        && event.getCurrentValue() != null);
    }
}
