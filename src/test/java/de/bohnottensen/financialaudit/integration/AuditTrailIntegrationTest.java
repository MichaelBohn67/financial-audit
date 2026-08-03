package de.bohnottensen.financialaudit.integration;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: AuditTrailWriter persists immutable AuditEvents to the real H2 database
 * with correct entity type, event type, actor, and change values (wer/was/wann/vorher/nachher).
 */
@SpringBootTest
@Transactional
class AuditTrailIntegrationTest {

    @Autowired
    private AuditTrailWriter auditTrailWriter;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void shouldPersistAuditEventWithAllRequiredFields() {
        long before = auditEventRepository.count();

        AuditEvent event = auditTrailWriter.record(
                "BOOKING",
                42L,
                "BOOKING_CREATED",
                "assistant",
                "Booking created via web UI",
                null,
                "description=Invoice A;amount=1000.00;currency=EUR"
        );

        assertThat(event.getId()).isNotNull();
        assertThat(auditEventRepository.count()).isEqualTo(before + 1);

        AuditEvent persisted = auditEventRepository.findById(event.getId()).orElseThrow();
        assertThat(persisted.getEntityType()).isEqualTo("BOOKING");
        assertThat(persisted.getEntityId()).isEqualTo(42L);
        assertThat(persisted.getEventType()).isEqualTo("BOOKING_CREATED");
        assertThat(persisted.getActor()).isEqualTo("assistant");
        assertThat(persisted.getSummary()).isEqualTo("Booking created via web UI");
        assertThat(persisted.getPreviousValue()).isNull();
        assertThat(persisted.getCurrentValue()).contains("Invoice A");
        assertThat(persisted.getOccurredAt()).isNotNull();
    }

    @Test
    void shouldPersistUpdateEventWithPreviousAndCurrentValues() {
        AuditEvent event = auditTrailWriter.record(
                "BOOKING",
                7L,
                "BOOKING_UPDATED",
                "senior",
                "Booking updated",
                "description=Old Invoice;amount=500.00",
                "description=Revised Invoice;amount=750.00"
        );

        AuditEvent persisted = auditEventRepository.findById(event.getId()).orElseThrow();
        assertThat(persisted.getPreviousValue()).contains("Old Invoice");
        assertThat(persisted.getCurrentValue()).contains("Revised Invoice");
    }

    @Test
    void shouldPersistMultipleEventsForDifferentEntityTypes() {
        long before = auditEventRepository.count();

        auditTrailWriter.record("WORKPAPER", 1L, "WORKPAPER_CREATED", "assistant",
                "Workpaper created", null, "title=AML-WP;status=DRAFT");
        auditTrailWriter.record("FINDING", 2L, "FINDING_CREATED", "SYSTEM_ANALYTICS",
                "Finding created by analytics", null, "ruleName=HIGH_AMOUNT;riskLevel=HIGH");
        auditTrailWriter.record("WORKPAPER", 1L, "WORKPAPER_SUBMIT", "assistant",
                "Workpaper submitted", "status=IN_PROGRESS", "status=SUBMITTED");

        assertThat(auditEventRepository.count()).isEqualTo(before + 3);

        List<AuditEvent> events = auditEventRepository.findAll();
        assertThat(events.stream().anyMatch(e -> "WORKPAPER".equals(e.getEntityType())
                && "WORKPAPER_CREATED".equals(e.getEventType()))).isTrue();
        assertThat(events.stream().anyMatch(e -> "FINDING".equals(e.getEntityType())
                && "FINDING_CREATED".equals(e.getEventType()))).isTrue();
        assertThat(events.stream().anyMatch(e -> "WORKPAPER_SUBMIT".equals(e.getEventType()))).isTrue();
    }

    @Test
    void shouldSetOccurredAtTimestampAutomatically() {
        AuditEvent event = auditTrailWriter.record(
                "WORKPAPER", 99L, "WORKPAPER_APPROVE",
                "wirtschaftspruefer", "Final approval", "status=SUBMITTED", "status=APPROVED");

        AuditEvent persisted = auditEventRepository.findById(event.getId()).orElseThrow();
        assertThat(persisted.getOccurredAt()).isNotNull();
    }
}
