package de.bohnottensen.financialaudit.application.usecase.audit;

import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditTrailWriterTest {

    @Test
    void shouldRecordAuditEventWithAllFields() {
        AuditEventRepository repository = mock(AuditEventRepository.class);
        when(repository.save(any(AuditEvent.class))).thenAnswer(invocation -> {
            AuditEvent event = invocation.getArgument(0);
            event.setId(123L);
            return event;
        });

        AuditTrailWriter writer = new AuditTrailWriter(repository);

        AuditEvent result = writer.record(
                "BOOKING",
                456L,
                "BOOKING_UPDATED",
                "auditor1",
                "Updated amount",
                "100.00",
                "200.00"
        );

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(123L);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        AuditEvent saved = captor.getValue();

        assertThat(saved.getEntityType()).isEqualTo("BOOKING");
        assertThat(saved.getEntityId()).isEqualTo(456L);
        assertThat(saved.getEventType()).isEqualTo("BOOKING_UPDATED");
        assertThat(saved.getActor()).isEqualTo("auditor1");
        assertThat(saved.getSummary()).isEqualTo("Updated amount");
        assertThat(saved.getPreviousValue()).isEqualTo("100.00");
        assertThat(saved.getCurrentValue()).isEqualTo("200.00");
    }
}
