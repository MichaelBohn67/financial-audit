package de.bohnottensen.financialaudit.application.usecase.audit;

import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditTrailWriter {

    private final AuditEventRepository auditEventRepository;

    public AuditTrailWriter(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public AuditEvent record(String entityType,
                             Long entityId,
                             String eventType,
                             String actor,
                             String summary,
                             String previousValue,
                             String currentValue) {
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEntityType(entityType);
        auditEvent.setEntityId(entityId);
        auditEvent.setEventType(eventType);
        auditEvent.setActor(actor);
        auditEvent.setSummary(summary);
        auditEvent.setPreviousValue(previousValue);
        auditEvent.setCurrentValue(currentValue);
        return auditEventRepository.save(auditEvent);
    }
}
