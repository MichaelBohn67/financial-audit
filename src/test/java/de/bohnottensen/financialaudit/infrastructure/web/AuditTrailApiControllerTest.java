package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditIntegrityService;
import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuditTrailApiControllerTest {

    private AuditEventRepository events;
    private AuditIntegrityService integrity;
    private AuditTrailApiController controller;

    @BeforeEach
    void setUp() {
        events = mock(AuditEventRepository.class);
        integrity = mock(AuditIntegrityService.class);
        controller = new AuditTrailApiController(events, integrity);
    }

    @Test
    void shouldReturnTop10Events() {
        AuditEvent event = new AuditEvent();
        event.setId(1L);
        when(events.findTop10ByOrderByMetadata_OccurredAtDesc()).thenReturn(List.of(event));

        List<AuditEvent> result = controller.events();

        assertThat(result).containsExactly(event);
        verify(events).findTop10ByOrderByMetadata_OccurredAtDesc();
    }

    @Test
    void shouldVerifyEmptyEventListAsValid() {
        when(events.findAll()).thenReturn(List.of());

        AuditTrailApiController.Verification result = controller.verify();

        assertThat(result.valid()).isTrue();
        assertThat(result.firstInvalidEventId()).isNull();
    }

    @Test
    void shouldVerifySortedEventChainAsValid() {
        AuditEvent event1 = new AuditEvent();
        event1.setId(1L);
        ReflectionTestUtils.setField(event1.getMetadata(), "occurredAt", LocalDateTime.of(2026, 1, 1, 10, 0));
        event1.setIntegrityHash("HASH-1");

        AuditEvent event2 = new AuditEvent();
        event2.setId(2L);
        ReflectionTestUtils.setField(event2.getMetadata(), "occurredAt", LocalDateTime.of(2026, 1, 2, 10, 0));
        event2.setIntegrityHash("HASH-2");

        // Return in unsorted order to verify sort behavior
        when(events.findAll()).thenReturn(List.of(event2, event1));
        when(integrity.verify(event1, "")).thenReturn(true);
        when(integrity.verify(event2, "HASH-1")).thenReturn(true);

        AuditTrailApiController.Verification result = controller.verify();

        assertThat(result.valid()).isTrue();
        assertThat(result.firstInvalidEventId()).isNull();

        verify(integrity).verify(event1, "");
        verify(integrity).verify(event2, "HASH-1");
    }

    @Test
    void shouldReturnInvalidWhenAnEventFailsIntegrityCheck() {
        AuditEvent event1 = new AuditEvent();
        event1.setId(1L);
        ReflectionTestUtils.setField(event1.getMetadata(), "occurredAt", LocalDateTime.of(2026, 1, 1, 10, 0));
        event1.setIntegrityHash("HASH-1");

        AuditEvent event2 = new AuditEvent();
        event2.setId(2L);
        ReflectionTestUtils.setField(event2.getMetadata(), "occurredAt", LocalDateTime.of(2026, 1, 2, 10, 0));
        event2.setIntegrityHash("HASH-2");

        AuditEvent event3 = new AuditEvent();
        event3.setId(3L);
        ReflectionTestUtils.setField(event3.getMetadata(), "occurredAt", LocalDateTime.of(2026, 1, 3, 10, 0));

        when(events.findAll()).thenReturn(List.of(event1, event2, event3));
        when(integrity.verify(event1, "")).thenReturn(true);
        when(integrity.verify(event2, "HASH-1")).thenReturn(false);

        AuditTrailApiController.Verification result = controller.verify();

        assertThat(result.valid()).isFalse();
        assertThat(result.firstInvalidEventId()).isEqualTo(2L);

        verify(integrity).verify(event1, "");
        verify(integrity).verify(event2, "HASH-1");
        verify(integrity, never()).verify(eq(event3), anyString());
    }

    @Test
    void shouldTestVerificationRecord() {
        AuditTrailApiController.Verification v1 = new AuditTrailApiController.Verification(true, null);
        assertThat(v1.valid()).isTrue();
        assertThat(v1.firstInvalidEventId()).isNull();

        AuditTrailApiController.Verification v2 = new AuditTrailApiController.Verification(false, 42L);
        assertThat(v2.valid()).isFalse();
        assertThat(v2.firstInvalidEventId()).isEqualTo(42L);
    }
}
