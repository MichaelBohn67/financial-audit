package de.bohnottensen.financialaudit.integration;

import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AutomaticAuditTrailIntegrationTest {
    @Autowired private BookingRepository bookings;
    @Autowired private AuditEventRepository auditEvents;
    @Autowired private EntityManager entityManager;

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void shouldAuditCreateUpdateAndDeleteWithPreviousAndCurrentSnapshots() {
        Booking booking = bookings.save(booking("100.00"));
        entityManager.flush();

        booking.setAmount(new BigDecimal("200.00"));
        bookings.save(booking);
        entityManager.flush();

        bookings.delete(booking);
        entityManager.flush();

        List<AuditEvent> events = auditEvents.findAll().stream()
                .filter(event -> "Booking".equals(event.getEntityType())
                        && event.getEventType().startsWith("PERSISTENCE_"))
                .toList();
        assertThat(events).extracting(AuditEvent::getEventType)
                .contains("PERSISTENCE_CREATE", "PERSISTENCE_UPDATE", "PERSISTENCE_DELETE");
        assertThat(events).allMatch(event -> "auditor".equals(event.getActor()));

        AuditEvent update = events.stream()
                .filter(event -> "PERSISTENCE_UPDATE".equals(event.getEventType()))
                .findFirst().orElseThrow();
        assertThat(update.getPreviousValue()).contains("amount=100.00");
        assertThat(update.getCurrentValue()).contains("amount=200.00");
    }

    @Test
    void shouldUseSystemActorForBackgroundPersistence() {
        SecurityContextHolder.clearContext();
        Booking booking = bookings.save(booking("50.00"));
        entityManager.flush();

        assertThat(auditEvents.findAll()).anyMatch(event ->
                "Booking".equals(event.getEntityType())
                        && "PERSISTENCE_CREATE".equals(event.getEventType())
                        && "SYSTEM".equals(event.getActor()));
    }

    private Booking booking(String amount) {
        Booking booking = new Booking();
        booking.setDescription("Automatic audit test");
        booking.setAmount(new BigDecimal(amount));
        booking.setCurrency("EUR");
        booking.setTransactionTimestamp(LocalDateTime.of(2026, 8, 17, 10, 0));
        booking.setSourceAccount("SOURCE");
        booking.setDestinationAccount("DESTINATION");
        return booking;
    }
}
