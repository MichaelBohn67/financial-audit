package de.bohnottensen.financialaudit.application.usecase.audit;

import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventHasherTest {

    private final AuditEventHasher hasher = new AuditEventHasher();
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Test
    void shouldBeDeterministicForIdenticalEvents() {
        AuditEvent first = eventWith("PERSISTENCE_CREATE", null, "amount=10;");
        AuditEvent second = eventWith("PERSISTENCE_CREATE", null, "amount=10;");

        assertThat(hasher.calculateHash(first, "")).isEqualTo(hasher.calculateHash(second, ""));
    }

    @Test
    void shouldTreatNullFieldsAsEmptyStrings() {
        // This is the contract that the divergent copy in the Hibernate listener violated:
        // null must serialize as "" and not as the literal text "null".
        AuditEvent event = eventWith("PERSISTENCE_CREATE", null, "amount=10;");

        String withExplicitNullPreviousHash = hasher.calculateHash(event, null);
        String withEmptyPreviousHash = hasher.calculateHash(event, "");

        assertThat(withExplicitNullPreviousHash).isEqualTo(withEmptyPreviousHash);
    }

    @Test
    void shouldChangeHashWhenAnyFieldChanges() {
        AuditEvent base = eventWith("PERSISTENCE_CREATE", null, "amount=10;");
        AuditEvent tampered = eventWith("PERSISTENCE_CREATE", null, "amount=9999;");

        assertThat(hasher.calculateHash(tampered, ""))
                .isNotEqualTo(hasher.calculateHash(base, ""));
    }

    @Test
    void shouldChainOnPreviousHash() {
        AuditEvent event = eventWith("PERSISTENCE_CREATE", null, "amount=10;");

        assertThat(hasher.calculateHash(event, "abc"))
                .isNotEqualTo(hasher.calculateHash(event, "def"));
    }

    private AuditEvent eventWith(String eventType, String previousValue, String currentValue) {
        AuditEvent event = new AuditEvent();
        event.setEntityType("Booking");
        event.setEntityId(42L);
        event.setEventType(eventType);
        event.setActor("SYSTEM");
        event.setSummary("Automatic persistence audit event");
        event.setPreviousValue(previousValue);
        event.setCurrentValue(currentValue);
        // Pin the timestamp: initializeOccurredAtIfMissing() uses the wall clock, which would
        // make two otherwise identical events hash differently.
        ReflectionTestUtils.setField(event.getMetadata(), "occurredAt", FIXED_TIME);
        return event;
    }
}
